import { SubmissionForm } from './submission/SubmissionForm'

export default function App() {
  return (
    <main>
      <h1>Sectors</h1>
      <p className="intro">Please enter your name and pick the sectors you are currently involved in.</p>
      <SubmissionForm />
    </main>
  )
}
