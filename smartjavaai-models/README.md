# SmartJavaAI 模型文件

本目录存放鹦鹉行为识别（`/api/parrot/behavior`）所需的模型与配套文件。

> ⚠️ **大模型二进制（`*.pt` / `*.onnx`）已通过 `.gitignore` 排除，不会进入版本控制。**
> clone 后需按下方说明自行下载放到本目录，否则相关接口无法启用。

## 文件清单

| 文件 | 大小 | 是否入库 | 用途 |
|---|---|---|---|
| `yolov8n.onnx` | ~13 MB | ❌ 需下载 | YOLOv8n COCO 目标检测模型（ONNX），用于检测画面中的 `bird` |
| `clip.pt` | ~578 MB | ❌ 需下载 | CLIP 模型，用于对检测到的鹦鹉做零样本行为分类 |
| `synset.txt` | 621 B | ✅ 已入库 | YOLO COCO 类别标签（`bird` 等 80 类），运行时与 `yolov8n.onnx` 同目录 |
| `tokenizer.json` | 2.2 MB | ✅ 已入库 | CLIP tokenizer，运行时与 `clip.pt` 同目录 |
| `parrot.png` | 2.2 MB | ✅ 已入库 | 骨架阶段本地占位截图（无真实摄像头时用） |

## 下载方式

### 1. `yolov8n.onnx`（YOLOv8n COCO ONNX）

Ultralytics 官方 YOLOv8n 模型导出的 ONNX 版本。两种获取方式：

- **直接下载 onnx**：从 Ultralytics assets release 获取 `yolov8n.onnx`。
- **从 pt 导出**：`pip install ultralytics` 后执行 `yolo export model=yolov8n.pt format=onnx`，把生成的 `yolov8n.onnx` 放到本目录。

### 2. `clip.pt`（CLIP 模型）

SmartJavaAI 使用的 CLIP 模型权重。从 SmartJavaAI 官方模型库 / 文档获取对应版本的 `clip.pt`，放到本目录。

> 若暂时无法获取模型，可在 `backend/src/main/resources/application.yml` 中将 `parrot.detection.enabled` 与 `parrot.clip.enabled` 设为 `false`，应用仍可正常启动，仅 `/api/parrot/behavior` 不可用。

## 配套文件依赖

根据 SmartJavaAI 运行时要求（见仓库记忆 `smartjavaai-model-files-gotcha`）：

- `yolov8n.onnx` 必须与 `synset.txt` 同目录，否则运行时报错。
- `clip.pt` 必须与 `tokenizer.json` 同目录，否则运行时报错。

本目录已入库 `synset.txt` 和 `tokenizer.json`，下载好两个模型二进制后即可直接运行。
