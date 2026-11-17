import { Badge, Bar, Card, Connector, Divider, Phone, Roll, Stage, Text, Toggle, Window, type Act } from '../../kit'
import './flagman.css'

// FlagMan hero: a device pair. The dashboard (the config) and Book Logger on a
// phone (an app calling POST /evaluate) are separate products; one setting in
// the first changes the second. The user is u-42, whose bucket is
// CRC32("dark-mode:u-42") % 100 = 11, as the real API computes it, so the phone
// goes dark the moment the slider passes 11%, not at the end of the drag.

type S = { enabled: boolean; rollout: number }

const initial: S = { enabled: false, rollout: 0 }
const POSTER: S = { enabled: true, rollout: 50 }
const BUCKET = 11

const REST = { wide: { x: 300, y: 330 }, compact: { x: 120, y: 236 } }

function evaluate(s: S) {
  if (!s.enabled) return { on: false, reason: 'FLAG_DISABLED' }
  return BUCKET < s.rollout ? { on: true, reason: 'IN_ROLLOUT' } : { on: false, reason: 'OUT_OF_ROLLOUT' }
}

// Book covers keep it a reading app; titles and authors are filler.
const BOOKS = [
  { cover: '#c4703f', title: 112, author: 70 },
  { cover: '#3f6e8c', title: 136, author: 84 },
  { cover: '#7d8b4e', title: 96, author: 62 },
  { cover: '#8c4a5e', title: 124, author: 76 },
  { cover: '#c9a24a', title: 104, author: 58 },
]

const NAV = [52, 40, 58, 46]

async function slide(a: Act<S>, from: number, to: number) {
  const t = a.rect('track')
  const y = t.y + t.height / 2
  const x = (p: number) => t.x + (t.width * p) / 100
  await a.move({ x: x(from), y })
  await a.wait(150)
  await a.drag(
    { x: x(to), y },
    (at) => a.put({ rollout: Math.max(0, Math.min(100, Math.round(((at.x - t.x) / t.width) * 100))) }),
    { duration: 1500 },
  )
  await a.set({ rollout: to })
}

async function script(a: Act<S>) {
  await a.wait(700)

  // 1. Switch the flag on: u-42 is still outside a 0% rollout
  await a.move({ at: 'toggle', x: 0.5, y: 0.5 })
  await a.wait(200)
  await a.click()
  await a.set({ enabled: true })
  await a.wait(1000)

  // 2. Roll it out to half: the phone flips as the slider passes u-42's bucket
  await slide(a, 0, 50)
  await a.move('rest')
  await a.wait(2200)

  // 3. Reset with a visible cause: roll back, switch off
  await slide(a, 50, 0)
  await a.wait(300)
  await a.move({ at: 'toggle', x: 0.5, y: 0.5 })
  await a.wait(150)
  await a.click()
  await a.set({ enabled: false })
  await a.move('rest')
  await a.wait(700)
}

export default function FlagmanHero() {
  return (
    <Stage
      width={760}
      height={420}
      compact={{ below: 620, width: 358, height: 520 }}
      initial={initial}
      poster={POSTER}
      script={script}
      cursor={{ at: REST }}
      accent="#2563eb"
      className="fm"
    >
      {(s, rect, layout) => {
        const r = evaluate(s)
        // The app asks FlagMan on every change: a line from the config to the
        // chip that shows the answer, drawn once the flag is live.
        const card = rect('config')
        const chip = rect('why')
        const compact = layout === 'compact'
        // Wide: one level run from the card's edge to the chip. Narrow: straight down to it.
        const from = compact ? { x: chip.x + chip.width / 2, y: card.y + card.height } : { x: card.x + card.width, y: chip.y + chip.height / 2 }
        const to = compact ? { x: chip.x + chip.width / 2, y: chip.y } : { x: chip.x, y: chip.y + chip.height / 2 }
        return (
          <>
            <Window url="flagman.dev/book-logger" className="fm-window">
              <aside className="fm-sidebar">
                <span className="fm-wordmark">FlagMan</span>
                {NAV.map((w, i) => (
                  <div key={i} className="fm-nav" data-active={i === 0 || undefined}>
                    <Bar w={10} h={10} r={3} tone={i === 0 ? 4 : 2} />
                    <Bar w={w} h={5} tone={i === 0 ? 4 : 2} />
                  </div>
                ))}
              </aside>

              <div className="fm-main">
                <div className="fm-head">
                  <span className="fm-key">dark-mode</span>
                  <Badge tone="neutral">Production</Badge>
                </div>
                <Bar w={150} h={5} tone={2} />

                <Card pad="medium" gap={14} className="fm-config" data-target="config">
                  <div className="fm-setting">
                    <Text className="fm-label">Enabled</Text>
                    <Toggle on={s.enabled} data-target="toggle" data-cursor="hand" className="fm-toggle" />
                  </div>
                  <Divider />
                  <div className="fm-rollout">
                    <div className="fm-setting">
                      <Text className="fm-label">Rollout</Text>
                      <span className="fm-pct">{s.rollout}%</span>
                    </div>
                    <div className="fm-track" data-target="track">
                      <div className="fm-fill" style={{ width: `${s.rollout}%` }} />
                      <div className="fm-knob" data-target="knob" data-cursor="hand" style={{ left: `${s.rollout}%` }} />
                    </div>
                  </div>
                </Card>

                <div className="fm-rules">
                  <Bar w={44} h={6} tone={3} />
                  {[96, 72].map((w, i) => (
                    <div key={i} className="fm-rule">
                      <Bar w={w * 0.5} h={6} tone={2} />
                      <Bar w={40} h={14} r={4} tone={1} />
                      <Bar w={w} h={6} tone={2} />
                    </div>
                  ))}
                </div>
              </div>
            </Window>

            <Phone width={layout === 'compact' ? 248 : 236} dark={r.on} className="fm-phone">
              <div className="fm-app" data-dark={r.on || undefined}>
                <div className="fm-app-head">
                  <span className="fm-app-title">Reading list</span>
                  <span className="fm-why" data-target="why">
                    <span className="fm-why-dot" data-on={r.on || undefined} />
                    <span className="fm-why-key">u-42</span>
                    <Roll delay={150}>{r.reason}</Roll>
                  </span>
                </div>
                {BOOKS.map((b, i) => (
                  <div key={i} className="fm-book">
                    <span className="fm-cover" style={{ background: b.cover }} />
                    <span className="fm-book-lines">
                      <span className="fm-line fm-line--title" style={{ width: b.title }} />
                      <span className="fm-line" style={{ width: b.author }} />
                    </span>
                  </div>
                ))}
              </div>
            </Phone>

            <span data-target="rest" style={{ position: 'absolute', left: REST[layout].x, top: REST[layout].y }} />

            <Connector
              className="fm-request"
              from={from}
              to={to}
              route="straight"
              dots="none"
              show={!compact && s.enabled && card.width > 0 && chip.width > 0}
              label="/evaluate"
              labelAt={compact ? 0.5 : 0.42}
            />
          </>
        )
      }}
    </Stage>
  )
}
