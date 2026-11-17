import { Badge, Bar, Card, Check, Collapse, Connector, Divider, Phone, Roll, Spinner, Stage, States, Text, Toggle, Window, type Act } from '../../kit'
import './killswitch.css'

// FlagMan clip: the kill switch. Shop's new-checkout is live and failing on
// the phone; switching the flag off in the dashboard sends the app back to
// the classic checkout on its next /evaluate call, and the order goes
// through. No deploy happens anywhere in the scene. The reset is the flag
// switched back on, which brings the new checkout back fresh.

type NewPay = 'idle' | 'busy' | 'failed'
type OldPay = 'idle' | 'busy' | 'done'
type S = { enabled: boolean; newPay: NewPay; oldPay: OldPay }

const initial: S = { enabled: true, newPay: 'idle', oldPay: 'idle' }
const POSTER: S = { enabled: false, newPay: 'idle', oldPay: 'done' }

const REST = { wide: { x: 300, y: 330 }, compact: { x: 120, y: 236 } }
const NAV = [52, 40, 58, 46]

async function script(a: Act<S>) {
  await a.wait(900)

  // 1. The new checkout fails
  await a.set({ newPay: 'busy' })
  await a.wait(1100)
  await a.set({ newPay: 'failed' })
  await a.wait(1500)

  // 2. Switch the flag off
  await a.move({ at: 'toggle', x: 0.5, y: 0.5 })
  await a.wait(200)
  await a.click()
  await a.set({ enabled: false })
  await a.wait(1300)

  // 3. The classic checkout takes the order
  await a.set({ oldPay: 'busy' })
  await a.wait(1000)
  await a.set({ oldPay: 'done' })
  await a.move('rest')
  await a.wait(2200)

  // 4. Reset with a visible cause: the flag back on, the new checkout fresh
  await a.move({ at: 'toggle', x: 0.5, y: 0.5 })
  await a.wait(150)
  await a.click()
  await a.set({ enabled: true, newPay: 'idle' })
  await a.wait(400)
  await a.snap({ oldPay: 'idle' })
  await a.move('rest')
  await a.wait(700)
}

export default function FlagmanKillSwitch() {
  return (
    <Stage
      width={760}
      height={420}
      compact={{ below: 620, width: 358, height: 600 }}
      initial={initial}
      poster={POSTER}
      script={script}
      cursor={{ at: REST }}
      accent="#2563eb"
      className="fk"
    >
      {(s, rect, layout) => {
        const compact = layout === 'compact'
        const reason = s.enabled ? 'IN_ROLLOUT' : 'FLAG_DISABLED'
        const card = rect('config')
        const chip = rect('why')
        const y = chip.y + chip.height / 2
        return (
          <>
            <Window url="flagman.dev/shop" className="fk-window">
              <aside className="fk-sidebar">
                <span className="fk-wordmark">FlagMan</span>
                {NAV.map((w, i) => (
                  <div key={i} className="fk-nav" data-active={i === 0 || undefined}>
                    <Bar w={10} h={10} r={3} tone={i === 0 ? 4 : 2} />
                    <Bar w={w} h={5} tone={i === 0 ? 4 : 2} />
                  </div>
                ))}
              </aside>

              <div className="fk-main">
                <div className="fk-head">
                  <span className="fk-key">new-checkout</span>
                  <Badge tone="neutral">Production</Badge>
                </div>
                <Bar w={150} h={5} tone={2} />

                <Card pad="medium" gap={14} className="fk-config" data-target="config">
                  <div className="fk-setting">
                    <Text className="fk-label">Enabled</Text>
                    <Toggle on={s.enabled} data-target="toggle" data-cursor="hand" className="fk-toggle" />
                  </div>
                  <Divider />
                  <div className="fk-rollout">
                    <div className="fk-setting">
                      <Text className="fk-label">Rollout</Text>
                      <span className="fk-pct">100%</span>
                    </div>
                    <div className="fk-track">
                      <div className="fk-fill" data-off={!s.enabled || undefined} />
                    </div>
                  </div>
                </Card>

                <div className="fk-rules">
                  <Bar w={44} h={6} tone={3} />
                  {[96, 72].map((w, i) => (
                    <div key={i} className="fk-rule">
                      <Bar w={w * 0.5} h={6} tone={2} />
                      <Bar w={40} h={14} r={4} tone={1} />
                      <Bar w={w} h={6} tone={2} />
                    </div>
                  ))}
                </div>
              </div>
            </Window>

            <Phone width={compact ? 248 : 236} className="fk-phone">
              <div className="fk-app">
                <div className="fk-app-head">
                  <span className="fk-app-title">Checkout</span>
                  <span className="fk-why" data-target="why">
                    <span className="fk-why-dot" data-on={s.enabled || undefined} />
                    <span className="fk-why-key">new-checkout</span>
                    <Roll delay={150}>{reason}</Roll>
                  </span>
                </div>

                <div className="fk-screens">
                  {/* The new one-page checkout: what the flag turns on */}
                  <div className="fk-screen" data-show={s.enabled || undefined}>
                    <div className="fk-summary">
                      <div className="fk-sum-row"><Bar w={120} h={9} tone={3} /><Bar w={44} h={9} tone={2} /></div>
                      <div className="fk-sum-row"><Bar w={96} h={9} tone={3} /><Bar w={44} h={9} tone={2} /></div>
                      <div className="fk-sum-row fk-total"><span>Total</span><span>$48.00</span></div>
                    </div>
                    <div className="fk-field"><Bar w={150} h={9} tone={2} /></div>
                    <Collapse open={s.newPay === 'failed'} reveal="drop">
                      <div className="fk-error">Payment failed</div>
                    </Collapse>
                    <div className="fk-button" data-busy={s.newPay === 'busy' || undefined}>
                      <States
                        value={s.newPay}
                        states={{
                          idle: <span>Pay $48.00</span>,
                          busy: <Spinner size={22} />,
                          failed: <span>Try again</span>,
                        }}
                      />
                    </div>
                    <Below />
                  </div>

                  {/* The classic checkout: what everyone gets with the flag off */}
                  <div className="fk-screen" data-show={!s.enabled || undefined}>
                    <div className="fk-steps">
                      <span className="fk-steps-label">Step 3 of 3</span>
                      <span className="fk-steps-bar"><span /><span /><span /></span>
                    </div>
                    <div className="fk-summary">
                      <div className="fk-sum-row"><Bar w={120} h={9} tone={3} /><Bar w={44} h={9} tone={2} /></div>
                      <div className="fk-sum-row"><Bar w={96} h={9} tone={3} /><Bar w={44} h={9} tone={2} /></div>
                      <div className="fk-sum-row fk-total"><span>Total</span><span>$48.00</span></div>
                    </div>
                    <div className="fk-button fk-button--classic" data-done={s.oldPay === 'done' || undefined}>
                      <States
                        value={s.oldPay}
                        states={{
                          idle: <span>Place order</span>,
                          busy: <Spinner size={22} />,
                          done: (
                            <span className="fk-done">
                              <Check on size={22} />
                              Order placed
                            </span>
                          ),
                        }}
                      />
                    </div>
                    <Below />
                  </div>
                </div>
              </div>
            </Phone>

            <span data-target="rest" style={{ position: 'absolute', left: REST[layout].x, top: REST[layout].y }} />

            <Connector
              className="fk-request"
              from={{ x: card.x + card.width, y }}
              to={{ x: chip.x, y }}
              route="straight"
              dots="none"
              show={!compact && card.width > 0 && chip.width > 0}
              label="/evaluate"
              labelAt={0.42}
            />
          </>
        )
      }}
    </Stage>
  )
}

// Filler under the button: the rest of the page, cut by the stage's foot
function Below() {
  return (
    <div className="fk-below">
      <Bar w={110} h={9} tone={2} />
      <div className="fk-below-card">
        <Bar w={140} h={9} tone={2} />
        <Bar w={90} h={9} tone={1} />
      </div>
    </div>
  )
}
