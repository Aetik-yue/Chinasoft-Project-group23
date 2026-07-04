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
  <article class="current-card" :data-route="parrot.route">
    <div class="avatar-bubble">
      <ParrotVisual :type="parrot.avatarType" />
    </div>
    <div class="current-text">
      <p>当前鹦鹉</p>
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
