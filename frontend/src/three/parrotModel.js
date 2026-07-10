import * as THREE from 'three'

function standard(color, options = {}) {
  return new THREE.MeshStandardMaterial({
    color,
    roughness: 0.78,
    metalness: 0.02,
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
  mesh.receiveShadow = true
  parent.add(mesh)
  return mesh
}

function cylinderBetween(parent, start, end, radius, material) {
  const a = new THREE.Vector3(...start)
  const b = new THREE.Vector3(...end)
  const midpoint = a.clone().add(b).multiplyScalar(0.5)
  const direction = b.clone().sub(a)
  const mesh = addMesh(parent, new THREE.CylinderGeometry(radius, radius * 0.82, direction.length(), 8), material)
  mesh.position.copy(midpoint)
  mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.normalize())
  return mesh
}

function createWing(side, materials) {
  const wing = new THREE.Group()
  wing.name = side < 0 ? 'left-wing' : 'right-wing'
  wing.position.set(side * 0.47, 1.02, -0.03)
  wing.rotation.z = side * -0.1

  addMesh(wing, new THREE.SphereGeometry(0.54, 20, 14), materials.wing, {
    scale: [0.5, 1.08, 0.26],
    rotation: [0.12, 0, side * -0.16],
  })

  const featherGeometry = new THREE.CapsuleGeometry(0.105, 0.55, 5, 10)
  for (let row = 0; row < 3; row += 1) {
    for (let index = 0; index < 3; index += 1) {
      const blue = row === 2 && index > 0
      addMesh(wing, featherGeometry, blue ? materials.flightBlue : materials.featherGreen, {
        position: [side * (0.08 + row * 0.035), 0.15 - index * 0.28 - row * 0.08, 0.18 + row * 0.015],
        scale: [0.7, 0.75 + row * 0.08, 0.42],
        rotation: [0, 0, side * (0.12 + index * 0.035)],
      })
    }
  }
  return wing
}

function createFoot(side, materials) {
  const foot = new THREE.Group()
  foot.name = side < 0 ? 'left-foot' : 'right-foot'
  foot.position.set(side * 0.24, 0.18, 0.02)
  cylinderBetween(foot, [0, 0.37, 0], [0, 0.05, 0.04], 0.055, materials.foot)

  const starts = [
    [0, 0.06, 0.05], [0, 0.06, 0.02], [0, 0.06, -0.01], [0, 0.06, -0.03],
  ]
  const ends = [
    [0.13, -0.01, 0.34], [-0.11, -0.01, 0.31], [0.12, -0.01, -0.27], [-0.1, -0.01, -0.25],
  ]
  starts.forEach((start, index) => {
    cylinderBetween(foot, start, ends[index], 0.025, materials.foot)
    const clawEnd = new THREE.Vector3(...ends[index]).add(new THREE.Vector3(0, -0.035, index < 2 ? -0.08 : 0.08))
    cylinderBetween(foot, ends[index], clawEnd.toArray(), 0.014, materials.claw)
  })
  return foot
}

export function createParrotModel() {
  const root = new THREE.Group()
  root.name = 'green-cheeked-conure'
  root.rotation.y = -0.25

  const materials = {
    body: standard('#397b43'),
    belly: standard('#8d3e34'),
    chest: standard('#c8bd9f'),
    chestLine: standard('#5e4a3f'),
    crown: standard('#3a342f'),
    cheek: standard('#479051'),
    wing: standard('#2f7a43'),
    featherGreen: standard('#3f9253'),
    flightBlue: standard('#245f86'),
    tail: standard('#913b38'),
    tailDark: standard('#6f2d35'),
    beak: standard('#726c61'),
    beakDark: standard('#4a4741'),
    eyeRing: standard('#eee9d9'),
    eye: standard('#14120f', { roughness: 0.25 }),
    eyeShine: standard('#ffffff', { emissive: '#ffffff', emissiveIntensity: 0.4 }),
    foot: standard('#796b65'),
    claw: standard('#322e2b'),
  }

  const body = addMesh(root, new THREE.SphereGeometry(0.72, 28, 22), materials.body, {
    position: [0, 0.92, 0],
    scale: [0.86, 1.22, 0.78],
    rotation: [0.05, 0, 0],
    name: 'body',
  })
  addMesh(root, new THREE.SphereGeometry(0.59, 24, 18), materials.chest, {
    position: [0, 1.01, 0.41],
    scale: [0.72, 1.03, 0.26],
    name: 'scalloped-chest',
  })
  addMesh(root, new THREE.SphereGeometry(0.46, 20, 16), materials.belly, {
    position: [0, 0.61, 0.47],
    scale: [0.8, 0.62, 0.19],
    name: 'maroon-belly',
  })

  const scaleGeometry = new THREE.TorusGeometry(0.085, 0.015, 5, 12, Math.PI)
  const scaleRows = [
    { y: 1.29, count: 3, spacing: 0.19, z: 0.57 },
    { y: 1.13, count: 4, spacing: 0.18, z: 0.61 },
    { y: 0.97, count: 5, spacing: 0.16, z: 0.62 },
  ]
  scaleRows.forEach(({ y, count, spacing, z }) => {
    for (let i = 0; i < count; i += 1) {
      addMesh(root, scaleGeometry, materials.chestLine, {
        position: [(i - (count - 1) / 2) * spacing, y, z],
        rotation: [Math.PI, 0, 0],
        scale: [0.9, 0.75, 0.8],
      })
    }
  })

  const head = new THREE.Group()
  head.name = 'head-joint'
  head.position.set(0, 1.55, 0.12)
  root.add(head)
  addMesh(head, new THREE.SphereGeometry(0.53, 28, 22), materials.crown, {
    scale: [0.92, 0.9, 0.94],
    name: 'brown-crown',
  })
  addMesh(head, new THREE.SphereGeometry(0.43, 22, 18), materials.cheek, {
    position: [0, -0.05, 0.27],
    scale: [1.02, 0.78, 0.58],
    name: 'green-cheeks',
  })

  const eyeRings = []
  const eyes = []
  ;[-1, 1].forEach((side) => {
    const ring = addMesh(head, new THREE.TorusGeometry(0.105, 0.03, 10, 22), materials.eyeRing, {
      position: [side * 0.405, 0.12, 0.2],
      rotation: [0, side * 1.16, 0],
    })
    const eye = addMesh(head, new THREE.SphereGeometry(0.06, 16, 12), materials.eye, {
      position: [side * 0.445, 0.12, 0.218],
    })
    addMesh(head, new THREE.SphereGeometry(0.018, 8, 6), materials.eyeShine, {
      position: [side * 0.47, 0.145, 0.25],
    })
    eyeRings.push(ring)
    eyes.push(eye)
  })

  const upperBeak = addMesh(head, new THREE.ConeGeometry(0.26, 0.56, 18), materials.beak, {
    position: [0, -0.04, 0.67],
    rotation: [Math.PI / 2, 0, 0],
    scale: [0.92, 1, 0.8],
    name: 'hooked-upper-beak',
  })
  addMesh(head, new THREE.SphereGeometry(0.12, 14, 10), materials.beakDark, {
    position: [0, -0.23, 0.84],
    scale: [0.8, 1.05, 0.64],
    rotation: [-0.45, 0, 0],
    name: 'beak-hook',
  })
  const lowerBeak = addMesh(head, new THREE.SphereGeometry(0.17, 16, 12), materials.beakDark, {
    position: [0, -0.2, 0.56],
    scale: [0.9, 0.5, 0.76],
    name: 'lower-beak',
  })

  const leftWing = createWing(-1, materials)
  const rightWing = createWing(1, materials)
  root.add(leftWing, rightWing)

  const tail = new THREE.Group()
  tail.name = 'tail-joint'
  tail.position.set(0, 0.53, -0.29)
  root.add(tail)
  const tailGeometry = new THREE.CapsuleGeometry(0.105, 1.06, 5, 10)
  ;[-2, -1, 0, 1, 2].forEach((index) => {
    addMesh(tail, tailGeometry, Math.abs(index) === 2 ? materials.tailDark : materials.tail, {
      position: [index * 0.13, -0.58 - Math.abs(index) * 0.04, 0],
      rotation: [0.12, 0, index * -0.045],
      scale: [0.86, 1 + (2 - Math.abs(index)) * 0.08, 0.42],
    })
  })

  const leftFoot = createFoot(-1, materials)
  const rightFoot = createFoot(1, materials)
  root.add(leftFoot, rightFoot)

  root.scale.setScalar(1.12)

  return {
    root,
    parts: {
      body,
      head,
      leftWing,
      rightWing,
      tail,
      leftFoot,
      rightFoot,
      upperBeak,
      lowerBeak,
      eyes,
      eyeRings,
    },
  }
}
