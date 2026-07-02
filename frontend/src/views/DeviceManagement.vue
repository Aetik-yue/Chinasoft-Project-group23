<template>
  <section class="mock-view device-management-view">
    <div class="mode-switcher">
      <button
        v-for="mode in modes"
        :key="mode.value"
        class="mock-button"
        :class="{ active: activeMode === mode.value }"
        type="button"
        @click="activeMode = mode.value"
      >
        {{ mode.label }}
      </button>
    </div>

    <template v-if="activeMode === 'pager'">
      <div class="pager-layout">
        <button class="add-device" type="button" @click="showAddHint = true">+</button>
        <button class="arrow-button arrow-left" type="button" aria-label="上一个设备" @click="prevDevice">‹</button>

        <article class="pager-device-card preview" :class="{ offline: previousDevice.status === '离线' }">
          <h3>{{ previousDevice.id }} {{ previousDevice.name }}</h3>
          <p>安装位置：{{ previousDevice.location }}</p>
          <p>在线状态：{{ previousDevice.status }}</p>
          <p>当前浓度：{{ previousDevice.smoke }} ppm</p>
          <p>电量 / 信号：{{ previousDevice.battery }} / {{ previousDevice.signal }}</p>
          <p>绑定用户：{{ previousDevice.user }}</p>
          <p>最后上报：{{ previousDevice.lastReport }}</p>
          <div class="card-actions preview-actions">
            <button class="mock-button" type="button" disabled>预览</button>
          </div>
        </article>

        <article class="pager-device-card current" :class="{ offline: currentDevice.status === '离线' }">
          <h3>{{ currentDevice.id }} {{ currentDevice.name }}</h3>
          <p>安装位置：{{ currentDevice.location }}</p>
          <p>在线状态：{{ currentDevice.status }}</p>
          <p>当前浓度：{{ currentDevice.smoke }} ppm</p>
          <p>电量 / 信号：{{ currentDevice.battery }} / {{ currentDevice.signal }}</p>
          <p>绑定用户：{{ currentDevice.user }}</p>
          <p>最后上报：{{ currentDevice.lastReport }}</p>
          <div class="card-actions">
            <button class="mock-button" type="button">编辑</button>
            <button class="mock-button danger" type="button" @click="confirmDelete(currentDevice)">删除</button>
          </div>
        </article>

        <article class="pager-device-card preview" :class="{ offline: nextDevice.status === '离线' }">
          <h3>{{ nextDevice.id }} {{ nextDevice.name }}</h3>
          <p>安装位置：{{ nextDevice.location }}</p>
          <p>在线状态：{{ nextDevice.status }}</p>
          <p>当前浓度：{{ nextDevice.smoke }} ppm</p>
          <p>电量 / 信号：{{ nextDevice.battery }} / {{ nextDevice.signal }}</p>
          <p>绑定用户：{{ nextDevice.user }}</p>
          <p>最后上报：{{ nextDevice.lastReport }}</p>
          <div class="card-actions preview-actions">
            <button class="mock-button" type="button" disabled>预览</button>
          </div>
        </article>

        <button class="arrow-button arrow-right" type="button" aria-label="下一个设备" @click="nextDeviceItem">›</button>
      </div>
    </template>

    <template v-else-if="activeMode === 'overview'">
      <div class="mock-toolbar">
        <input v-model="deviceKeyword" class="mock-input wide" type="search" placeholder="搜索设备名称、编号或位置" />
      </div>

      <div class="device-overview-layout">
        <aside class="device-filter-panel">
          <button class="mock-button filter-reset" type="button" @click="resetOverviewFilters">重置</button>
          <div v-for="filter in filterGroups" :key="filter.key" class="device-filter-item">
            <button
              class="mock-button filter-trigger"
              :class="{ active: openFilter === filter.key }"
              type="button"
              @click="toggleFilter(filter.key)"
            >
              <span>{{ filter.label }}</span>
              <em>{{ overviewFilters[filter.key] }}</em>
            </button>
            <div v-if="openFilter === filter.key" class="filter-options">
              <button
                v-for="option in filter.options"
                :key="option"
                class="filter-option"
                :class="{ active: overviewFilters[filter.key] === option }"
                type="button"
                @click="selectFilter(filter.key, option)"
              >
                {{ option }}
              </button>
            </div>
          </div>
        </aside>

        <div class="device-card-grid overview-card-grid">
          <article
            v-for="device in overviewFilteredDevices"
            :key="device.id"
            class="device-small-card"
            :class="{ offline: device.status === '离线' }"
            :title="device.location + ' / ' + device.user + ' / ' + device.lastReport"
          >
            <h3>{{ device.id }}</h3>
            <p>{{ device.name }}</p>
            <p>{{ device.status }}</p>
            <p>{{ device.smoke }} ppm</p>
            <p>{{ device.battery }} / {{ device.signal }}</p>
            <button class="mock-link-button" type="button" @click="confirmDelete(device)">删除</button>
          </article>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="mock-toolbar">
        <input v-model="mapKeyword" class="mock-input wide" type="search" placeholder="搜索地图设备名称、编号或位置" />
        <button class="mock-button" :class="{ active: mapStatus === '全部' }" type="button" @click="setMapStatus('全部')">全部</button>
        <button class="mock-button" :class="{ active: mapStatus === '在线' }" type="button" @click="setMapStatus('在线')">在线</button>
        <button class="mock-button" :class="{ active: mapStatus === '离线' }" type="button" @click="setMapStatus('离线')">离线</button>
        <button class="mock-button" type="button" @click="showAddHint = true">新增设备</button>
      </div>
      <div class="map-placeholder" @dblclick="mapFeedback = '已标记双击位置'">
        <span class="map-feedback">{{ mapFeedback || '地图占位区域' }}</span>
        <article
          v-for="device in mapFilteredDevices"
          :key="device.id"
          class="map-device-card"
          :class="{ offline: device.status === '离线' }"
          :style="{ left: device.x + '%', top: device.y + '%' }"
        >
          <strong>{{ shortName(device.name) }}</strong>
          <span>{{ device.status }}</span>
        </article>
      </div>
    </template>

    <div v-if="deleteTarget" class="mock-modal-mask" @click.self="deleteTarget = null">
      <div class="mock-modal">
        <h3>确认删除</h3>
        <p>是否删除 {{ deleteTarget.name }}？</p>
        <button class="mock-button danger" type="button" @click="deleteDevice">确认删除</button>
        <button class="mock-button" type="button" @click="deleteTarget = null">取消</button>
      </div>
    </div>

    <div v-if="showAddHint" class="mock-modal-mask" @click.self="showAddHint = false">
      <div class="mock-modal">
        <h3>新增设备</h3>
        <p>新增设备表单占位。</p>
        <button class="mock-button" type="button" @click="showAddHint = false">关闭</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";

const modes = [
  { value: "pager", label: "翻页模式" },
  { value: "overview", label: "总览模式" },
  { value: "map", label: "地图模式" }
];
const activeMode = ref("pager");
const currentIndex = ref(0);
const deviceKeyword = ref("");
const mapKeyword = ref("");
const mapStatus = ref("全部");
const deleteTarget = ref(null);
const showAddHint = ref(false);
const mapFeedback = ref("");
const openFilter = ref("");

const devices = ref([
  { id: "SMK-001", name: "一号楼烟感", location: "一号楼大厅", status: "在线", smoke: 68, battery: "91%", signal: "强", user: "张三", lastReport: "2026-07-02 09:30", operation: "可编辑", x: 18, y: 28 },
  { id: "SMK-008", name: "库房烟感", location: "库房 A 区", status: "离线", smoke: 0, battery: "42%", signal: "弱", user: "李四", lastReport: "2026-07-01 22:10", operation: "可删除", x: 42, y: 48 },
  { id: "SMK-011", name: "实验室烟感", location: "实验楼 1F", status: "在线", smoke: 126, battery: "76%", signal: "中", user: "王五", lastReport: "2026-07-02 09:28", operation: "可编辑", x: 66, y: 34 },
  { id: "SMK-023", name: "三号楼烟感", location: "三号楼 2F", status: "在线", smoke: 286, battery: "88%", signal: "强", user: "赵六", lastReport: "2026-07-02 09:31", operation: "可删除", x: 72, y: 68 },
  { id: "SMK-030", name: "地下车库烟感", location: "地下车库", status: "离线", smoke: 0, battery: "33%", signal: "弱", user: "钱七", lastReport: "2026-07-01 20:12", operation: "可删除", x: 28, y: 72 }
]);

const overviewDefaults = {
  status: "全部",
  smokeRange: "全部",
  signal: "全部",
  reportTime: "全部",
  operation: "全部"
};

const overviewFilters = reactive({ ...overviewDefaults });

const filterGroups = [
  { key: "status", label: "在线状态", options: ["全部", "在线", "离线"] },
  { key: "smokeRange", label: "当前浓度范围", options: ["全部", "0-100", "100-200", "200-400", "400以上"] },
  { key: "signal", label: "电量 / 信号", options: ["全部", "电量高", "电量低", "信号强", "信号弱"] },
  { key: "reportTime", label: "最后上报时间", options: ["全部", "近一小时", "近六小时", "近一天", "近七天"] },
  { key: "operation", label: "操作", options: ["全部", "可删除", "可编辑"] }
];

const latestReportTime = computed(() => getLatestDate(devices.value.map((device) => device.lastReport)));
const overviewFilteredDevices = computed(() => {
  const query = deviceKeyword.value.trim();
  return devices.value.filter((device) => {
    const keywordMatched = matchDeviceKeyword(device, query);
    const statusMatched = overviewFilters.status === "全部" || device.status === overviewFilters.status;
    const smokeMatched = overviewFilters.smokeRange === "全部" || matchSmokeRange(device.smoke, overviewFilters.smokeRange);
    const signalMatched = overviewFilters.signal === "全部" || matchSignal(device, overviewFilters.signal);
    const reportMatched = overviewFilters.reportTime === "全部" || matchRecentTime(device.lastReport, overviewFilters.reportTime, latestReportTime.value);
    const operationMatched = overviewFilters.operation === "全部" || device.operation === overviewFilters.operation;
    return keywordMatched && statusMatched && smokeMatched && signalMatched && reportMatched && operationMatched;
  });
});
const mapFilteredDevices = computed(() => {
  const query = mapKeyword.value.trim();
  return devices.value.filter((device) => {
    const keywordMatched = matchDeviceKeyword(device, query);
    const statusMatched = mapStatus.value === "全部" || device.status === mapStatus.value;
    return keywordMatched && statusMatched;
  });
});
const currentDevice = computed(() => devices.value[currentIndex.value]);
const previousDevice = computed(() => devices.value[(currentIndex.value + devices.value.length - 1) % devices.value.length]);
const nextDevice = computed(() => devices.value[(currentIndex.value + 1) % devices.value.length]);

function prevDevice() {
  currentIndex.value = (currentIndex.value + devices.value.length - 1) % devices.value.length;
}

function nextDeviceItem() {
  currentIndex.value = (currentIndex.value + 1) % devices.value.length;
}

function confirmDelete(device) {
  deleteTarget.value = device;
}

function deleteDevice() {
  if (!deleteTarget.value) return;
  const targetId = deleteTarget.value.id;
  devices.value = devices.value.filter((device) => device.id !== targetId);
  deleteTarget.value = null;
  if (!devices.value.length) {
    currentIndex.value = 0;
    return;
  }
  if (currentIndex.value >= devices.value.length) {
    currentIndex.value = devices.value.length - 1;
  }
}

function toggleFilter(key) {
  openFilter.value = openFilter.value === key ? "" : key;
}

function selectFilter(key, option) {
  overviewFilters[key] = option;
  openFilter.value = "";
}

function resetOverviewFilters() {
  Object.assign(overviewFilters, overviewDefaults);
  deviceKeyword.value = "";
  openFilter.value = "";
}

function setMapStatus(status) {
  mapStatus.value = status;
  if (status === "全部") mapKeyword.value = "";
}

function matchDeviceKeyword(device, query) {
  if (!query) return true;
  return `${device.id}${device.name}${device.location}${device.status}${device.user}`.includes(query);
}

function matchSmokeRange(smoke, range) {
  if (range === "0-100") return smoke >= 0 && smoke <= 100;
  if (range === "100-200") return smoke > 100 && smoke <= 200;
  if (range === "200-400") return smoke > 200 && smoke <= 400;
  if (range === "400以上") return smoke > 400;
  return true;
}

function matchSignal(device, option) {
  const batteryValue = Number.parseInt(device.battery, 10);
  if (option === "电量高") return batteryValue >= 70;
  if (option === "电量低") return batteryValue < 50;
  if (option === "信号强") return device.signal === "强";
  if (option === "信号弱") return device.signal === "弱";
  return true;
}

function matchRecentTime(timeText, range, baseDate) {
  const date = parseDate(timeText);
  if (!date || !baseDate) return true;
  const hours = {
    近一小时: 1,
    近六小时: 6,
    近一天: 24,
    近七天: 24 * 7
  }[range];
  if (!hours) return true;
  const diff = baseDate.getTime() - date.getTime();
  return diff >= 0 && diff <= hours * 60 * 60 * 1000;
}

function getLatestDate(values) {
  const dates = values.map(parseDate).filter(Boolean);
  if (!dates.length) return null;
  return new Date(Math.max(...dates.map((date) => date.getTime())));
}

function parseDate(value) {
  const date = new Date(value.replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function shortName(name) {
  return name.length > 5 ? `${name.slice(0, 5)}...` : name;
}
</script>