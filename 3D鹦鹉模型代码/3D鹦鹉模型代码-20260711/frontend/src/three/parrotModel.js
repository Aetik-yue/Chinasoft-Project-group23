import * as THREE from 'three'

const CM = 0.1
const IS_DEV = Boolean(import.meta.env?.DEV)

export const PARROT_TARGET = Object.freeze({
  totalHeight: 30 * CM,
  totalLength: 24 * CM,
  maxFrontWidth: 14 * CM,
  headWidth: 10.5 * CM,
  headHeight: 10.1 * CM,
  headDepth: 10.8 * CM,
  bodyHeight: 17.5 * CM,
  bodyWidth: 11.6 * CM,
  bodyDepth: 12 * CM,
  wingLength: 14.5 * CM,
  wingVisibleWidthFront: 3 * CM,
  wingVisibleWidthSide: 5.2 * CM,
  tailLength: 9.2 * CM,
  tailWidth: 3.6 * CM,
  beakProjection: 4 * CM,
  beakWidth: 3.8 * CM,
  beakHeight: 5 * CM,
  eyeDiameter: 2.2 * CM,
  legHeight: 4 * CM,
  footLength: 4.6 * CM,
})

export const PARROT_MODEL_TUNING = {
  unitPerCm: CM,
  colors: {
    body: '#E7D2AE',
    head: '#E9B04A',
    face: '#F2CF63',
    wing: '#6E9B62',
    tail: '#5F8A4C',
    tailDark: '#557B43',
    beak: '#D9904A',
    beakDark: '#C77838',
    foot: '#D0B08A',
    eyeWhite: '#F4F0DD',
    eyeBrown: '#2A2119',
    eyeBlack: '#0B0907',
    dropping: '#E8E1C4',
    droppingCenter: '#5C6740',
  },
  dimensions: {
    bodyBottomY: 0.38,
    bodyHeight: 1.75,
    bodyDepthScale: 1,
    bodyLeanRadians: 0.14,
    neckPivot: [0, 1.99, 0.1],
    headCenterOffset: [0, 0.48, 0.11],
    headRadii: [0.5, 0.515, 0.525],
    eyeDiameter: 0.25,
    eyeCenterX: 0.365,
    eyeCenterY: 0.065,
    eyeSurfaceOffset: 0.025,
    wingPivot: [0.45, 1.9, 0.12],
    wingMeshOffset: [0, -0.6, -0.1],
    wingScale: [0.22, 1.33, 0.8],
    wingRotationX: 0.3,
    wingRotationZ: 0.065,
    tailPivot: [0, 0.92, -0.52],
    tailMeshOffset: [0, -0.47, 0],
    tailScale: [0.38, 1.08, 0.3],
    tailRotationX: 0.72,
    footOffsetX: 0.23,
    footJointY: 0.6,
  },
}

function standard(color, options = {}) {
  return new THREE.MeshStandardMaterial({
    color,
    roughness: 0.76,
    metalness: 0,
    envMapIntensity: 0.55,
    ...options,
  })
}

function addMesh(parent, geometry, material, {
  position = [0, 0, 0], scale = [1, 1, 1], rotation = [0, 0, 0], name = '',
} = {}) {
  const mesh = new THREE.Mesh(geometry, material)
  mesh.position.set(...position)
  mesh.scale.set(...scale)
  mesh.rotation.set(...rotation)
  mesh.name = name
  mesh.castShadow = true
  mesh.receiveShadow = false
  parent.add(mesh)
  return mesh
}

function cylinderBetween(parent, start, end, radius, material) {
  const a = new THREE.Vector3(...start)
  const b = new THREE.Vector3(...end)
  const midpoint = a.clone().add(b).multiplyScalar(0.5)
  const direction = b.clone().sub(a)
  const mesh = addMesh(
    parent,
    new THREE.CylinderGeometry(radius, radius * 0.9, direction.length(), 16),
    material,
  )
  mesh.position.copy(midpoint)
  mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.normalize())
  return mesh
}

function createReferenceBodyGeometry(dimensions) {
  const controls = [
    new THREE.Vector3(0, 0, 0),
    new THREE.Vector3(0.1, 0.04, 0),
    new THREE.Vector3(0.25, 0.11, 0),
    new THREE.Vector3(0.42, 0.25, 0),
    new THREE.Vector3(0.54, 0.48, 0),
    new THREE.Vector3(0.58, 0.72, 0),
    new THREE.Vector3(0.56, 1.02, 0),
    new THREE.Vector3(0.5, 1.32, 0),
    new THREE.Vector3(0.47, 1.52, 0),
    new THREE.Vector3(0.44, 1.75, 0),
  ]
  const curve = new THREE.CatmullRomCurve3(controls, false, 'catmullrom', 0.5)
  const profile = curve.getPoints(48).map((point) => (
    new THREE.Vector2(Math.max(0, point.x), point.y)
  ))
  const geometry = new THREE.LatheGeometry(profile, 48)
  geometry.scale(1, 1, dimensions.bodyDepthScale)
  const positions = geometry.attributes.position
  for (let index = 0; index < positions.count; index += 1) {
    const y = positions.getY(index)
    const z = positions.getZ(index)
    const height = THREE.MathUtils.clamp(y / dimensions.bodyHeight, 0, 1)
    const chestCurve = Math.sin(height * Math.PI) * 0.115
    const axisLean = Math.tan(dimensions.bodyLeanRadians) * y
    const shoulderTaper = THREE.MathUtils.smoothstep(height, 0.72, 1)
    const frontDepth = THREE.MathUtils.lerp(1.13, 0.96, shoulderTaper)
    positions.setZ(index, z * (z >= 0 ? frontDepth : 0.97) + chestCurve + axisLean)
  }
  positions.needsUpdate = true
  geometry.computeVertexNormals()
  return geometry
}

function createRoundedTeardropGeometry(radius = 0.54, tipWidth = 0.24) {
  const geometry = new THREE.SphereGeometry(radius, 32, 24)
  const positions = geometry.attributes.position
  for (let index = 0; index < positions.count; index += 1) {
    const y = positions.getY(index)
    const height = THREE.MathUtils.clamp((y / radius + 1) * 0.5, 0, 1)
    const width = height < 0.42
      ? THREE.MathUtils.lerp(tipWidth, 1, height / 0.42)
      : THREE.MathUtils.lerp(1, 0.74, (height - 0.42) / 0.58)
    positions.setX(index, positions.getX(index) * width)
    positions.setZ(index, positions.getZ(index) * width)
  }
  positions.needsUpdate = true
  geometry.computeVertexNormals()
  return geometry
}

function createNeckBridgeGeometry() {
  const controls = [
    new THREE.Vector3(0.43, 0, 0),
    new THREE.Vector3(0.48, 0.1, 0),
    new THREE.Vector3(0.46, 0.22, 0),
    new THREE.Vector3(0.35, 0.34, 0),
    new THREE.Vector3(0.16, 0.42, 0),
    new THREE.Vector3(0, 0.46, 0),
  ]
  const curve = new THREE.CatmullRomCurve3(controls, false, 'centripetal', 0.5)
  const profile = curve.getPoints(40).map((point) => (
    new THREE.Vector2(Math.max(0, point.x), point.y)
  ))
  const geometry = new THREE.LatheGeometry(profile, 48)
  const positions = geometry.attributes.position
  for (let index = 0; index < positions.count; index += 1) {
    const y = positions.getY(index)
    const z = positions.getZ(index)
    positions.setZ(index, z * (z >= 0 ? 1.02 : 0.96) + y * 0.055)
  }
  positions.needsUpdate = true
  geometry.computeVertexNormals()
  return geometry
}

function createTailConnectorGeometry() {
  const controls = [
    new THREE.Vector3(0, -0.29, 0),
    new THREE.Vector3(0.12, -0.24, 0),
    new THREE.Vector3(0.17, -0.1, 0),
    new THREE.Vector3(0.18, 0.08, 0),
    new THREE.Vector3(0.15, 0.22, 0),
    new THREE.Vector3(0, 0.3, 0),
  ]
  const curve = new THREE.CatmullRomCurve3(controls, false, 'centripetal', 0.5)
  const profile = curve.getPoints(36).map((point) => (
    new THREE.Vector2(Math.max(0, point.x), point.y)
  ))
  const geometry = new THREE.LatheGeometry(profile, 40)
  geometry.scale(1, 1, 0.6)
  geometry.computeVertexNormals()
  return geometry
}

function createHeadGeometry(colors) {
  const controls = [
    new THREE.Vector3(0, -1, 0),
    new THREE.Vector3(0.78, -0.9, 0),
    new THREE.Vector3(0.93, -0.68, 0),
    new THREE.Vector3(1.02, -0.32, 0),
    new THREE.Vector3(1, 0.02, 0),
    new THREE.Vector3(0.92, 0.38, 0),
    new THREE.Vector3(0.75, 0.68, 0),
    new THREE.Vector3(0.48, 0.9, 0),
    new THREE.Vector3(0, 1, 0),
  ]
  const curve = new THREE.CatmullRomCurve3(controls, false, 'centripetal', 0.5)
  const profile = curve.getPoints(52).map((point) => (
    new THREE.Vector2(Math.max(0, point.x), point.y)
  ))
  const geometry = new THREE.LatheGeometry(profile, 64)
  const positions = geometry.attributes.position
  const vertexColors = []
  const crownColor = new THREE.Color(colors.head)
  const faceColor = new THREE.Color(colors.face)
  for (let index = 0; index < positions.count; index += 1) {
    const y = positions.getY(index)
    const originalZ = positions.getZ(index)
    const centerShift = 0.022 * Math.max(0, 1 - y * y)
    const z = originalZ * (originalZ >= 0 ? 1.025 : 0.985) + centerShift
    positions.setZ(index, z)
    const boundary = -0.05 + z * 0.2
    const raw = THREE.MathUtils.clamp((boundary + 0.085 - y) / 0.17, 0, 1)
    const blend = raw * raw * (3 - 2 * raw)
    const color = crownColor.clone().lerp(faceColor, blend)
    vertexColors.push(color.r, color.g, color.b)
  }
  positions.needsUpdate = true
  geometry.setAttribute('color', new THREE.Float32BufferAttribute(vertexColors, 3))
  geometry.computeVertexNormals()
  return geometry
}

function createCurvedBeakGeometry() {
  const curve = new THREE.CubicBezierCurve3(
    new THREE.Vector3(0, 0.04, 0.38),
    new THREE.Vector3(0, 0.09, 0.6),
    new THREE.Vector3(0, 0.01, 0.82),
    new THREE.Vector3(0, -0.29, 0.69),
  )
  const lengthSegments = 30
  const radialSegments = 24
  const vertices = []
  const indices = []
  const xAxis = new THREE.Vector3(1, 0, 0)
  // Build regular rings up to the final section; the end itself is one shared point.
  for (let segment = 0; segment < lengthSegments; segment += 1) {
    const t = segment / lengthSegments
    const center = curve.getPoint(t)
    const tangent = curve.getTangent(t).normalize()
    const verticalAxis = new THREE.Vector3(0, -tangent.z, tangent.y).normalize()
    // The final regular ring stays visible from the front; topology supplies the sharp tip.
    const width = 0.02 + 0.17 * Math.pow(1 - t, 0.58)
    const height = 0.018 + 0.152 * Math.pow(1 - t, 0.6)
    for (let radial = 0; radial < radialSegments; radial += 1) {
      const angle = (radial / radialSegments) * Math.PI * 2
      const point = center.clone()
        .addScaledVector(xAxis, Math.cos(angle) * width)
        .addScaledVector(verticalAxis, Math.sin(angle) * height)
      vertices.push(point.x, point.y, point.z)
    }
  }
  for (let segment = 0; segment < lengthSegments - 1; segment += 1) {
    for (let radial = 0; radial < radialSegments; radial += 1) {
      const nextRadial = (radial + 1) % radialSegments
      const a = segment * radialSegments + radial
      const b = segment * radialSegments + nextRadial
      const c = (segment + 1) * radialSegments + nextRadial
      const d = (segment + 1) * radialSegments + radial
      indices.push(a, b, d, b, c, d)
    }
  }
  const tipIndex = vertices.length / 3
  const tipPoint = curve.getPoint(1)
  vertices.push(tipPoint.x, tipPoint.y, tipPoint.z)
  const lastRing = (lengthSegments - 1) * radialSegments
  for (let radial = 0; radial < radialSegments; radial += 1) {
    const nextRadial = (radial + 1) % radialSegments
    indices.push(lastRing + radial, lastRing + nextRadial, tipIndex)
  }
  const startCenter = vertices.length / 3
  const startPoint = curve.getPoint(0)
  vertices.push(startPoint.x, startPoint.y, startPoint.z)
  for (let radial = 0; radial < radialSegments; radial += 1) {
    const nextRadial = (radial + 1) % radialSegments
    indices.push(startCenter, nextRadial, radial)
  }
  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3))
  geometry.setIndex(indices)
  geometry.userData.tipIndex = tipIndex
  geometry.userData.pointedTip = true
  geometry.computeVertexNormals()
  return geometry
}

function createEyeTexture(colors) {
  if (typeof document === 'undefined') {
    const data = new Uint8Array([
      244, 240, 221, 255,
      42, 33, 25, 255,
      11, 9, 7, 255,
      255, 255, 255, 255,
    ])
    const texture = new THREE.DataTexture(data, 2, 2, THREE.RGBAFormat)
    texture.colorSpace = THREE.SRGBColorSpace
    texture.needsUpdate = true
    return texture
  }
  const canvas = document.createElement('canvas')
  canvas.width = 256
  canvas.height = 256
  const context = canvas.getContext('2d')
  context.clearRect(0, 0, 256, 256)
  context.fillStyle = colors.eyeWhite
  context.beginPath()
  context.arc(128, 128, 108, 0, Math.PI * 2)
  context.fill()
  context.fillStyle = colors.eyeBrown
  context.beginPath()
  context.ellipse(128, 130, 72, 82, 0, 0, Math.PI * 2)
  context.fill()
  context.fillStyle = colors.eyeBlack
  context.beginPath()
  context.ellipse(128, 132, 68, 78, 0, 0, Math.PI * 2)
  context.fill()
  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return texture
}

function placeFlatDiscOnEllipsoid(mesh, { x, y, rx, ry, rz, offset = 0.012 }) {
  const inside = Math.max(0, 1 - (x * x) / (rx * rx) - (y * y) / (ry * ry))
  const z = rz * Math.sqrt(inside)
  const normal = new THREE.Vector3(
    x / (rx * rx),
    y / (ry * ry),
    z / (rz * rz),
  ).normalize()
  mesh.position.set(x, y, z).addScaledVector(normal, offset)
  mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 0, 1), normal)
}

function createWing(side, materials, dimensions) {
  const socket = new THREE.Group()
  socket.name = side < 0 ? 'left-wing-socket' : 'right-wing-socket'
  socket.position.set(
    side * dimensions.wingPivot[0],
    dimensions.wingPivot[1],
    dimensions.wingPivot[2],
  )
  const shoulder = addMesh(socket, new THREE.SphereGeometry(0.5, 28, 22), materials.wing, {
    position: [0, -0.25, -0.03],
    scale: [0.24, 0.62, 0.52],
    rotation: [0.18, 0, side * -0.02],
    name: side < 0 ? 'left-wing-shoulder' : 'right-wing-shoulder',
  })

  const wing = new THREE.Group()
  wing.name = side < 0 ? 'left-wing' : 'right-wing'
  wing.rotation.x = dimensions.wingRotationX
  wing.rotation.z = side * -dimensions.wingRotationZ
  socket.add(wing)

  const feather = new THREE.Group()
  feather.name = side < 0 ? 'left-wing-feather-joint' : 'right-wing-feather-joint'
  feather.position.x = side * 0.1
  feather.position.y = -0.15
  wing.add(feather)
  addMesh(feather, createRoundedTeardropGeometry(0.54, 0.28), materials.wing, {
    position: [
      dimensions.wingMeshOffset[0],
      dimensions.wingMeshOffset[1] + 0.15,
      dimensions.wingMeshOffset[2],
    ],
    scale: dimensions.wingScale,
    rotation: [0, 0, side * -0.035],
    name: side < 0 ? 'left-wing-mesh' : 'right-wing-mesh',
  })
  return { socket, wing, feather, shoulder }
}

function createFoot(side, materials, dimensions) {
  const foot = new THREE.Group()
  foot.name = side < 0 ? 'left-foot' : 'right-foot'
  // The group pivot sits inside the belly so leg animations cannot tear the joint apart.
  foot.position.set(side * dimensions.footOffsetX, dimensions.footJointY, 0.02)
  cylinderBetween(foot, [0, 0, 0.025], [0, -0.5, 0], 0.065, materials.foot)
  addMesh(foot, new THREE.SphereGeometry(0.5, 20, 16), materials.foot, {
    position: [0, -0.518, 0.04],
    scale: [0.25, 0.14, 0.23],
    name: 'foot-palm',
  })
  const toeGeometry = new THREE.SphereGeometry(0.5, 20, 16)
  const toes = [
    { position: [-0.07, -0.562, 0.16], scale: [0.095, 0.076, 0.23], yaw: -0.16 },
    { position: [0.07, -0.562, 0.16], scale: [0.095, 0.076, 0.23], yaw: 0.16 },
    { position: [-0.065, -0.566, -0.09], scale: [0.09, 0.068, 0.17], yaw: 0.14 },
    { position: [0.065, -0.566, -0.09], scale: [0.09, 0.068, 0.17], yaw: -0.14 },
  ]
  toes.forEach((toe, index) => {
    addMesh(foot, toeGeometry, materials.foot, {
      position: toe.position,
      scale: toe.scale,
      rotation: [0, toe.yaw, 0],
      name: `rounded-toe-${index}`,
    })
  })
  return foot
}

function updatePerchContact(root, leftFoot, rightFoot) {
  root.updateMatrixWorld(true)
  const feetBox = new THREE.Box3().makeEmpty()
  feetBox.expandByObject(leftFoot)
  feetBox.expandByObject(rightFoot)
  root.userData.perchContactY = feetBox.min.y
}

export function sizeOf(object) {
  object.updateWorldMatrix(true, true)
  return new THREE.Box3().setFromObject(object).getSize(new THREE.Vector3())
}

export function createParrotModel() {
  const { colors, dimensions } = PARROT_MODEL_TUNING
  const [headRx, headRy, headRz] = dimensions.headRadii
  const root = new THREE.Group()
  root.name = 'rounded-reference-parrot'
  root.rotation.set(0, 0, 0)
  root.scale.set(1, 1, 1)

  const materials = {
    body: standard(colors.body),
    head: standard('#FFFFFF', { vertexColors: true }),
    wing: standard(colors.wing),
    tail: standard(colors.tail),
    beak: standard(colors.beak, { side: THREE.DoubleSide }),
    beakDark: standard(colors.beakDark),
    foot: standard(colors.foot),
    dropping: standard(colors.dropping),
    droppingCenter: standard(colors.droppingCenter),
  }

  const torso = new THREE.Group()
  torso.name = 'body-and-neck'
  root.add(torso)
  const bodyCore = addMesh(torso, createReferenceBodyGeometry(dimensions), materials.body, {
    position: [0, dimensions.bodyBottomY, 0],
    name: 'body',
  })
  const neckBlend = addMesh(torso, createNeckBridgeGeometry(), materials.body, {
    position: [0, 1.72, 0.18],
    scale: [0.98, 1, 1.05],
    name: 'neck-body-blend',
  })
  const throatBlend = addMesh(torso, new THREE.SphereGeometry(0.5, 32, 24), materials.body, {
    position: [0, 2, 0.38],
    scale: [0.56, 0.28, 0.58],
    name: 'throat-body-blend',
  })
  const rumpBlend = addMesh(torso, new THREE.SphereGeometry(0.5, 32, 24), materials.body, {
    position: [0, 0.82, -0.34],
    scale: [0.54, 0.5, 0.52],
    name: 'body-tail-blend',
  })

  const head = new THREE.Group()
  head.name = 'neck-pivot'
  head.position.set(...dimensions.neckPivot)
  root.add(head)
  const napeBlend = addMesh(head, new THREE.SphereGeometry(0.5, 32, 24), materials.body, {
    position: [0, 0.22, -0.04],
    scale: [0.82, 0.44, 0.68],
    name: 'moving-nape-blend',
  })
  const headVisual = new THREE.Group()
  headVisual.name = 'head-visual'
  headVisual.position.set(...dimensions.headCenterOffset)
  head.add(headVisual)
  addMesh(headVisual, createHeadGeometry(colors), materials.head, {
    scale: dimensions.headRadii,
    name: 'yellow-head',
  })

  const eyeTexture = createEyeTexture(colors)
  const eyeMaterial = new THREE.MeshBasicMaterial({
    map: eyeTexture,
    transparent: true,
    alphaTest: 0.08,
    depthWrite: false,
    depthTest: true,
    toneMapped: false,
    side: THREE.DoubleSide,
    polygonOffset: true,
    polygonOffsetFactor: -2,
    polygonOffsetUnits: -2,
  })
  const eyes = []
  ;[-1, 1].forEach((side) => {
    const eye = addMesh(
      headVisual,
      new THREE.CircleGeometry(dimensions.eyeDiameter / 2, 48),
      eyeMaterial,
      { name: side < 0 ? 'left-eye-decal' : 'right-eye-decal' },
    )
    placeFlatDiscOnEllipsoid(eye, {
      x: side * dimensions.eyeCenterX,
      y: dimensions.eyeCenterY,
      rx: headRx,
      ry: headRy,
      rz: headRz,
      offset: dimensions.eyeSurfaceOffset,
    })
    eye.renderOrder = 10
    eyes.push(eye)
  })
  const eyeRings = eyes

  const upperBeak = new THREE.Group()
  upperBeak.name = 'upper-beak'
  headVisual.add(upperBeak)
  addMesh(upperBeak, createCurvedBeakGeometry(), materials.beak, {
    name: 'curved-upper-beak',
  })
  const lowerBeak = addMesh(headVisual, new THREE.SphereGeometry(0.5, 28, 20), materials.beakDark, {
    position: [0, -0.17, 0.5],
    scale: [0.14, 0.08, 0.12],
    name: 'lower-beak',
  })

  const leftWingRig = createWing(-1, materials, dimensions)
  const rightWingRig = createWing(1, materials, dimensions)
  const leftWing = leftWingRig.wing
  const rightWing = rightWingRig.wing
  root.add(leftWingRig.socket, rightWingRig.socket)

  const tail = new THREE.Group()
  tail.name = 'tail-joint'
  tail.position.set(...dimensions.tailPivot)
  tail.rotation.x = dimensions.tailRotationX
  root.add(tail)
  const tailBase = addMesh(tail, createTailConnectorGeometry(), materials.tail, {
    position: [0, -0.08, 0],
    name: 'tail-root-blend',
  })
  addMesh(tail, createRoundedTeardropGeometry(0.5), materials.tail, {
    position: dimensions.tailMeshOffset,
    scale: dimensions.tailScale,
    name: 'main-tail-feather',
  })

  const leftFoot = createFoot(-1, materials, dimensions)
  const rightFoot = createFoot(1, materials, dimensions)
  root.add(leftFoot, rightFoot)

  const dropping = new THREE.Group()
  dropping.name = 'temporary-dropping'
  dropping.position.set(0, 0.28, -0.43)
  addMesh(dropping, new THREE.SphereGeometry(0.055, 10, 8), materials.dropping, {
    scale: [0.8, 1.25, 0.8],
  })
  addMesh(dropping, new THREE.SphereGeometry(0.03, 8, 6), materials.droppingCenter, {
    position: [0, -0.015, 0.025],
    scale: [0.75, 1.35, 0.75],
  })
  dropping.visible = false
  root.add(dropping)

  updatePerchContact(root, leftFoot, rightFoot)
  const measurements = {
    wholeModel: sizeOf(root),
    body: sizeOf(bodyCore),
    head: sizeOf(headVisual),
    leftWing: sizeOf(leftWingRig.socket),
    rightWing: sizeOf(rightWingRig.socket),
    tail: sizeOf(tail),
    leftFoot: sizeOf(leftFoot),
    rightFoot: sizeOf(rightFoot),
  }
  if (IS_DEV) {
    console.table(Object.fromEntries(
      Object.entries(measurements).map(([key, value]) => [key, value.toArray().map((n) => Number(n.toFixed(3)))]),
    ))
    console.assert(
      Math.abs(measurements.wholeModel.y - PARROT_TARGET.totalHeight) <= 0.05,
      '模型总高度不符合三视图目标',
    )
    console.assert(
      measurements.wholeModel.x >= 1.35 && measurements.wholeModel.x <= 1.45,
      '模型正面最大宽度不符合目标',
    )
  }

  return {
    root,
    measurements,
    parts: {
      body: torso,
      bodyCore,
      neckBlend,
      throatBlend,
      rumpBlend,
      head,
      headVisual,
      napeBlend,
      leftWing,
      rightWing,
      leftWingSocket: leftWingRig.socket,
      rightWingSocket: rightWingRig.socket,
      leftWingFeather: leftWingRig.feather,
      rightWingFeather: rightWingRig.feather,
      leftWingShoulder: leftWingRig.shoulder,
      rightWingShoulder: rightWingRig.shoulder,
      tail,
      tailBase,
      leftFoot,
      rightFoot,
      upperBeak,
      lowerBeak,
      eyes,
      eyeRings,
      dropping,
    },
  }
}
