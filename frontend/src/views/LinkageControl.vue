<template>
  <section class="mock-view linkage-view">
    <div class="linkage-status" :class="alarmActive ? 'alarm' : 'safe'">
      当前状态：{{ alarmActive ? "告警中" : "安全" }}
      <button class="mock-button" type="button" @click="alarmActive = !alarmActive">切换状态</button>
    </div>

    <div class="hardware-grid linkage-hardware-grid">
      <article v-for="item in hardware" :key="item.key" class="hardware-card" :class="item.on ? 'on' : 'off'">
        <h3>{{ item.name }}</h3>
        <p>当前状态：{{ item.on ? "开启" : "关闭" }}</p>
        <button class="mock-button" type="button" @click="item.on = !item.on">{{ item.on ? "关闭" : "开启" }}</button>
      </article>
    </div>

    <section class="linkage-settings">
      <h3>联动设置</h3>
      <label v-for="setting in settings" :key="setting.key" class="setting-toggle">
        <span>高风险时自动开启：{{ setting.name }}</span>
        <button class="mock-button" type="button" @click="setting.enabled = !setting.enabled">
          {{ setting.enabled ? "已开启" : "已关闭" }}
        </button>
      </label>
    </section>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";

const alarmActive = ref(false);
const hardware = reactive([
  { key: "buzzer", name: "蜂鸣器", on: false },
  { key: "light", name: "报警灯", on: false }
]);
const settings = reactive([
  { key: "buzzer", name: "蜂鸣器", enabled: true },
  { key: "light", name: "报警灯", enabled: true },
  { key: "broadcast", name: "广播", enabled: false }
]);
</script>