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
