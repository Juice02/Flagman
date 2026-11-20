// Thin client for the FlagMan API. Requests go to /api, which the Vite dev server (vite.config.ts)
// and nginx (nginx.conf) forward to the API, so the browser never makes a cross-origin call.

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
    res = await fetch('/api' + path, {
      ...init,
      headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
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
