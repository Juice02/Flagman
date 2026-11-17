import { Bar, Card, Divider, Phone, Stage, Text, type Act } from '../../kit'
import './rollout.css'

// FlagMan clip 2: what a rollout percentage means. Two users on Book Logger,
// each with a fixed bucket from the real formula, CRC32("dark-mode:" + id) % 100:
// u-42 is 11, u-1 is 64. One slow drag of the rollout turns u-42 on at 12% and
// u-1 at 65%; the 50% poster shows one dark phone and one light.
// The phones are the hero; the config card is the satellite over their margin.

type S = { rollout: number }

const initial: S = { rollout: 0 }
const POSTER: S = { rollout: 50 }

const USERS = [
  { id: 'u-42', bucket: 11 },
  { id: 'u-1', bucket: 64 },
]

const REST = { wide: { x: 640, y: 250 }, compact: { x: 300, y: 136 } }

const BOOKS = [
  { cover: '#c4703f', title: 112, author: 70 },
  { cover: '#3f6e8c', title: 136, author: 84 },
  { cover: '#7d8b4e', title: 96, author: 62 },
  { cover: '#8c4a5e', title: 124, author: 76 },
  { cover: '#c9a24a', title: 104, author: 58 },
]

async function slide(a: Act<S>, from: number, to: number, duration: number) {
  const t = a.rect('track')
  const y = t.y + t.height / 2
  const x = (p: number) => t.x + (t.width * p) / 100
  await a.move({ x: x(from), y })
  await a.wait(150)
  await a.drag(
    { x: x(to), y },
    // The knob glides with the hand; the readout and the answer use whole percents
    (at) => a.put({ rollout: Math.max(0, Math.min(100, ((at.x - t.x) / t.width) * 100)) }),
    { duration },
  )
  await a.set({ rollout: to })
}

async function script(a: Act<S>) {
  await a.wait(800)
  // To half: u-42 (bucket 11) turns on, u-1 (bucket 64) does not. The lesson
  // gets its own beat, with the hand held still on the knob.
  await slide(a, 0, 50, 2000)
  await a.wait(1600)
  // On to everyone: u-1 turns on as the knob passes 64
  await slide(a, 50, 100, 1600)
  await a.move('rest')
  await a.wait(1600)
  // Back down: they turn off in reverse, u-1 first
  await slide(a, 100, 0, 2400)
  await a.move('rest')
  await a.wait(900)
}

export default function FlagmanRollout() {
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
      className="fr"
    >
      {(s, _rect, layout) => {
        const pct = Math.round(s.rollout)
        return (
        <>
          {USERS.map((u, i) => {
            const on = u.bucket < pct
            return (
              <Phone key={u.id} width={layout === 'compact' ? 250 : 256} dark={on} className={`fr-phone fr-phone--${i}`}>
                <div className="fr-app" data-dark={on || undefined}>
                  <div className="fr-app-head">
                    <span className="fr-app-title">Reading list</span>
                    <span className="fr-who">
                      <span className="fr-who-dot" data-on={on || undefined} />
                      <span className="fr-who-id">{u.id}</span>
                      <span className="fr-who-bucket">bucket {u.bucket}</span>
                    </span>
                  </div>
                  {BOOKS.map((b, j) => (
                    <div key={j} className="fr-book">
                      <span className="fr-cover" style={{ background: b.cover }} />
                      <span className="fr-book-lines">
                        <span className="fr-line fr-line--title" style={{ width: b.title }} />
                        <span className="fr-line" style={{ width: b.author }} />
                      </span>
                    </div>
                  ))}
                </div>
              </Phone>
            )
          })}

          <Card pad="medium" gap={14} className="fr-config">
            <span className="fr-key">dark-mode</span>
            <div className="fr-setting">
              <Text className="fr-label">Rollout</Text>
              <span className="fr-pct">{pct}%</span>
            </div>
            <div className="fr-track" data-target="track">
              <div className="fr-fill" style={{ width: `${s.rollout}%` }} />
              {USERS.map((u) => (
                <span key={u.id} className="fr-tick" style={{ left: `${u.bucket}%` }} />
              ))}
              <div className="fr-knob" data-target="knob" data-cursor="hand" style={{ left: `${s.rollout}%` }} />
            </div>
            <Divider />
            {[120, 92, 108].map((w, i) => (
              <div key={i} className="fr-filler">
                <Bar w={w * 0.45} h={6} tone={2} />
                <Bar w={w} h={6} tone={1} />
              </div>
            ))}
          </Card>

          <span data-target="rest" style={{ position: 'absolute', left: REST[layout].x, top: REST[layout].y }} />
        </>
        )
      }}
    </Stage>
  )
}
