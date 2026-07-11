import test from 'node:test'
import assert from 'node:assert/strict'
import { createSeededRandom } from './parrotBehaviorMachine.js'
import { ParrotSimulationRuntime } from './parrotSimulationRuntime.js'

test('simulation keeps advancing while no 3D view is subscribed', () => {
  const runtime = new ParrotSimulationRuntime({ random: createSeededRandom(31) })
  const firstView = []
  const unsubscribe = runtime.subscribe((snapshot) => firstView.push(snapshot.key))
  runtime.advance(5)
  unsubscribe()

  const beforeRoute = runtime.snapshot()
  runtime.advance(18)
  const afterRoute = runtime.snapshot()

  assert.ok(afterRoute.elapsed > beforeRoute.elapsed)
  assert.notEqual(afterRoute.progress, beforeRoute.progress)
  assert.equal(firstView.length > 0, true)
})

test('user interaction and life needs survive view unsubscribe and resubscribe', () => {
  const runtime = new ParrotSimulationRuntime({ random: createSeededRandom(41) })
  assert.equal(runtime.request('feed'), true)
  const eating = runtime.snapshot()
  assert.equal(eating.key, 'eating')
  assert.equal(eating.source, 'user')
  assert.ok(eating.environment.foodLevel > 0.7)

  runtime.advance(2)
  let restored = runtime.snapshot()
  const unsubscribe = runtime.subscribe((snapshot) => { restored = snapshot })
  runtime.advance(0.25)
  unsubscribe()

  assert.equal(restored.key, 'eating')
  assert.ok(restored.progress > eating.progress)
  assert.equal(restored.environment.foodLevel, eating.environment.foodLevel)
})
