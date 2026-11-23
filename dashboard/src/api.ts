// Thin client for the FlagMan API. Requests go to /api, which the Vite dev server (vite.config.ts)
// and nginx (nginx.conf) forward to the API, so the browser never makes a cross-origin call.
// When the dashboard is hosted apart from the API, VITE_FLAGMAN_API_URL is the API's address instead.
const BASE: string = import.meta.env.VITE_FLAGMAN_API_URL || '/api'

// The admin key every change needs once FLAGMAN_ADMIN_KEY is set on the server. Typed in by the
// user (AdminKey in ui.tsx) and kept for this browser tab only, never built into the code.
const KEY_STORAGE = 'flagman-admin-key'
export function readAdminKey(): string | null {
  try { return sessionStorage.getItem(KEY_STORAGE) } catch { return null }
}
export function saveAdminKey(key: string | null) {
  try { if (key) sessionStorage.setItem(KEY_STORAGE, key); else sessionStorage.removeItem(KEY_STORAGE) } catch { /* storage blocked */ }
}

export const ENVIRONMENTS = ['DEV', 'STAGING', 'PREPROD', 'PROD'] as const
export type Environment = (typeof ENVIRONMENTS)[number]

export const OPERATORS = ['EQUALS', 'NOT_EQUALS', 'IN', 'CONTAINS'] as const
export type Operator = (typeof OPERATORS)[number]

export type Project = { projectId: number; name: string; createdOn: string }
export type Flag = { projectId: number; flagKey: string; friendlyName: string | null; description: string | null; createdOn: string }
export type Config = { environment: Environment; enabled: boolean; rolloutPercentage: number }
export type Rule = { ruleId: number; attribute: string; operator: Operator; value: string }

// Errors from the API are RFC 9457 problem details; `detail` is the readable message.
export class ApiError extends Error {}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    const key = readAdminKey()
    res = await fetch(BASE + path, {
      ...init,
      headers: { ...(init?.body ? { 'Content-Type': 'application/json' } : {}), ...(key ? { 'X-Admin-Key': key } : {}) },
    })
  } catch {
    throw new ApiError('Cannot reach the FlagMan API. Is it running (make up)?')
  }
  if (!res.ok) {
    const problem = await res.json().catch(() => null)
    throw new ApiError(problem?.detail ?? `Request failed (${res.status})`)
  }
  return (res.status === 204 ? undefined : await res.json()) as T
}

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) })
const flagPath = (projectId: number, flagKey: string) => `/projects/${projectId}/flags/${encodeURIComponent(flagKey)}`
const configPath = (projectId: number, flagKey: string, env: Environment) => `${flagPath(projectId, flagKey)}/configs/${env}`

export const api = {
  projects: () => request<Project[]>('/projects'),
  project: (projectId: number) => request<Project>(`/projects/${projectId}`),
  createProject: (name: string) => request<Project>('/projects', json('POST', { name })),

  flags: (projectId: number) => request<Flag[]>(`/projects/${projectId}/flags`),
  createFlag: (projectId: number, flag: { flagKey: string; friendlyName: string; description: string }) =>
    request<Flag>(`/projects/${projectId}/flags`, json('POST', flag)),
  deleteFlag: (projectId: number, flagKey: string) => request<void>(flagPath(projectId, flagKey), { method: 'DELETE' }),

  config: (projectId: number, flagKey: string, env: Environment) => request<Config>(configPath(projectId, flagKey, env)),
  updateConfig: (projectId: number, flagKey: string, env: Environment, change: Partial<Omit<Config, 'environment'>>) =>
    request<Config>(configPath(projectId, flagKey, env), json('PATCH', change)),

  rules: (projectId: number, flagKey: string, env: Environment) => request<Rule[]>(`${configPath(projectId, flagKey, env)}/rules`),
  createRule: (projectId: number, flagKey: string, env: Environment, rule: Omit<Rule, 'ruleId'>) =>
    request<Rule>(`${configPath(projectId, flagKey, env)}/rules`, json('POST', rule)),
  deleteRule: (projectId: number, flagKey: string, env: Environment, ruleId: number) =>
    request<void>(`${configPath(projectId, flagKey, env)}/rules/${ruleId}`, { method: 'DELETE' }),
}
