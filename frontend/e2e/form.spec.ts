import { expect, test, type Page } from '@playwright/test'

const nameField = (page: Page) => page.getByRole('textbox', { name: 'Name' })
const sectors = (page: Page) => page.getByRole('listbox', { name: 'Sectors' })
const filter = (page: Page) => page.getByRole('searchbox', { name: 'Filter sectors' })
const terms = (page: Page) => page.getByRole('checkbox', { name: 'Agree to terms' })
const save = (page: Page) => page.getByRole('button', { name: 'Save' })
const chips = (page: Page) => page.getByRole('list', { name: 'Selected sectors' }).getByRole('listitem')
const option = (page: Page, label: string) => sectors(page).getByRole('option', { name: label, exact: true })

test.beforeEach(async ({ page }) => {
  await page.goto('/')
  await expect(save(page)).toBeVisible()
})

test('shows the sector hierarchy loaded from the API', async ({ page }) => {
  const options = sectors(page).getByRole('option')
  await expect(options).toHaveCount(79)
  await expect(options.first()).toHaveText('Manufacturing')
  await expect(options.last()).toHaveText('Water')
  // sub-sectors are indented by level
  await expect(option(page, 'Food and Beverage')).toHaveClass(/level-1/)
  await expect(option(page, 'Fish & fish products')).toHaveClass(/level-2/)
  await expect(option(page, 'Aluminium and steel workboats')).toHaveClass(/level-3/)
  await expect(page.getByText('No sectors selected yet')).toBeVisible()
})

test('rejects an empty form and clears each error once it is fixed', async ({ page }) => {
  await save(page).click()

  await expect(page.getByText('Name is required')).toBeVisible()
  await expect(page.getByText('Select at least one sector')).toBeVisible()
  await expect(page.getByText('You must agree to the terms')).toBeVisible()
  await expect(nameField(page)).toBeFocused()

  await nameField(page).fill('John Doe')
  await expect(page.getByText('Name is required')).toBeHidden()
  await option(page, 'Manufacturing').click()
  await expect(page.getByText('Select at least one sector')).toBeHidden()
  await terms(page).check()
  await expect(page.getByText('You must agree to the terms')).toBeHidden()
})

test('saves, refills after a reload, and lets the same session edit its data', async ({ page }) => {
  await nameField(page).fill('  John Doe  ')
  await option(page, 'Manufacturing').click()
  await option(page, 'Fish & fish products').click()
  await expect(page.getByText('2 selected')).toBeVisible()
  await terms(page).check()
  await save(page).click()

  await expect(page.getByRole('status')).toContainText('Saved.')
  await expect(page.getByText('Editing your saved data')).toBeVisible()
  // the form is refilled from what the server stored: the name comes back trimmed
  await expect(nameField(page)).toHaveValue('John Doe')

  await page.reload()
  await expect(nameField(page)).toHaveValue('John Doe')
  await expect(chips(page)).toHaveText(['Manufacturing', 'Manufacturing › Food and Beverage › Fish & fish products'])
  await expect(terms(page)).toBeChecked()
  await expect(page.getByText('Editing your saved data')).toBeVisible()

  await nameField(page).fill('Jane Doe')
  await option(page, 'Manufacturing').click() // deselect
  await option(page, 'Water').click()
  await save(page).click()
  await expect(page.getByRole('status')).toContainText('Saved.')

  await page.reload()
  await expect(nameField(page)).toHaveValue('Jane Doe')
  await expect(chips(page)).toHaveText([
    'Manufacturing › Food and Beverage › Fish & fish products',
    'Service › Transport and Logistics › Water',
  ])
})

test('another browser session starts with an empty form', async ({ page, browser }) => {
  await nameField(page).fill('John Doe')
  await option(page, 'Wood').click()
  await terms(page).check()
  await save(page).click()
  await expect(page.getByRole('status')).toContainText('Saved.')

  const otherContext = await browser.newContext()
  const otherPage = await otherContext.newPage()
  await otherPage.goto('/')
  await expect(save(otherPage)).toBeVisible()
  await expect(nameField(otherPage)).toHaveValue('')
  await expect(otherPage.getByText('No sectors selected yet')).toBeVisible()
  await expect(otherPage.getByText('Editing your saved data')).toBeHidden()
  await otherContext.close()
})

test('filters the list by partial words and keeps selections made outside the filter', async ({ page }) => {
  await option(page, 'Manufacturing').click()
  await option(page, 'Beverages').click()

  await filter(page).fill('wood')
  await expect(sectors(page).getByRole('option')).toHaveText([
    'Manufacturing',
    'Wood',
    'Other (Wood)',
    'Wooden building materials',
    'Wooden houses',
  ])
  await expect(page.getByText('2 selected')).toBeVisible()

  await filter(page).fill('fish prod')
  await expect(sectors(page).getByRole('option')).toHaveText(['Manufacturing', 'Food and Beverage', 'Fish & fish products'])

  await filter(page).fill('banking')
  await expect(sectors(page).getByRole('option')).toHaveCount(0)
  await expect(page.getByText('No sectors match “banking”.')).toBeVisible()

  await filter(page).fill('')
  await expect(sectors(page).getByRole('option')).toHaveCount(79)
  await expect(chips(page)).toHaveText(['Manufacturing', 'Manufacturing › Food and Beverage › Beverages'])
})

test('chips and Clear all remove selections', async ({ page }) => {
  await option(page, 'Manufacturing').click()
  await option(page, 'Water').click()
  await expect(chips(page)).toHaveCount(2)

  await page.getByRole('button', { name: 'Remove Manufacturing', exact: true }).click()
  await expect(chips(page)).toHaveText(['Service › Transport and Logistics › Water'])

  await page.getByRole('button', { name: 'Clear all' }).click()
  await expect(chips(page)).toHaveCount(0)
  await expect(page.getByText('No sectors selected yet')).toBeVisible()
})
