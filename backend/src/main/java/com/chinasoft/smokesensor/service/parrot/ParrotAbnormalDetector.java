package com.chinasoft.smokesensor.service.parrot;

import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotAbnormalEvent;
import com.chinasoft.smokesensor.dto.ParrotBox;
import java.util.List;

/**
 * 鹦鹉异常行为规则引擎（有状态，按会话/设备维护）。
 *
 * <p>每帧把实时检测结果喂给 {@link #update}，引擎根据时序与框运动判断四类异常：
 * <ul>
 *   <li>MISSING：连续无检测超过 missingSeconds → 失踪/逃逸</li>
 *   <li>STATIC：检测到但框质心位移持续低于阈值超过 staticSeconds → 可能受伤/昏迷</li>
 *   <li>PLUCKING：行为持续为"梳理羽毛"超过 pluckingSeconds → 疑似拔羽</li>
 *   <li>COUNT：检测数 != expectedCount → 数量异常</li>
 * </ul>
 */
public class ParrotAbnormalDetector {

    private final ParrotProperties.Abnormal cfg;

    private long noDetectSince = -1L;
    private long staticSince = -1L;
    private long pluckSince = -1L;
    private Double lastCx;
    private Double lastCy;
    private boolean lastCountWarned;

    public ParrotAbnormalDetector(ParrotProperties.Abnormal cfg) {
        this.cfg = cfg;
    }

    /**
     * 喂入一帧结果，返回当前应提示的异常事件（无异常返回 null）。
     *
     * @param detected 本帧是否检测到鹦鹉
     * @param boxes    本帧鹦鹉框（detected 为 false 时可为空）
     * @param behavior 本帧行为标签（中文，可能 null）
     * @param now     当前时间（epoch 毫秒）
     */
    public ParrotAbnormalEvent update(boolean detected, List<ParrotBox> boxes,
                                    String behavior, long now) {
        if (!detected || boxes == null || boxes.isEmpty()) {
            if (noDetectSince < 0) {
                noDetectSince = now;
            }
            if (now - noDetectSince > (long) cfg.getMissingSeconds() * 1000L) {
                return event(ParrotAbnormalEvent.Type.MISSING,
                        ParrotAbnormalEvent.Severity.DANGER,
                        "连续 " + cfg.getMissingSeconds() + "s 未检测到鹦鹉，可能失踪/逃逸");
            }
            return null;
        }

        // 本帧检测到鹦鹉
        noDetectSince = -1L;

        // 数量异常
        if (boxes.size() != cfg.getExpectedCount()) {
            lastCountWarned = true;
            return event(ParrotAbnormalEvent.Type.COUNT,
                    ParrotAbnormalEvent.Severity.WARNING,
                    "鹦鹉数量异常：期望 " + cfg.getExpectedCount() + " 只，实际 " + boxes.size() + " 只");
        }
        lastCountWarned = false;

        // 主框（面积最大）质心
        ParrotBox main = mainBox(boxes);
        double cx = main.getX() + main.getWidth() / 2.0;
        double cy = main.getY() + main.getHeight() / 2.0;

        // 静止判断
        if (lastCx != null) {
            double move = Math.hypot(cx - lastCx, cy - lastCy);
            if (move < cfg.getStaticMoveThreshold()) {
                if (staticSince < 0) {
                    staticSince = now;
                }
                if (now - staticSince > (long) cfg.getStaticSeconds() * 1000L) {
                    return event(ParrotAbnormalEvent.Type.STATIC,
                            ParrotAbnormalEvent.Severity.WARNING,
                            "鹦鹉长时间几乎不动（>" + cfg.getStaticSeconds() + "s），可能受伤/昏迷");
                }
            } else {
                staticSince = -1L;
            }
        }
        lastCx = cx;
        lastCy = cy;

        // 拔羽判断（持续"梳理羽毛"）
        if (behavior != null && behavior.contains("梳理羽毛")) {
            if (pluckSince < 0) {
                pluckSince = now;
            }
            if (now - pluckSince > (long) cfg.getPluckingSeconds() * 1000L) {
                return event(ParrotAbnormalEvent.Type.PLUCKING,
                        ParrotAbnormalEvent.Severity.WARNING,
                        "持续梳理羽毛（>" + cfg.getPluckingSeconds() + "s），疑似拔羽行为");
            }
        } else {
            pluckSince = -1L;
        }

        return null;
    }

    private ParrotBox mainBox(List<ParrotBox> boxes) {
        ParrotBox best = boxes.get(0);
        int bestArea = best.getWidth() * best.getHeight();
        for (int i = 1; i < boxes.size(); i++) {
            ParrotBox b = boxes.get(i);
            int area = b.getWidth() * b.getHeight();
            if (area > bestArea) {
                best = b;
                bestArea = area;
            }
        }
        return best;
    }

    private ParrotAbnormalEvent event(ParrotAbnormalEvent.Type type,
                                        ParrotAbnormalEvent.Severity severity, String message) {
        return new ParrotAbnormalEvent(type, severity, message, System.currentTimeMillis());
    }
}
