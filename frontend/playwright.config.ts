import { defineConfig, devices } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// Point E2E_BASE_URL at a running stack (for example the Docker Compose one) to test that instead of
// starting the backend and the Vite dev server here.
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'
const backendDir = fileURLToPath(new URL('../backend/', import.meta.url))
const gradlew = path.join(backendDir, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew')

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : [
        {
          // Backend on in-memory H2, so the suite needs no database container.
          command: `"${gradlew}" bootRun --args=--spring.profiles.active=h2`,
          cwd: backendDir,
          url: 'http://localhost:8080/api/sectors',
          timeout: 180_000,
          reuseExistingServer: true,
        },
        {
          command: 'node node_modules/vite/bin/vite.js',
          url: 'http://localhost:5173',
          timeout: 60_000,
          reuseExistingServer: true,
        },
      ],
})
