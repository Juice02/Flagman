import { useState, type FormEvent } from 'react'
import { api, type Flag } from './api'
import { flagHref } from './routes'
import { ErrorText } from './ui'
import { formatDate, useLoad } from './lib'

const emptyFlag = { flagKey: '', friendlyName: '', description: '' }

export function ProjectPage({ projectId }: { projectId: number }) {
  const project = useLoad(() => api.project(projectId), [projectId])
  const flags = useLoad(() => api.flags(projectId), [projectId])
  const [form, setForm] = useState(emptyFlag)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function create(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.createFlag(projectId, { ...form, flagKey: form.flagKey.trim() })
      setForm(emptyFlag)
      flags.reload()
    } catch (err) {
      setError((err as Error).message)
    }
    setBusy(false)
  }

  async function remove(flag: Flag) {
    if (!confirm(`Delete flag "${flag.flagKey}" with its configs and rules in every environment?`)) return
    try {
      await api.deleteFlag(projectId, flag.flagKey)
      flags.setData((list) => list?.filter((f) => f.flagKey !== flag.flagKey) ?? null)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  // An unknown project: show only the 404 message.
  if (project.error) return <><Crumbs /><ErrorText>{project.error}</ErrorText></>

  return (
    <>
      <Crumbs />
      <div className="page-head">
        <h1>{project.data?.name ?? '…'}</h1>
        <p className="mono muted">Project ID {projectId}</p>
      </div>

      <section aria-labelledby="flags">
        <h2 id="flags">Flags</h2>
        <ErrorText>{flags.error}</ErrorText>
        {flags.data && flags.data.length === 0 && <p className="empty">No flags yet. A new flag starts off in every environment.</p>}
        {flags.data && flags.data.length > 0 && (
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr><th>Key</th><th>Name</th><th>Description</th><th>Created</th><th><span className="sr-only">Actions</span></th></tr>
              </thead>
              <tbody>
                {flags.data.map((f) => (
                  <tr key={f.flagKey}>
                    <td><a className="link mono" href={flagHref(projectId, f.flagKey)}>{f.flagKey}</a></td>
                    <td>{f.friendlyName || <span className="muted">-</span>}</td>
                    <td className="muted">{f.description || '-'}</td>
                    <td className="muted nowrap">{formatDate(f.createdOn)}</td>
                    <td className="actions">
                      <button className="btn btn--ghost btn--danger" onClick={() => remove(f)} aria-label={`Delete flag ${f.flagKey}`}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel" aria-labelledby="new-flag">
        <h2 id="new-flag">New flag</h2>
        <form className="form-grid" onSubmit={create}>
          <label className="field">
            <span>Key</span>
            <input className="mono" value={form.flagKey} onChange={(e) => setForm({ ...form, flagKey: e.target.value })}
              required maxLength={100} placeholder="dark-mode" />
          </label>
          <label className="field">
            <span>Name</span>
            <input value={form.friendlyName} onChange={(e) => setForm({ ...form, friendlyName: e.target.value })}
              maxLength={255} placeholder="Dark mode" />
          </label>
          <label className="field field--wide">
            <span>Description</span>
            <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="What the flag switches on" />
          </label>
          <button className="btn btn--primary" disabled={busy || !form.flagKey.trim()}>Create flag</button>
        </form>
        <ErrorText>{error}</ErrorText>
      </section>
    </>
  )
}

function Crumbs() {
  return (
    <nav className="crumbs" aria-label="Breadcrumb">
      <a className="link" href="#/">Projects</a>
    </nav>
  )
}
