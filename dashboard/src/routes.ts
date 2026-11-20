// Hash routes (#/projects/6767/flags/dark-mode) need no router library and no server rewrite rules.
type Route =
  | { page: 'projects' }
  | { page: 'project'; projectId: number }
  | { page: 'flag'; projectId: number; flagKey: string }

export function parseRoute(hash: string): Route {
  const [, projects, id, flags, key] = hash.replace(/^#/, '').split('/')
  const projectId = Number(id)
  if (projects === 'projects' && Number.isInteger(projectId)) {
    if (flags === 'flags' && key) return { page: 'flag', projectId, flagKey: decodeURIComponent(key) }
    return { page: 'project', projectId }
  }
  return { page: 'projects' }
}

export const projectHref = (projectId: number) => `#/projects/${projectId}`
export const flagHref = (projectId: number, flagKey: string) => `#/projects/${projectId}/flags/${encodeURIComponent(flagKey)}`
