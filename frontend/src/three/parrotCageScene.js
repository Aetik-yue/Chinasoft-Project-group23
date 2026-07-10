import * as THREE from 'three'

function material(color, options = {}) {
  return new THREE.MeshStandardMaterial({ color, roughness: 0.72, metalness: 0.04, ...options })
}

function mesh(parent, geometry, meshMaterial, position = [0, 0, 0], options = {}) {
  const item = new THREE.Mesh(geometry, meshMaterial)
  item.position.set(...position)
  if (options.rotation) item.rotation.set(...options.rotation)
  if (options.scale) item.scale.set(...options.scale)
  item.castShadow = options.castShadow !== false
  item.receiveShadow = options.receiveShadow !== false
  parent.add(item)
  return item
}

function cylinderBetween(parent, start, end, radius, meshMaterial, radialSegments = 12) {
  const a = new THREE.Vector3(...start)
  const b = new THREE.Vector3(...end)
  const direction = b.clone().sub(a)
  const item = mesh(parent, new THREE.CylinderGeometry(radius, radius, direction.length(), radialSegments), meshMaterial)
  item.position.copy(a.clone().add(b).multiplyScalar(0.5))
  item.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.normalize())
  return item
}

function markInteraction(group, action) {
  group.name = `${action}-interaction`
  group.traverse((object) => {
    if (object.isMesh) object.userData.interaction = action
  })
}

function createCageBars(root, cageMaterial, frontMaterial) {
  const verticalGeometry = new THREE.CylinderGeometry(0.025, 0.025, 5.2, 6)
  const xValues = []
  for (let x = -3.35; x <= 3.35; x += 0.48) xValues.push(x)
  const vertical = new THREE.InstancedMesh(verticalGeometry, cageMaterial, xValues.length)
  const transform = new THREE.Object3D()
  xValues.forEach((x, index) => {
    transform.position.set(x, 3.05, -2.18)
    transform.updateMatrix()
    vertical.setMatrixAt(index, transform.matrix)
  })
  vertical.castShadow = true
  root.add(vertical)

  const frontValues = xValues.filter((_, index) => index % 2 === 0)
  const front = new THREE.InstancedMesh(verticalGeometry, frontMaterial, frontValues.length)
  frontValues.forEach((x, index) => {
    transform.position.set(x, 3.05, 2.24)
    transform.updateMatrix()
    front.setMatrixAt(index, transform.matrix)
  })
  front.renderOrder = 5
  root.add(front)

  const horizontalGeometry = new THREE.CylinderGeometry(0.022, 0.022, 6.72, 6)
  const heights = [0.66, 1.34, 2.02, 2.7, 3.38, 4.06, 4.74, 5.42]
  const horizontal = new THREE.InstancedMesh(horizontalGeometry, cageMaterial, heights.length)
  heights.forEach((y, index) => {
    transform.position.set(0, y, -2.18)
    transform.rotation.z = Math.PI / 2
    transform.updateMatrix()
    horizontal.setMatrixAt(index, transform.matrix)
  })
  root.add(horizontal)

  const sideGeometry = new THREE.CylinderGeometry(0.023, 0.023, 5.2, 6)
  const sideBars = new THREE.InstancedMesh(sideGeometry, cageMaterial, 20)
  let sideIndex = 0
  ;[-3.36, 3.36].forEach((x) => {
    for (let z = -1.95; z <= 1.95; z += 0.48) {
      transform.position.set(x, 3.05, z)
      transform.rotation.set(0, 0, 0)
      transform.updateMatrix()
      sideBars.setMatrixAt(sideIndex, transform.matrix)
      sideIndex += 1
    }
  })
  sideBars.count = sideIndex
  root.add(sideBars)
}

export function createParrotCageScene(scene) {
  const root = new THREE.Group()
  root.name = 'interactive-parrot-cage'
  scene.add(root)

  const materials = {
    cage: material('#736d63', { metalness: 0.72, roughness: 0.3 }),
    frontCage: material('#8d887e', { metalness: 0.64, roughness: 0.34, transparent: true, opacity: 0.2, depthWrite: false }),
    frame: material('#4e4a44', { metalness: 0.76, roughness: 0.26 }),
    tray: material('#e7d7bf'),
    trayEdge: material('#9d7a58'),
    wood: material('#9b6335'),
    woodLight: material('#bd814d'),
    feeder: material('#d9a65e'),
    feederGlass: material('#d8e8dc', { transparent: true, opacity: 0.42, roughness: 0.18, metalness: 0, depthWrite: false }),
    food: material('#c78a35'),
    waterCup: material('#77a7b5', { transparent: true, opacity: 0.52, roughness: 0.2, metalness: 0 }),
    water: material('#58c4dc', { transparent: true, opacity: 0.72, roughness: 0.14, metalness: 0 }),
    rope: material('#c49862'),
    toyRed: material('#d95845'),
    toyYellow: material('#eab94e'),
    toyBlue: material('#4e8fb5'),
  }

  mesh(root, new THREE.BoxGeometry(7.2, 0.22, 4.8), materials.tray, [0, 0.28, 0])
  mesh(root, new THREE.BoxGeometry(7.35, 0.24, 4.95), materials.trayEdge, [0, 0.15, 0])
  mesh(root, new THREE.BoxGeometry(6.95, 0.07, 4.45), material('#d7c5a8'), [0, 0.42, 0])

  const framePoints = [
    [-3.48, 0.42, -2.3], [3.48, 0.42, -2.3], [-3.48, 0.42, 2.3], [3.48, 0.42, 2.3],
    [-3.48, 5.72, -2.3], [3.48, 5.72, -2.3], [-3.48, 5.72, 2.3], [3.48, 5.72, 2.3],
  ]
  ;[0, 1, 2, 3].forEach((index) => cylinderBetween(root, framePoints[index], framePoints[index + 4], 0.07, materials.frame))
  ;[[4, 5], [6, 7], [4, 6], [5, 7], [0, 1], [2, 3]].forEach(([a, b]) => {
    cylinderBetween(root, framePoints[a], framePoints[b], 0.07, materials.frame)
  })
  createCageBars(root, materials.cage, materials.frontCage)

  cylinderBetween(root, [-2.8, 1.78, 0.15], [2.65, 1.78, -0.08], 0.12, materials.wood, 14)
  cylinderBetween(root, [-2.2, 3.35, -0.7], [1.85, 3.55, -0.45], 0.1, materials.woodLight, 12)
  cylinderBetween(root, [0.85, 4.25, -1.22], [2.85, 4.2, -1.05], 0.105, materials.wood, 12)
  cylinderBetween(root, [-2.9, 1.0, -1.05], [-1.7, 2.48, -0.78], 0.095, materials.woodLight, 10)

  const feeder = new THREE.Group()
  feeder.position.set(-2.55, 1.15, 0.72)
  root.add(feeder)
  mesh(feeder, new THREE.CylinderGeometry(0.58, 0.45, 0.42, 20, 1, false, 0, Math.PI), materials.feeder, [0, 0.05, 0.08], { rotation: [Math.PI / 2, 0, Math.PI] })
  mesh(feeder, new THREE.BoxGeometry(0.92, 1.32, 0.65), materials.feederGlass, [0, 0.98, -0.12])
  const gate = mesh(feeder, new THREE.BoxGeometry(0.72, 0.12, 0.14), materials.frame, [0, 0.44, 0.2])
  gate.geometry.translate(0, 0, -0.05)
  const foodSurface = mesh(feeder, new THREE.CylinderGeometry(0.43, 0.43, 0.12, 20), materials.food, [0, 0.18, 0.12], { scale: [1, 0.7, 0.8] })
  markInteraction(feeder, 'feed')

  const pelletGeometry = new THREE.SphereGeometry(0.055, 8, 6)
  const pellets = Array.from({ length: 18 }, (_, index) => {
    const pellet = mesh(feeder, pelletGeometry, index % 3 === 0 ? materials.toyYellow : materials.food, [0, 0.65, 0.2])
    pellet.visible = false
    return pellet
  })

  const waterStation = new THREE.Group()
  waterStation.position.set(2.55, 1.28, 0.68)
  root.add(waterStation)
  mesh(waterStation, new THREE.CylinderGeometry(0.48, 0.42, 0.55, 20, 1, true), materials.waterCup, [0, 0.02, 0], { rotation: [Math.PI / 2, 0, 0] })
  const waterSurface = mesh(waterStation, new THREE.CylinderGeometry(0.39, 0.39, 0.06, 20), materials.water, [0, 0.05, 0.12], { rotation: [Math.PI / 2, 0, 0] })
  markInteraction(waterStation, 'water')

  const toy = new THREE.Group()
  toy.position.set(1.45, 5.55, 0.55)
  root.add(toy)
  cylinderBetween(toy, [0, 0, 0], [0, -1.45, 0], 0.035, materials.rope, 8)
  mesh(toy, new THREE.TorusGeometry(0.38, 0.075, 10, 24), materials.toyBlue, [0, -1.6, 0])
  mesh(toy, new THREE.SphereGeometry(0.18, 14, 10), materials.toyRed, [-0.25, -2.02, 0])
  mesh(toy, new THREE.SphereGeometry(0.18, 14, 10), materials.toyYellow, [0.25, -2.02, 0])
  markInteraction(toy, 'play')

  const ladder = new THREE.Group()
  ladder.position.set(-2.55, 0.8, -0.8)
  root.add(ladder)
  cylinderBetween(ladder, [0, 0, 0], [0.75, 2.0, 0], 0.035, materials.rope, 8)
  cylinderBetween(ladder, [0.44, -0.15, 0], [1.19, 1.85, 0], 0.035, materials.rope, 8)
  for (let i = 0; i < 5; i += 1) {
    const y = i * 0.42
    cylinderBetween(ladder, [0.15 + i * 0.157, y, 0], [0.59 + i * 0.157, y - 0.15, 0], 0.035, materials.woodLight, 8)
  }

  const anchors = {
    main: new THREE.Vector3(0.2, 1.92, 0.2),
    upper: new THREE.Vector3(-0.75, 3.62, -0.28),
    feeder: new THREE.Vector3(-2.05, 1.92, 0.5),
    water: new THREE.Vector3(2.03, 1.94, 0.5),
    sleep: new THREE.Vector3(1.72, 4.38, -0.88),
    climb: new THREE.Vector3(-2.35, 2.55, -0.52),
    toy: new THREE.Vector3(0.92, 3.76, 0.4),
  }

  const effects = { feedAt: -100, waterAt: -100, playAt: -100 }

  function trigger(action, elapsed) {
    if (action === 'feed') effects.feedAt = elapsed
    if (action === 'water') effects.waterAt = elapsed
    if (action === 'play') effects.playAt = elapsed
  }

  function update(elapsed) {
    const feedTime = elapsed - effects.feedAt
    gate.rotation.x = feedTime >= 0 && feedTime < 0.65 ? -Math.sin((feedTime / 0.65) * Math.PI) * 1.05 : 0
    pellets.forEach((pellet, index) => {
      const local = feedTime - 0.12 - index * 0.045
      pellet.visible = local >= 0 && local < 0.75
      if (pellet.visible) {
        const spread = ((index % 5) - 2) * 0.075
        pellet.position.set(spread, 0.78 - local * 0.78, 0.2 + Math.sin(index * 3.2) * 0.08)
      }
    })
    if (feedTime >= 0 && feedTime < 2) foodSurface.scale.y = THREE.MathUtils.lerp(0.7, 1.25, Math.min(1, feedTime / 1.2))

    const waterTime = elapsed - effects.waterAt
    if (waterTime >= 0 && waterTime < 2.2) {
      waterSurface.position.y = THREE.MathUtils.lerp(-0.08, 0.16, Math.min(1, waterTime / 1.2))
      waterSurface.scale.setScalar(1 + Math.sin(waterTime * 12) * 0.025)
    } else {
      waterSurface.scale.setScalar(1)
    }

    const playTime = elapsed - effects.playAt
    const activeSwing = playTime >= 0 && playTime < 8 ? Math.exp(-playTime * 0.12) : 0.15
    toy.rotation.z = Math.sin(elapsed * (activeSwing > 0.2 ? 4.2 : 1.4)) * 0.24 * activeSwing
  }

  return {
    root,
    anchors,
    pickables: [feeder, waterStation, toy],
    trigger,
    update,
  }
}
