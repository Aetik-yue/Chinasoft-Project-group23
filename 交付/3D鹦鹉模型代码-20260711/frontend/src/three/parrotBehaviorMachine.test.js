import test from 'node:test'
import assert from 'node:assert/strict'
import { PARROT_BEHAVIORS, ParrotBehaviorMachine, createSeededRandom } from './parrotBehaviorMachine.js'

test('all autonomous transitions stay inside the supported behavior set', () => {
  const machine = new ParrotBehaviorMachine({ random: createSeededRandom(7) })
  for (let i = 0; i < 5000; i += 1) {
    const state = machine.tick(0.25)
    assert.ok(PARROT_BEHAVIORS[state.key])
  }
})

test('defecating, sleeping, and calling are supported life behaviors', () => {
  const machine = new ParrotBehaviorMachine({ random: createSeededRandom(5) })
  const expected = ['defecating', 'sleeping', 'calling']
  expected.forEach((behavior) => {
    assert.ok(PARROT_BEHAVIORS[behavior])
    assert.equal(machine.transition(behavior), true)
    assert.equal(machine.snapshot().key, behavior)
    assert.ok(machine.duration > 0)
  })
})

test('a user interaction preempts autonomous behavior and rejects stacking', () => {
  const machine = new ParrotBehaviorMachine({ random: createSeededRandom(11) })
  assert.equal(machine.request('feed'), true)
  assert.equal(machine.snapshot().key, 'eating')
  assert.equal(machine.snapshot().source, 'user')
  assert.equal(machine.request('water'), false)
  assert.equal(machine.request('play'), false)

  for (let i = 0; i < 40; i += 1) machine.tick(0.25)
  assert.equal(machine.snapshot().source, 'autonomous')
  assert.equal(machine.request('water'), true)
  assert.equal(machine.snapshot().key, 'drinking')
})

test('needs stay clamped between zero and one hundred', () => {
  const machine = new ParrotBehaviorMachine({ random: createSeededRandom(19) })
  machine.needs = { satiety: 0.001, hydration: 0.001, energy: 99.999, stimulation: 99.999 }
  for (let i = 0; i < 20000; i += 1) machine.tick(0.25)
  Object.values(machine.snapshot().needs).forEach((value) => {
    assert.ok(value >= 0 && value <= 100)
  })
})

test('a fixed seed produces a reproducible sequence', () => {
  const run = () => {
    const sequence = []
    const machine = new ParrotBehaviorMachine({
      random: createSeededRandom(23),
      onChange: ({ key }) => sequence.push(key),
    })
    for (let i = 0; i < 600; i += 1) machine.tick(0.25)
    return sequence
  }
  assert.deepEqual(run(), run())
})
