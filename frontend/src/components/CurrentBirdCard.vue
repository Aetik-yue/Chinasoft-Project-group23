<script setup>
import ParrotVisual from './ParrotVisual.vue'

defineProps({
  parrot: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open'])
</script>

<template>
  <button
    class="current-card"
    type="button"
    :data-route="parrot.route"
    aria-label="切换当前鹦鹉"
    @click="emit('open', parrot)"
  >
    <div class="avatar-bubble">
      <ParrotVisual :type="parrot.avatarType" />
    </div>
    <div class="current-text">
      <p>当前鹦鹉</p>
      <h1>{{ parrot.name }}</h1>
    </div>
    <span class="drop-button" aria-hidden="true"></span>
  </button>
</template>
