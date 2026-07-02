<template>
  <section class="mock-view settings-view">
    <section class="settings-section">
      <h3>基础设置</h3>
      <label>烟雾单位<select v-model="form.unit"><option>ppm</option></select></label>
      <label>数据刷新频率<select v-model="form.refresh"><option>3秒</option><option>5秒</option><option>1分钟</option></select></label>
      <label>默认时间范围<select v-model="form.range"><option>5分钟</option><option>半小时</option><option>1小时</option><option>半天</option><option>1天</option><option>近7天</option></select></label>
    </section>

    <section class="settings-section">
      <h3>阈值设置</h3>
      <label>正常上限<input v-model.number="form.normalMax" min="1" type="number" @input="saved = false" /></label>
      <label>低风险上限<input v-model.number="form.lowMax" min="1" type="number" @input="saved = false" /></label>
      <label>高风险上限<input v-model.number="form.highMax" min="1" type="number" @input="saved = false" /></label>
    </section>

    <section class="settings-section">
      <h3>主题设置</h3>
      <label class="setting-toggle"><span>根据风险自动切换背景</span><button class="theme-switch-toggle" :class="{ on: form.autoTheme }" type="button" :aria-pressed="form.autoTheme" aria-label="根据风险自动切换背景" @click="toggleTheme"><span></span></button></label>
    </section>

    <div class="settings-actions">
      <button class="mock-button" :disabled="saved" type="button" @click="saveSettings">{{ saved ? "保存成功" : "保存设置" }}</button>
      <button class="mock-button" type="button" @click="restoreDefault">恢复默认</button>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref, watch } from "vue";

const saved = ref(false);
const defaults = { unit: "ppm", refresh: "3秒", range: "1小时", normalMax: 100, lowMax: 200, highMax: 400, autoTheme: true };
const form = reactive({ ...defaults });

watch(form, () => { saved.value = false; }, { deep: true });

function validateThresholds() {
  return form.normalMax > 0 && form.lowMax > 0 && form.highMax > 0 && form.lowMax >= form.normalMax && form.highMax >= form.lowMax;
}

function saveSettings() {
  if (!validateThresholds()) {
    window.alert("请按规范填写");
    return;
  }
  saved.value = true;
}

function restoreDefault() {
  Object.assign(form, defaults);
  saved.value = false;
}

function toggleTheme() {
  form.autoTheme = !form.autoTheme;
}
</script>