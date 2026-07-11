<script setup>
import ParrotVisual from './ParrotVisual.vue'

defineProps({
  parrot: {
    type: Object,
    required: true,
  },
  avatarSrc: {
    type: String,
    default: '',
  },
  label: {
    type: String,
    default: '当前鹦鹉',
  },
})

const emit = defineEmits(['open'])
</script>

<template>
  <article class="current-card" :data-route="parrot.route">
    <div class="avatar-bubble">
      <img v-if="avatarSrc" class="pet-avatar-photo" :src="avatarSrc" :alt="parrot.name" />
      <ParrotVisual v-else :type="parrot.avatarType" />
    </div>
    <div class="current-text">
      <p>{{ label }}</p>
      <h1>{{ parrot.name }}</h1>
    </div>
    <button
      class="drop-button"
      type="button"
      aria-label="展开宠物切换面板"
      @click="emit('open', parrot)"
    ></button>
  </article>
</template>
