<script setup>
defineProps({
  card: {
    type: Object,
    required: true,
  },
  size: {
    type: String,
    default: 'medium',
  },
})

const emit = defineEmits(['open'])
</script>

<template>
  <button
    class="entry-card feature-card"
    :class="[`theme-${card.theme}`, `size-${size}`, `card-${card.key}`]"
    type="button"
    :aria-label="`打开${card.title}`"
    :data-route="card.route"
    @click="emit('open', card)"
  >
    <span v-if="card.badge" class="notice-badge">{{ card.badge }}</span>
    <h2 class="card-title">{{ card.title }}</h2>
  </button>
</template>
