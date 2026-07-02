<template>
  <section class="device-disconnected">
    <h2>&#35774;&#22791;&#26410;&#36830;&#25509;</h2>
    <p v-if="message">{{ message }}</p>
    <p v-else>&#35831;&#26816;&#26597;&#35774;&#22791;&#20379;&#30005;&#12289;&#32593;&#32476;&#25110;&#21518;&#31471;&#36830;&#25509;&#29366;&#24577;</p>
    <button type="button" @click="$emit('reconnect')">&#37325;&#26032;&#36830;&#25509;</button>
  </section>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: ""
  }
});

defineEmits(["reconnect"]);
</script>