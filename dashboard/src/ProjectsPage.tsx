import { useState, type FormEvent } from 'react'
import { api } from './api'
import { projectHref } from './routes'
import { ErrorText } from './ui'
import { formatDate, useLoad } from './lib'

export function ProjectsPage() {
  const projects = useLoad(api.projects, [])
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function create(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const project = await api.createProject(name.trim())
      location.hash = projectHref(project.projectId)
    } catch (err) {
      setError((err as Error).message)
      setBusy(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Projects</h1>
        <p className="muted">Each project holds its own flags, with a config per environment.</p>
      </div>

      <ErrorText>{projects.error}</ErrorText>
      {projects.data && projects.data.length === 0 && <p className="empty">No projects yet. Create the first one below.</p>}
      {projects.data && projects.data.length > 0 && (
        <ul className="grid">
          {projects.data.map((p) => (
            <li key={p.projectId}>
              <a className="card card--link" href={projectHref(p.projectId)}>
                <span className="card__title">{p.name}</span>
                <span className="mono muted">ID {p.projectId} · created {formatDate(p.createdOn)}</span>
              </a>
            </li>
          ))}
        </ul>
      )}

      <section className="panel" aria-labelledby="new-project">
        <h2 id="new-project">New project</h2>
        <form className="form-row" onSubmit={create}>
          <label className="field">
            <span>Name</span>
            <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={255} placeholder="Book Logger" />
          </label>
          <button className="btn btn--primary" disabled={busy || !name.trim()}>Create project</button>
        </form>
        <ErrorText>{error}</ErrorText>
      </section>
    </>
  )
}
