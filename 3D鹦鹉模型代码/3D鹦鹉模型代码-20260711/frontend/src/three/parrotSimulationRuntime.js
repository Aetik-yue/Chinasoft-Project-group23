import { ParrotBehaviorMachine } from './parrotBehaviorMachine.js'

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

export class ParrotSimulationRuntime {
  constructor({ random = Math.random, autoStart = false, intervalMs = 100 } = {}) {
    this.listeners = new Set()
    this.elapsed = 0
    this.environment = {
      foodLevel: 0.7,
      waterLevel: 0.05,
      lastFeedAt: -100,
      lastWaterAt: -100,
      lastPlayAt: -100,
    }
    this.machine = new ParrotBehaviorMachine({
      random,
      onChange: () => this.notify(),
    })
    this.lastTime = this.now()
    this.timer = 0
    if (autoStart && typeof window !== 'undefined') {
      this.timer = window.setInterval(() => this.sync(), intervalMs)
    }
  }

  now() {
    return typeof performance !== 'undefined' ? performance.now() : Date.now()
  }

  sync() {
    const current = this.now()
    const delta = Math.min(30, Math.max(0, (current - this.lastTime) / 1000))
    this.lastTime = current
    this.advance(delta)
  }

  advance(deltaSeconds) {
    let remaining = Math.max(0, Number(deltaSeconds) || 0)
    while (remaining > 0) {
      const step = Math.min(0.25, remaining)
      this.elapsed += step
      this.machine.tick(step)
      remaining -= step
    }
    return this.snapshot()
  }

  request(action) {
    if (!this.machine.request(action)) return false
    if (action === 'feed') {
      this.environment.foodLevel = clamp(this.environment.foodLevel + 0.28, 0.7, 1.25)
      this.environment.lastFeedAt = this.elapsed
    }
    if (action === 'water') {
      this.environment.waterLevel = 0.16
      this.environment.lastWaterAt = this.elapsed
    }
    if (action === 'play') this.environment.lastPlayAt = this.elapsed
    return true
  }

  subscribe(listener) {
    if (typeof listener !== 'function') return () => {}
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  notify() {
    const snapshot = this.snapshot()
    this.listeners.forEach((listener) => listener(snapshot))
  }

  snapshot() {
    return {
      ...this.machine.snapshot(),
      elapsed: this.elapsed,
      environment: { ...this.environment },
    }
  }

  destroy() {
    if (this.timer && typeof window !== 'undefined') window.clearInterval(this.timer)
    this.timer = 0
    this.listeners.clear()
  }
}

export const parrotSimulationRuntime = new ParrotSimulationRuntime({ autoStart: true })
