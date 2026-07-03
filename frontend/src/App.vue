<script setup>
import { ref } from 'vue'
import CurrentBirdCard from './components/CurrentBirdCard.vue'
import EntryCard from './components/EntryCard.vue'
import MonitorCard from './components/MonitorCard.vue'
import { currentParrot, entryCards, primaryCards } from './data/mockDashboard'

const lastOpenedRoute = ref('')

function handleOpen(entry) {
  lastOpenedRoute.value = entry.route
  console.info('[route-entry]', entry.route, entry)
}
</script>

<template>
  <main class="app-shell">
    <section class="dashboard" aria-label="鹦鹉智能照护系统首页">
      <div class="column left-column">
        <EntryCard :card="entryCards.archive" size="archive" @open="handleOpen" />
        <EntryCard :card="entryCards.growth" size="growth" @open="handleOpen" />
      </div>

      <div class="column center-column">
        <CurrentBirdCard :parrot="currentParrot" @open="handleOpen" />
        <MonitorCard :card="primaryCards.monitor" @open="handleOpen" />
        <EntryCard :card="entryCards.ledger" size="ledger" @open="handleOpen" />
      </div>

      <div class="column right-column">
        <EntryCard :card="entryCards.settings" size="settings" @open="handleOpen" />
        <EntryCard :card="entryCards.medical" size="medical" @open="handleOpen" />
        <EntryCard :card="entryCards.handbook" size="handbook" @open="handleOpen" />
      </div>

      <span class="route-probe" aria-hidden="true">{{ lastOpenedRoute }}</span>
    </section>
  </main>
</template>
