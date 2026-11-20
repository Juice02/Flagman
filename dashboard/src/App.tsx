import { useEffect, useState } from 'react'
import { FlagPage } from './FlagPage'
import { ProjectPage } from './ProjectPage'
import { ProjectsPage } from './ProjectsPage'
import { parseRoute } from './routes'

export default function App() {
  const [route, setRoute] = useState(() => parseRoute(location.hash))

  useEffect(() => {
    const onChange = () => {
      setRoute(parseRoute(location.hash))
      window.scrollTo(0, 0)
    }
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])

  return (
    <>
      <a className="skip" href="#main">Skip to content</a>
      <header className="topbar">
        <div className="wrap topbar__inner">
          <a className="brand" href="#/">
            <span className="brand__mark" aria-hidden="true" />
            FlagMan
          </a>
          <span className="topbar__note">Feature flags</span>
        </div>
      </header>
      <main id="main" className="wrap">
        {route.page === 'projects' && <ProjectsPage />}
        {route.page === 'project' && <ProjectPage key={route.projectId} projectId={route.projectId} />}
        {route.page === 'flag' && (
          <FlagPage key={route.projectId + route.flagKey} projectId={route.projectId} flagKey={route.flagKey} />
        )}
      </main>
    </>
  )
}
