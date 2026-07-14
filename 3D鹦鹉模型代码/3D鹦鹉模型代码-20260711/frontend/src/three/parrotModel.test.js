import test from 'node:test'
import assert from 'node:assert/strict'
import { createParrotModel, PARROT_TARGET, sizeOf } from './parrotModel.js'

const closeTo = (actual, expected, tolerance) => {
  assert.ok(Math.abs(actual - expected) <= tolerance, `${actual} is not within ${tolerance} of ${expected}`)
}

test('model uses direct target dimensions without root scaling', () => {
  const model = createParrotModel()
  assert.deepEqual(model.root.scale.toArray(), [1, 1, 1])
  closeTo(model.measurements.wholeModel.y, PARROT_TARGET.totalHeight, 0.05)
  assert.ok(model.measurements.wholeModel.x >= 1.35)
  assert.ok(model.measurements.wholeModel.x <= 1.45)
  closeTo(model.measurements.body.x, PARROT_TARGET.bodyWidth, 0.03)
  closeTo(model.measurements.body.y, PARROT_TARGET.bodyHeight, 0.05)
})

test('feet define perch contact and tail does not define placement', () => {
  const model = createParrotModel()
  closeTo(model.root.userData.perchContactY, 0, 0.005)
  assert.ok(model.parts.tail.position.y > model.root.userData.perchContactY)
})

test('model keeps flat eyes and contains no extruded beak geometry', () => {
  const model = createParrotModel()
  model.parts.eyes.forEach((eye) => assert.equal(eye.geometry.type, 'CircleGeometry'))
  const geometryTypes = []
  model.root.traverse((object) => {
    if (object.geometry?.type) geometryTypes.push(object.geometry.type)
  })
  assert.equal(geometryTypes.includes('ExtrudeGeometry'), false)
  assert.equal(model.parts.head.name, 'neck-pivot')
})

test('custom head and transition meshes keep the silhouette continuous', () => {
  const model = createParrotModel()
  assert.equal(model.parts.headVisual.getObjectByName('yellow-head').geometry.type, 'LatheGeometry')
  assert.equal(model.parts.neckBlend.parent, model.parts.body)
  assert.equal(model.parts.napeBlend.parent, model.parts.head)
  assert.equal(model.parts.rumpBlend.parent, model.parts.body)
  assert.equal(model.parts.tailBase.parent, model.parts.tail)
  closeTo(sizeOf(model.parts.upperBeak).x, PARROT_TARGET.beakWidth, 0.02)
  assert.ok(sizeOf(model.parts.upperBeak).y > 0.48)
  const beakGeometry = model.parts.upperBeak.getObjectByName('curved-upper-beak').geometry
  assert.equal(beakGeometry.userData.pointedTip, true)
  assert.ok(Number.isInteger(beakGeometry.userData.tipIndex))
})

test('feet pivot inside the belly and keep two toes forward and two backward', () => {
  const model = createParrotModel()
  ;[model.parts.leftFoot, model.parts.rightFoot].forEach((foot) => {
    assert.ok(foot.position.y > 0.5)
    const toes = foot.children.filter((child) => child.name.startsWith('rounded-toe-'))
    assert.equal(toes.filter((toe) => toe.position.z > 0).length, 2)
    assert.equal(toes.filter((toe) => toe.position.z < 0).length, 2)
  })
})

test('fixed wing sockets keep the articulated flight feathers attached', () => {
  const model = createParrotModel()
  assert.equal(model.parts.leftWing.parent, model.parts.leftWingSocket)
  assert.equal(model.parts.rightWing.parent, model.parts.rightWingSocket)
  assert.equal(model.parts.leftWingFeather.parent, model.parts.leftWing)
  assert.equal(model.parts.rightWingFeather.parent, model.parts.rightWing)
  assert.equal(model.parts.leftWingShoulder.parent, model.parts.leftWingSocket)
  assert.equal(model.parts.rightWingShoulder.parent, model.parts.rightWingSocket)
})
