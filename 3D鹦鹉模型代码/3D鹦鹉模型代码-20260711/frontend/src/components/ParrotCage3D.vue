<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as THREE from 'three'
import ParrotVisual from './ParrotVisual.vue'
import { createParrotModel } from '../three/parrotModel'
import { createParrotCageScene } from '../three/parrotCageScene'
import { parrotSimulationRuntime } from '../three/parrotSimulationRuntime'

const props = defineProps({
  active: { type: Boolean, default: true },
  locale: { type: String, default: 'zh' },
})

const emit = defineEmits(['behavior-change', 'interaction-state', 'ready', 'error', 'resize'])

const canvas = ref(null)
const viewport = ref(null)
const loading = ref(true)
const failure = ref('')
const debugView = ref('front')
const debugBoxVisible = ref(false)

const PARROT_MODEL_DEBUG = import.meta.env.DEV
  && typeof window !== 'undefined'
  && new URLSearchParams(window.location.search).get('parrotDebug') === '1'

let renderer = null
let scene = null
let camera = null
let resizeObserver = null
let unsubscribeSimulation = null
let cage = null
let parrot = null
let elapsed = 0
let running = false
let reducedMotion = false
let reducedMotionQuery = null
let movement = null
let currentAnchor = 'main'
let alternateAnchor = 'upper'
let pointerDown = null
let disposed = false
let debugBoxHelper = null

const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()

const FALLBACK_COPY = {
  zh: '当前设备无法启动 3D 鸟笼，已显示静态鹦鹉。',
  en: '3D cage is unavailable on this device. Showing a static parrot.',
  es: 'La jaula 3D no está disponible. Se muestra un loro estático.',
  ja: 'この端末では3D鳥かごを表示できないため、静止画を表示しています。',
}

function addRoom() {
  scene.background = new THREE.Color('#d9e8e9')
  if (!PARROT_MODEL_DEBUG) {
    scene.fog = new THREE.Fog('#d9e8e9', 15, 25)
    const wallMaterial = new THREE.MeshStandardMaterial({ color: '#eadfcf', roughness: 0.92 })
    const wall = new THREE.Mesh(new THREE.PlaneGeometry(24, 14), wallMaterial)
    wall.position.set(0, 5, -2.8)
    wall.receiveShadow = true
    scene.add(wall)
    const floorMaterial = new THREE.MeshStandardMaterial({ color: '#c6a57d', roughness: 0.88 })
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(24, 16), floorMaterial)
    floor.rotation.x = -Math.PI / 2
    floor.position.y = 0.02
    floor.receiveShadow = true
    scene.add(floor)
  }

  const hemisphere = new THREE.HemisphereLight('#fff8e7', '#d8cfc0', 2)
  scene.add(hemisphere)
  const ambient = new THREE.AmbientLight('#fffaf0', 0.24)
  scene.add(ambient)
  const keyLight = new THREE.DirectionalLight('#fff3db', 1)
  keyLight.position.set(5.5, 9, 8)
  keyLight.castShadow = true
  keyLight.shadow.mapSize.set(1024, 1024)
  keyLight.shadow.camera.left = -6
  keyLight.shadow.camera.right = 6
  keyLight.shadow.camera.top = 7
  keyLight.shadow.camera.bottom = -1
  keyLight.shadow.radius = 4
  keyLight.shadow.bias = -0.0002
  keyLight.shadow.normalBias = 0.025
  keyLight.shadow.blurSamples = 8
  scene.add(keyLight)
  const fillLight = new THREE.DirectionalLight('#d7eef0', 1.1)
  fillLight.position.set(-6, 4, 5)
  scene.add(fillLight)
}

function rememberBaseTransforms(parts, root) {
  return {
    rootScale: root.scale.clone(),
    bodyScale: parts.body.scale.clone(),
    headRotation: parts.head.rotation.clone(),
    leftWingRotation: parts.leftWing.rotation.clone(),
    rightWingRotation: parts.rightWing.rotation.clone(),
    leftWingFeatherRotation: parts.leftWingFeather.rotation.clone(),
    rightWingFeatherRotation: parts.rightWingFeather.rotation.clone(),
    tailRotation: parts.tail.rotation.clone(),
    leftFootRotation: parts.leftFoot.rotation.clone(),
    rightFootRotation: parts.rightFoot.rotation.clone(),
    lowerBeakPosition: parts.lowerBeak.position.clone(),
    droppingPosition: parts.dropping.position.clone(),
    eyeScales: parts.eyes.map((eye) => eye.scale.clone()),
  }
}

function placementForAnchor(name) {
  const position = (cage.anchors[name] || cage.anchors.main).clone()
  position.y -= Number(parrot?.root?.userData?.perchContactY) || 0
  return position
}

function targetForBehavior(state) {
  if (state === 'eating') return 'feeder'
  if (state === 'drinking') return 'water'
  if (state === 'sleeping') return 'sleep'
  if (state === 'climbing') return 'climb'
  if (state === 'playing') return 'toy'
  if (state === 'flying' || state === 'hop') {
    const target = alternateAnchor
    alternateAnchor = alternateAnchor === 'upper' ? 'main' : 'upper'
    return target
  }
  return currentAnchor
}

const yawByAnchor = {
  main: 0.18,
  upper: 0.15,
  feeder: -Math.PI / 2,
  water: Math.PI / 2,
  sleep: -0.3,
  climb: -0.75,
  toy: Math.PI / 2,
}

function restoredAnchorForBehavior(state) {
  if (state === 'eating') return 'feeder'
  if (state === 'drinking') return 'water'
  if (state === 'sleeping') return 'sleep'
  if (state === 'climbing') return 'climb'
  if (state === 'playing') return 'toy'
  if (state === 'flying' || state === 'hop') return 'upper'
  return 'main'
}

function startMovement(state) {
  const targetName = targetForBehavior(state)
  const target = placementForAnchor(targetName)
  const from = parrot.root.position.clone()
  const distance = from.distanceTo(target)
  movement = {
    state,
    targetName,
    from,
    to: target.clone(),
    fromYaw: parrot.root.rotation.y,
    toYaw: yawByAnchor[targetName] ?? 0.18,
    startedAt: elapsed,
    duration: reducedMotion ? 0.45 : Math.max(0.75, Math.min(2.2, distance * 0.48)),
  }
  currentAnchor = targetName
}

function restorePlacement(snapshot) {
  const targetName = restoredAnchorForBehavior(snapshot.key)
  currentAnchor = targetName
  alternateAnchor = targetName === 'upper' ? 'main' : 'upper'
  movement = null
  parrot.root.position.copy(placementForAnchor(targetName))
  parrot.root.rotation.y = yawByAnchor[targetName] ?? 0.18
}

function handleBehaviorChange(snapshot, { restore = false } = {}) {
  if (!parrot || !cage) return
  elapsed = Number(snapshot.elapsed) || elapsed
  if (restore) restorePlacement(snapshot)
  else startMovement(snapshot.key)
  const payload = {
    key: snapshot.key,
    label: snapshot.label,
    source: snapshot.source,
    startedAt: new Date().toISOString(),
  }
  emit('behavior-change', payload)
  emit('interaction-state', { busy: snapshot.source === 'user', action: snapshot.key })
}

function updateMovement() {
  if (!movement) return
  const raw = Math.min(1, Math.max(0, (elapsed - movement.startedAt) / movement.duration))
  const progress = raw * raw * (3 - 2 * raw)
  parrot.root.position.lerpVectors(movement.from, movement.to, progress)
  parrot.root.rotation.y = THREE.MathUtils.lerp(movement.fromYaw, movement.toYaw, progress)
  if (movement.state === 'flying') {
    parrot.root.position.y += Math.sin(raw * Math.PI) * 0.62
  } else if (movement.state === 'hop') {
    parrot.root.position.y += Math.sin(raw * Math.PI) * 0.28
  }
  if (raw >= 1) movement = null
}

function updatePose(snapshot) {
  const { parts, base } = parrot
  const t = elapsed
  const progress = snapshot.progress
  const breath = 1 + Math.sin(t * 2.4) * (reducedMotion ? 0.006 : 0.016)
  parts.body.scale.copy(base.bodyScale)
  parts.body.scale.multiplyScalar(breath)
  parts.head.rotation.copy(base.headRotation)
  parts.leftWing.rotation.copy(base.leftWingRotation)
  parts.rightWing.rotation.copy(base.rightWingRotation)
  parts.leftWingFeather.rotation.copy(base.leftWingFeatherRotation)
  parts.rightWingFeather.rotation.copy(base.rightWingFeatherRotation)
  parts.tail.rotation.copy(base.tailRotation)
  parts.leftFoot.rotation.copy(base.leftFootRotation)
  parts.rightFoot.rotation.copy(base.rightFootRotation)
  parts.lowerBeak.position.copy(base.lowerBeakPosition)
  parts.dropping.position.copy(base.droppingPosition)
  parts.dropping.visible = false
  parts.leftFoot.visible = true
  parts.rightFoot.visible = true
  parrot.root.rotation.x = 0
  parrot.root.rotation.z = 0
  parrot.root.scale.copy(base.rootScale)

  const blink = Math.sin(t * 1.7) > 0.965 ? 0.12 : 1
  parts.eyes.forEach((eye, index) => {
    eye.scale.copy(base.eyeScales[index])
    eye.scale.y *= blink
  })

  if (snapshot.key === 'idle') {
    parts.head.rotation.y = Math.sin(t * 0.72) * (reducedMotion ? 0.08 : 0.24)
    parts.head.rotation.x = Math.sin(t * 0.46) * 0.06
  }
  if (snapshot.key === 'eating') {
    const peck = Math.max(0, Math.sin(t * 6.6))
    parts.head.rotation.x = 0.58 + peck * 0.2
    parts.lowerBeak.position.y -= Math.sin(t * 9) * 0.035
    parts.tail.rotation.x = base.tailRotation.x - 0.14
  }
  if (snapshot.key === 'drinking') {
    const dip = (Math.sin(t * 4.5) + 1) * 0.5
    parts.head.rotation.x = 0.42 + dip * 0.68
    parts.lowerBeak.position.y -= Math.sin(t * 7) * 0.025
  }
  if (snapshot.key === 'preening') {
    const side = Math.sin(progress * Math.PI * 2) > 0 ? 1 : -1
    parts.head.rotation.y = side * 1.08
    parts.head.rotation.z = side * -0.72
    parts.head.rotation.x = 0.38 + Math.sin(t * 5.2) * 0.12
    if (side > 0) parts.rightWing.rotation.x = base.rightWingRotation.x - 0.15
    else parts.leftWing.rotation.x = base.leftWingRotation.x - 0.15
  }
  if (snapshot.key === 'flying') {
    const flap = Math.sin(t * 11.5)
    const featherLag = Math.sin(t * 11.5 - 0.42)
    parts.leftWing.rotation.z = base.leftWingRotation.z - 0.55 - flap * 0.58
    parts.rightWing.rotation.z = base.rightWingRotation.z + 0.55 + flap * 0.58
    parts.leftWing.rotation.x = base.leftWingRotation.x + Math.cos(t * 11.5) * 0.08
    parts.rightWing.rotation.x = base.rightWingRotation.x + Math.cos(t * 11.5) * 0.08
    parts.leftWingFeather.rotation.z = base.leftWingFeatherRotation.z - 0.16 - featherLag * 0.22
    parts.rightWingFeather.rotation.z = base.rightWingFeatherRotation.z + 0.16 + featherLag * 0.22
    parts.tail.rotation.x = base.tailRotation.x - 0.32
    parts.leftFoot.rotation.x = 0.65
    parts.rightFoot.rotation.x = 0.65
  }
  if (snapshot.key === 'climbing') {
    parrot.root.rotation.z = -0.18
    parts.head.rotation.x = -0.18
    parts.leftFoot.rotation.x = Math.sin(t * 5) * 0.3
    parts.rightFoot.rotation.x = Math.sin(t * 5 + Math.PI) * 0.3
  }
  if (snapshot.key === 'sleeping') {
    parts.head.rotation.y = 0.88
    parts.head.rotation.z = -0.92
    parts.head.rotation.x = 0.28
    parrot.root.scale.copy(base.rootScale).multiplyScalar(1.05)
    parts.leftFoot.visible = false
    parts.leftWing.rotation.x = base.leftWingRotation.x - 0.08
    parts.rightWing.rotation.x = base.rightWingRotation.x - 0.08
    parts.eyes.forEach((eye, index) => {
      eye.scale.copy(base.eyeScales[index])
      eye.scale.y *= 0.06
    })
  }
  if (snapshot.key === 'playing') {
    parts.head.rotation.x = 0.2 + Math.max(0, Math.sin(t * 5.5)) * 0.38
    parts.head.rotation.y = Math.sin(t * 2.3) * 0.36
    parts.leftWing.rotation.z = base.leftWingRotation.z - Math.max(0, Math.sin(t * 7)) * 0.16
    parts.rightWing.rotation.z = base.rightWingRotation.z + Math.max(0, Math.sin(t * 7)) * 0.16
  }
  if (snapshot.key === 'hop') {
    parts.leftWing.rotation.z = base.leftWingRotation.z - 0.18
    parts.rightWing.rotation.z = base.rightWingRotation.z + 0.18
  }
  if (snapshot.key === 'defecating') {
    const strain = Math.sin(progress * Math.PI)
    parrot.root.rotation.x = -0.09 * strain
    parrot.root.scale.set(
      base.rootScale.x * 1.035,
      base.rootScale.y * (0.98 - strain * 0.055),
      base.rootScale.z * 1.035,
    )
    parts.body.scale.x *= 1.04
    parts.body.scale.y *= 0.94
    parts.tail.rotation.x = base.tailRotation.x + 0.42 * strain
    parts.head.rotation.x = -0.12 * strain
    if (progress > 0.38 && progress < 0.92) {
      const fall = (progress - 0.38) / 0.54
      parts.dropping.visible = true
      parts.dropping.position.y = base.droppingPosition.y - fall * 1.35
      parts.dropping.rotation.z = fall * 1.8
    }
  }
  if (snapshot.key === 'calling') {
    const callPulse = Math.max(0, Math.sin(t * 10.5))
    parts.lowerBeak.position.y = base.lowerBeakPosition.y - callPulse * 0.085
    parts.head.rotation.x = -0.08 + callPulse * 0.13
    parts.head.rotation.z = Math.sin(t * 5.25) * 0.035
    parts.body.scale.x *= 1 + callPulse * 0.022
    parts.body.scale.z *= 1 + callPulse * 0.028
  }
}

function resizeRenderer() {
  if (!renderer || !camera || !canvas.value || !viewport.value) return
  const rect = viewport.value.getBoundingClientRect()
  const width = Math.max(1, Math.round(rect.width))
  const height = Math.max(1, Math.round(rect.height))
  const requestedRatio = Math.min(1.5, window.devicePixelRatio || 1)
  const maxPixels = 1_500_000
  const requestedPixels = width * height * requestedRatio * requestedRatio
  const ratio = requestedPixels > maxPixels
    ? Math.sqrt(maxPixels / (width * height))
    : requestedRatio
  renderer.setPixelRatio(Math.max(1, ratio))
  renderer.setSize(width, height, false)
  if (camera.isOrthographicCamera) {
    const halfHeight = 1.7
    const halfWidth = halfHeight * (width / height)
    camera.left = -halfWidth
    camera.right = halfWidth
    camera.top = halfHeight
    camera.bottom = -halfHeight
  } else {
    camera.aspect = width / height
  }
  camera.updateProjectionMatrix()
  emit('resize', { width: canvas.value.width, height: canvas.value.height })
}

function renderFrame() {
  if (renderer && scene && camera) renderer.render(scene, camera)
}

function animate() {
  if (!running || disposed) return
  if (PARROT_MODEL_DEBUG) {
    renderFrame()
    return
  }
  parrotSimulationRuntime.sync()
  const snapshot = parrotSimulationRuntime.snapshot()
  elapsed = snapshot.elapsed
  updateMovement()
  updatePose(snapshot)
  cage.update(elapsed)
  renderFrame()
}

function setRunning(value) {
  if (!renderer || disposed) return
  running = Boolean(value) && !document.hidden
  renderer.setAnimationLoop(running ? animate : null)
  if (!running) renderFrame()
}

function triggerInteraction(action) {
  if (PARROT_MODEL_DEBUG) return false
  if (!cage || !parrotSimulationRuntime.request(action)) return false
  const snapshot = parrotSimulationRuntime.snapshot()
  elapsed = snapshot.elapsed
  cage.restore(snapshot.environment)
  cage.trigger(action, elapsed)
  return true
}

function feed() {
  return triggerInteraction('feed')
}

function refillWater() {
  return triggerInteraction('water')
}

function play() {
  return triggerInteraction('play')
}

function getCanvas() {
  return canvas.value
}

function captureJpeg({ quality = 0.6, maxWidth = 480 } = {}) {
  if (!canvas.value || !renderer) return ''
  renderFrame()
  const source = canvas.value
  const scale = Math.min(1, maxWidth / source.width)
  const target = document.createElement('canvas')
  target.width = Math.max(1, Math.round(source.width * scale))
  target.height = Math.max(1, Math.round(source.height * scale))
  const context = target.getContext('2d')
  context.drawImage(source, 0, 0, target.width, target.height)
  return target.toDataURL('image/jpeg', quality)
}

function pickInteraction(event) {
  if (PARROT_MODEL_DEBUG) return ''
  if (!canvas.value || !camera || !cage || !props.active) return ''
  const rect = canvas.value.getBoundingClientRect()
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const meshes = cage.pickables.flatMap((group) => {
    const list = []
    group.traverse((object) => { if (object.isMesh) list.push(object) })
    return list
  })
  return raycaster.intersectObjects(meshes, false)[0]?.object?.userData?.interaction || ''
}

function setDebugView(view) {
  if (!PARROT_MODEL_DEBUG || !camera || !parrot) return
  debugView.value = view
  const positions = {
    front: [0, 1.5, 8],
    left: [-8, 1.5, 0],
    back: [0, 1.5, -8],
  }
  camera.position.set(...(positions[view] || positions.front))
  camera.lookAt(0, 1.5, 0)
  camera.updateProjectionMatrix()
  renderFrame()
}

function toggleDebugBox() {
  if (!debugBoxHelper) return
  debugBoxVisible.value = !debugBoxVisible.value
  debugBoxHelper.visible = debugBoxVisible.value
  debugBoxHelper.update()
  renderFrame()
}

function onPointerDown(event) {
  pointerDown = { x: event.clientX, y: event.clientY }
}

function onPointerUp(event) {
  if (!pointerDown) return
  const distance = Math.hypot(event.clientX - pointerDown.x, event.clientY - pointerDown.y)
  pointerDown = null
  if (distance > 8) return
  const action = pickInteraction(event)
  if (action === 'feed') feed()
  if (action === 'water') refillWater()
  if (action === 'play') play()
}

function onPointerMove(event) {
  if (canvas.value) canvas.value.style.cursor = pickInteraction(event) ? 'pointer' : 'default'
}

function handleVisibilityChange() {
  setRunning(props.active)
}

function handleReducedMotion(event) {
  reducedMotion = event.matches
}

function handleContextLost(event) {
  event.preventDefault()
  failure.value = FALLBACK_COPY[props.locale] || FALLBACK_COPY.zh
  setRunning(false)
  emit('error', new Error('WebGL context lost'))
}

async function initialize() {
  try {
    scene = new THREE.Scene()
    if (PARROT_MODEL_DEBUG) {
      camera = new THREE.OrthographicCamera(-1.7, 1.7, 1.7, -1.7, 0.1, 100)
      camera.position.set(0, 1.5, 8)
      camera.lookAt(0, 1.5, 0)
    } else {
      camera = new THREE.PerspectiveCamera(38, 16 / 9, 0.1, 40)
      camera.position.set(6.2, 4.4, 12.4)
      camera.lookAt(0, 2.65, 0)
    }

    renderer = new THREE.WebGLRenderer({
      canvas: canvas.value,
      antialias: true,
      alpha: false,
      preserveDrawingBuffer: true,
      powerPreference: 'high-performance',
    })
    renderer.outputColorSpace = THREE.SRGBColorSpace
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.VSMShadowMap

    addRoom()
    cage = createParrotCageScene(scene)
    const model = createParrotModel()
    parrot = { ...model, base: rememberBaseTransforms(model.parts, model.root) }
    if (PARROT_MODEL_DEBUG) {
      cage.root.visible = false
      parrot.root.position.set(0, -(Number(parrot.root.userData.perchContactY) || 0), 0)
      parrot.root.rotation.set(0, 0, 0)
      const groundGeometry = new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(-1.7, 0, 0),
        new THREE.Vector3(1.7, 0, 0),
      ])
      scene.add(new THREE.Line(
        groundGeometry,
        new THREE.LineBasicMaterial({ color: '#7d4f35' }),
      ))
      debugBoxHelper = new THREE.BoxHelper(parrot.root, '#7d4f35')
      debugBoxHelper.visible = false
      scene.add(debugBoxHelper)
    } else {
      parrot.root.position.copy(placementForAnchor('main'))
    }
    scene.add(parrot.root)

    parrotSimulationRuntime.sync()
    const restoredSnapshot = parrotSimulationRuntime.snapshot()
    elapsed = restoredSnapshot.elapsed
    cage.restore(restoredSnapshot.environment)
    cage.update(elapsed)
    if (!PARROT_MODEL_DEBUG) {
      unsubscribeSimulation = parrotSimulationRuntime.subscribe(handleBehaviorChange)
    }
    resizeObserver = new ResizeObserver(resizeRenderer)
    resizeObserver.observe(viewport.value)
    resizeRenderer()

    reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    reducedMotion = reducedMotionQuery.matches
    reducedMotionQuery.addEventListener?.('change', handleReducedMotion)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    canvas.value.addEventListener('webglcontextlost', handleContextLost)
    canvas.value.addEventListener('pointerdown', onPointerDown)
    canvas.value.addEventListener('pointerup', onPointerUp)
    canvas.value.addEventListener('pointermove', onPointerMove)
    canvas.value.addEventListener('pointerleave', () => { pointerDown = null })

    loading.value = false
    if (PARROT_MODEL_DEBUG) {
      updatePose({ ...restoredSnapshot, key: 'idle', progress: 0 })
      setDebugView('front')
    } else {
      handleBehaviorChange(restoredSnapshot, { restore: true })
    }
    setRunning(PARROT_MODEL_DEBUG ? false : props.active)
    await nextTick()
    emit('ready', { canvas: canvas.value })
  } catch (error) {
    loading.value = false
    failure.value = FALLBACK_COPY[props.locale] || FALLBACK_COPY.zh
    emit('error', error)
  }
}

function disposeMaterial(item) {
  const materials = Array.isArray(item.material) ? item.material : [item.material]
  materials.filter(Boolean).forEach((itemMaterial) => {
    Object.values(itemMaterial).forEach((value) => {
      if (value?.isTexture) value.dispose()
    })
    itemMaterial.dispose?.()
  })
}

function cleanup() {
  disposed = true
  running = false
  unsubscribeSimulation?.()
  unsubscribeSimulation = null
  resizeObserver?.disconnect()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  reducedMotionQuery?.removeEventListener?.('change', handleReducedMotion)
  if (canvas.value) {
    canvas.value.removeEventListener('webglcontextlost', handleContextLost)
    canvas.value.removeEventListener('pointerdown', onPointerDown)
    canvas.value.removeEventListener('pointerup', onPointerUp)
    canvas.value.removeEventListener('pointermove', onPointerMove)
  }
  renderer?.setAnimationLoop(null)
  scene?.traverse((object) => {
    object.geometry?.dispose?.()
    if (object.material) disposeMaterial(object)
  })
  renderer?.dispose()
  renderer?.forceContextLoss()
  renderer = null
  scene = null
  camera = null
}

watch(() => props.active, (active) => setRunning(PARROT_MODEL_DEBUG ? false : active))
watch(() => props.locale, () => {
  if (failure.value) failure.value = FALLBACK_COPY[props.locale] || FALLBACK_COPY.zh
})

onMounted(initialize)
onBeforeUnmount(cleanup)

defineExpose({ feed, refillWater, play, captureJpeg, getCanvas })
</script>

<template>
  <div ref="viewport" class="parrot-cage-3d">
    <canvas ref="canvas" aria-label="3D 小太阳鹦鹉互动鸟笼"></canvas>
    <div v-if="PARROT_MODEL_DEBUG" class="parrot-debug-controls" aria-label="鹦鹉模型三视图">
      <button type="button" :class="{ active: debugView === 'front' }" @click="setDebugView('front')">正视图</button>
      <button type="button" :class="{ active: debugView === 'left' }" @click="setDebugView('left')">左视图</button>
      <button type="button" :class="{ active: debugView === 'back' }" @click="setDebugView('back')">后视图</button>
      <button type="button" :class="{ active: debugBoxVisible }" @click="toggleDebugBox">包围盒</button>
    </div>
    <div v-if="loading" class="parrot-3d-loading" role="status">
      <span></span>
      {{ locale === 'zh' ? '正在搭建 3D 鸟笼…' : 'Loading 3D cage…' }}
    </div>
    <div v-if="failure" class="parrot-3d-fallback" role="status">
      <ParrotVisual type="main-orange" />
      <p>{{ failure }}</p>
    </div>
  </div>
</template>
