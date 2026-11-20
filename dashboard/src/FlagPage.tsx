import { useState, type CSSProperties, type FormEvent, type KeyboardEvent } from 'react'
import { api, ENVIRONMENTS, OPERATORS, type Config, type Environment, type Operator } from './api'
import { projectHref } from './routes'
import { ErrorText, Switch } from './ui'
import { useLoad } from './lib'

const OPERATOR_HINT: Record<Operator, string> = {
  EQUALS: 'equals',
  NOT_EQUALS: 'does not equal',
  IN: 'is one of (comma-separated)',
  CONTAINS: 'contains',
}

export function FlagPage({ projectId, flagKey }: { projectId: number; flagKey: string }) {
  const project = useLoad(() => api.project(projectId), [projectId])
  const flag = useLoad(() => api.flags(projectId).then((list) => list.find((f) => f.flagKey === flagKey) ?? null), [projectId, flagKey])
  const configs = useLoad(
    () => Promise.all(ENVIRONMENTS.map((env) => api.config(projectId, flagKey, env))),
    [projectId, flagKey],
  )
  const [env, setEnv] = useState<Environment>('DEV')

  // The config request 404s with "Flag … not found in project …" for an unknown flag or project.
  const notFound = configs.error ?? project.error
  const config = configs.data?.find((c) => c.environment === env)

  // Arrow keys move between tabs, as the WAI-ARIA tabs pattern expects.
  function onTabKey(e: KeyboardEvent) {
    const step = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0
    if (!step) return
    const next = ENVIRONMENTS[(ENVIRONMENTS.indexOf(env) + step + ENVIRONMENTS.length) % ENVIRONMENTS.length]
    setEnv(next)
    document.getElementById(`tab-${next}`)?.focus()
  }

  const replaceConfig = (updated: Config) =>
    configs.setData((list) => list?.map((c) => (c.environment === updated.environment ? updated : c)) ?? null)

  return (
    <>
      <nav className="crumbs" aria-label="Breadcrumb">
        <a className="link" href="#/">Projects</a>
        <span aria-hidden="true">/</span>
        <a className="link" href={projectHref(projectId)}>{project.data?.name ?? `Project ${projectId}`}</a>
      </nav>

      {notFound ? (
        <ErrorText>{notFound}</ErrorText>
      ) : (
        <>
          <div className="page-head">
            <h1>{flag.data?.friendlyName || flagKey}</h1>
            <p className="mono muted">{flagKey}</p>
            {flag.data?.description && <p className="muted">{flag.data.description}</p>}
          </div>

          <div className="tabs" role="tablist" aria-label="Environment">
            {ENVIRONMENTS.map((e) => {
              const c = configs.data?.find((x) => x.environment === e)
              return (
                <button
                  key={e}
                  id={`tab-${e}`}
                  role="tab"
                  type="button"
                  className="tab"
                  aria-selected={env === e}
                  aria-controls="env-panel"
                  tabIndex={env === e ? 0 : -1}
                  onClick={() => setEnv(e)}
                  onKeyDown={onTabKey}
                >
                  <span className="tab__env">{e}</span>
                  <span className={'status' + (c?.enabled ? ' status--on' : '')}>
                    {c ? (c.enabled ? `On · ${c.rolloutPercentage}%` : 'Off') : '…'}
                  </span>
                </button>
              )
            })}
          </div>

          <div id="env-panel" role="tabpanel" aria-labelledby={`tab-${env}`} className="env-panel">
            {config && (
              <ConfigEditor key={env} projectId={projectId} flagKey={flagKey} config={config} onSaved={replaceConfig} />
            )}
            <Rules key={'rules-' + env} projectId={projectId} flagKey={flagKey} env={env} />
          </div>
        </>
      )}
    </>
  )
}

function ConfigEditor({ projectId, flagKey, config, onSaved }: {
  projectId: number
  flagKey: string
  config: Config
  onSaved: (c: Config) => void
}) {
  const [rollout, setRollout] = useState(config.rolloutPercentage)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function save(change: Partial<Omit<Config, 'environment'>>) {
    setBusy(true)
    setError(null)
    try {
      const updated = await api.updateConfig(projectId, flagKey, config.environment, change)
      onSaved(updated)
      setRollout(updated.rolloutPercentage)
    } catch (err) {
      setError((err as Error).message)
    }
    setBusy(false)
  }

  const dirty = rollout !== config.rolloutPercentage

  return (
    <section className="panel" aria-labelledby="config-title">
      <h2 id="config-title">Config in {config.environment}</h2>
      <div className="config">
        <div className="config__row">
          <div>
            <p className="config__label">Enabled</p>
            <p className="muted small">Off means no user gets the flag, whatever the rules and rollout say.</p>
          </div>
          <Switch
            checked={config.enabled}
            disabled={busy}
            label={`Enable ${flagKey} in ${config.environment}`}
            onChange={(enabled) => save({ enabled })}
          />
        </div>

        <div className="config__row">
          <div>
            <label className="config__label" htmlFor="rollout">Rollout</label>
            <p className="muted small">Share of users who pass the rules and get the flag. The same user always lands in the same bucket.</p>
          </div>
          <div className="rollout">
            <input
              id="rollout"
              type="range"
              min={0}
              max={100}
              value={rollout}
              onChange={(e) => setRollout(Number(e.target.value))}
              style={{ '--fill': `${rollout}%` } as CSSProperties}
            />
            <output htmlFor="rollout" className="mono rollout__value">{rollout}%</output>
            <button className="btn btn--primary" disabled={!dirty || busy} onClick={() => save({ rolloutPercentage: rollout })}>
              Save
            </button>
          </div>
        </div>
      </div>
      <ErrorText>{error}</ErrorText>
    </section>
  )
}

const emptyRule = { attribute: '', operator: 'EQUALS' as Operator, value: '' }

function Rules({ projectId, flagKey, env }: { projectId: number; flagKey: string; env: Environment }) {
  const rules = useLoad(() => api.rules(projectId, flagKey, env), [projectId, flagKey, env])
  const [form, setForm] = useState(emptyRule)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function add(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const rule = await api.createRule(projectId, flagKey, env, form)
      rules.setData((list) => [...(list ?? []), rule])
      setForm(emptyRule)
    } catch (err) {
      setError((err as Error).message)
    }
    setBusy(false)
  }

  async function remove(ruleId: number) {
    setError(null)
    try {
      await api.deleteRule(projectId, flagKey, env, ruleId)
      rules.setData((list) => list?.filter((r) => r.ruleId !== ruleId) ?? null)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <section className="panel" aria-labelledby="rules-title">
      <h2 id="rules-title">Rules in {env}</h2>
      <p className="muted small">A user must match every rule. A user without the attribute never matches.</p>

      <ErrorText>{rules.error}</ErrorText>
      {rules.data && rules.data.length === 0 && <p className="empty">No rules: every user goes straight to the rollout.</p>}
      {rules.data && rules.data.length > 0 && (
        <ul className="rules">
          {rules.data.map((r) => (
            <li key={r.ruleId} className="rule">
              <span className="mono rule__attr">{r.attribute}</span>
              <span className="rule__op">{OPERATOR_HINT[r.operator] ?? r.operator}</span>
              <span className="mono rule__value">{r.value}</span>
              <button className="btn btn--ghost btn--danger" onClick={() => remove(r.ruleId)}
                aria-label={`Remove rule ${r.attribute} ${r.operator} ${r.value}`}>
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}

      <form className="form-grid form-grid--rule" onSubmit={add}>
        <label className="field">
          <span>Attribute</span>
          <input className="mono" value={form.attribute} onChange={(e) => setForm({ ...form, attribute: e.target.value })}
            required maxLength={100} placeholder="country" />
        </label>
        <label className="field">
          <span>Operator</span>
          <select value={form.operator} onChange={(e) => setForm({ ...form, operator: e.target.value as Operator })}>
            {OPERATORS.map((o) => <option key={o} value={o}>{OPERATOR_HINT[o]}</option>)}
          </select>
        </label>
        <label className="field">
          <span>Value</span>
          <input className="mono" value={form.value} onChange={(e) => setForm({ ...form, value: e.target.value })}
            required maxLength={255} placeholder={form.operator === 'IN' ? 'IN,US,GB' : 'IN'} />
        </label>
        <button className="btn btn--primary" disabled={busy || !form.attribute.trim() || !form.value.trim()}>Add rule</button>
      </form>
      <ErrorText>{error}</ErrorText>
    </section>
  )
}
