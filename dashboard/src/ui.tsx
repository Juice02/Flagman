import { useState } from 'react'
import { readAdminKey, saveAdminKey } from './api'

export function ErrorText({ children }: { children: string | null }) {
  return children ? <p className="error" role="alert">{children}</p> : null
}

export function Switch({ checked, onChange, label, disabled }: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
  disabled?: boolean
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      className="switch"
      disabled={disabled}
      onClick={() => onChange(!checked)}
    >
      <span className="switch__thumb" />
    </button>
  )
}

// Where the admin enters the key that changes need on a hosted FlagMan. Kept for this browser tab only.
export function AdminKey() {
  const [isSet, setIsSet] = useState(() => readAdminKey() !== null)
  const [value, setValue] = useState('')

  if (isSet) {
    return (
      <span className="admin-key topbar__note">
        Admin key set
        <button type="button" className="btn btn--ghost" onClick={() => { saveAdminKey(null); setIsSet(false) }}>Forget</button>
      </span>
    )
  }
  return (
    <form className="admin-key" onSubmit={(e) => {
      e.preventDefault()
      if (!value.trim()) return
      saveAdminKey(value.trim())
      setValue('')
      setIsSet(true)
    }}>
      <label className="sr-only" htmlFor="admin-key">Admin key</label>
      <input id="admin-key" type="password" autoComplete="off" placeholder="Admin key" value={value} onChange={(e) => setValue(e.target.value)} />
      <button type="submit" className="btn btn--ghost">Use</button>
    </form>
  )
}
