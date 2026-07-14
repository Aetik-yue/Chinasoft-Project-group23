export const PARROT_BEHAVIORS = Object.freeze({
  idle: '站立观察',
  hop: '跳跃',
  eating: '进食',
  drinking: '饮水',
  preening: '梳理羽毛',
  flying: '飞翔',
  climbing: '攀爬',
  sleeping: '睡觉',
  playing: '玩耍',
  defecating: '排泄',
  calling: '鸣叫',
})

const USER_ACTIONS = Object.freeze({
  feed: 'eating',
  water: 'drinking',
  play: 'playing',
})

const DURATIONS = Object.freeze({
  idle: [4, 7],
  hop: [2.4, 3.2],
  eating: [6, 8],
  drinking: [5, 7],
  preening: [5, 8],
  flying: [3.2, 4.2],
  climbing: [5, 7],
  sleeping: [9, 13],
  playing: [6, 9],
  defecating: [2.2, 3.2],
  calling: [3.5, 5.5],
})

const AUTONOMOUS_POOL = Object.freeze([
  'idle', 'idle', 'hop', 'preening', 'preening', 'flying', 'climbing', 'playing',
  'defecating', 'calling', 'calling',
])

const clamp = (value) => Math.min(100, Math.max(0, value))

export class ParrotBehaviorMachine {
  constructor({ random = Math.random, onChange = () => {} } = {}) {
    this.random = random
    this.onChange = onChange
    this.state = 'idle'
    this.source = 'autonomous'
    this.elapsed = 0
    this.duration = this.randomDuration('idle')
    this.needs = {
      satiety: 72,
      hydration: 76,
      energy: 84,
      stimulation: 64,
    }
  }

  randomDuration(state) {
    const [min, max] = DURATIONS[state] || DURATIONS.idle
    return min + (max - min) * this.random()
  }

  request(action) {
    const next = USER_ACTIONS[action]
    if (!next || this.source === 'user') return false
    this.transition(next, 'user')
    return true
  }

  transition(next, source = 'autonomous') {
    if (!Object.prototype.hasOwnProperty.call(PARROT_BEHAVIORS, next)) return false
    this.state = next
    this.source = source
    this.elapsed = 0
    this.duration = this.randomDuration(next)
    this.onChange(this.snapshot())
    return true
  }

  updateNeeds(delta) {
    const needs = this.needs
    needs.satiety -= 0.025 * delta
    needs.hydration -= 0.035 * delta
    needs.energy -= (this.state === 'sleeping' ? -1.4 : 0.018) * delta
    needs.stimulation -= (this.state === 'playing' ? -1.1 : 0.03) * delta

    if (this.state === 'eating') needs.satiety += 1.8 * delta
    if (this.state === 'drinking') needs.hydration += 2.2 * delta
    if (['flying', 'climbing', 'hop', 'playing'].includes(this.state)) {
      needs.energy -= 0.12 * delta
    }

    Object.keys(needs).forEach((key) => {
      needs[key] = clamp(needs[key])
    })
  }

  chooseAutonomousState() {
    if (this.needs.hydration < 45) return 'drinking'
    if (this.needs.satiety < 42) return 'eating'
    if (this.needs.energy < 30) return 'sleeping'
    if (this.needs.stimulation < 35) return 'playing'
    return AUTONOMOUS_POOL[Math.floor(this.random() * AUTONOMOUS_POOL.length)] || 'idle'
  }

  tick(deltaSeconds) {
    const delta = Math.min(0.25, Math.max(0, Number(deltaSeconds) || 0))
    this.updateNeeds(delta)
    this.elapsed += delta
    if (this.elapsed >= this.duration) {
      this.transition(this.chooseAutonomousState(), 'autonomous')
    }
    return this.snapshot()
  }

  snapshot() {
    return {
      key: this.state,
      label: PARROT_BEHAVIORS[this.state],
      source: this.source,
      progress: this.duration > 0 ? Math.min(1, this.elapsed / this.duration) : 0,
      needs: { ...this.needs },
    }
  }
}

export function createSeededRandom(seed = 1) {
  let value = seed >>> 0
  return () => {
    value = (value * 1664525 + 1013904223) >>> 0
    return value / 0x100000000
  }
}
