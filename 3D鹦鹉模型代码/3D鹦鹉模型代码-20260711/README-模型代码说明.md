# 3D 鹦鹉模型代码包

## 核心文件

- `frontend/src/three/parrotModel.js`：鹦鹉几何、材质、三视图尺寸与集中调节参数。
- `frontend/src/three/parrotCageScene.js`：鸟笼、栖木、食盆、水杯、玩具和场景锚点。
- `frontend/src/three/parrotBehaviorMachine.js`：自主行为状态机与生活需求。
- `frontend/src/three/parrotSimulationRuntime.js`：跨组件持续运行的模拟状态。
- `frontend/src/components/ParrotCage3D.vue`：Three.js 渲染、姿态动画、互动、截图和资源释放。

## 页面接入文件

- `frontend/src/components/MonitorCard.vue`：监控卡片、识别状态和互动按钮。
- `frontend/src/components/ParrotVisual.vue`：WebGL 不可用时的降级插画。
- `frontend/src/composables/useParrotVision.js`：视觉识别 WebSocket 桥接。
- `frontend/src/api/`：监控与视觉识别相关接口。
- `frontend/src/styles.css`：3D 场景、识别状态和互动区布局。
- `frontend/package.json`：Vue 3、Three.js、ECharts 及测试命令。

## 当前模型结构

- 身体、头部和颈背均使用自定义旋转轮廓，不再依赖简单球体缩放。
- 上喙为封闭的贝塞尔扫掠曲面，正视可见、侧视下勾且尖端收尖。
- 眼睛为无高光贴片；左右眼位置、尺寸和贴面偏移集中配置。
- 翅膀采用固定肩根、活动翼臂和滞后外翼关节；飞翔时肩根不会离开身体。
- 后颈采用固定颈桥与随头转动的后颈片，低头进食时保持背部连续。
- 尾根由臀部过渡体和锥形连接面组成，避免球形关节。
- 腿的旋转轴位于腹部内部，足趾为鹦鹉正确的两前两后结构。

## 如何自己调整

主要修改 `frontend/src/three/parrotModel.js` 顶部的 `PARROT_MODEL_TUNING`：

- `headRadii`：头部正面宽度、高度和侧面厚度。
- `eyeDiameter / eyeCenterX / eyeCenterY / eyeSurfaceOffset`：眼睛大小、横向位置、高度和贴面距离。
- `bodyLeanRadians`：身体前倾角。
- `wingPivot / wingScale / wingRotationX / wingRotationZ`：翼根位置、翅膀厚度和贴身角度。
- `tailPivot / tailScale / tailRotationX`：尾根、尾长和尾羽下斜角。
- `footOffsetX / footJointY`：腿部间距和腹部连接高度。

头、身体、颈桥、喙和尾根的轮廓控制点位于相应的 `create...Geometry()` 函数中。每次只调整一个部位，保存后分别检查正视、左视和后视，避免只在单一角度看起来正确。

## 三视图调试

开发模式启动前端后，在地址末尾增加：

```text
?parrotDebug=1
```

调试模式会隐藏鸟笼、暂停自主行为并使用正交相机，可切换正视、左视、后视和包围盒。建议每轮保存这三张固定视角截图，再补一张鸟笼实际画面进行遮挡和光照检查。

## 验证

在 `frontend` 目录执行：

```powershell
npm.cmd install
npm.cmd test
npm.cmd run build
npm.cmd run dev
```

模型采用 `1 Three.js unit = 10 cm`，`root.scale` 固定为 `[1, 1, 1]`。当前自动检查覆盖尺寸、喙的正视宽度、头颈尾过渡、翼肩层级、腿根连接和两前两后足趾。
