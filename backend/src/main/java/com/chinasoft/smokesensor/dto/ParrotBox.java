package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单只鹦鹉检测框（实时模式用），坐标为图像像素。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParrotBox {

    /** 框左上角 x（像素） */
    private int x;

    /** 框左上角 y（像素） */
    private int y;

    /** 框宽（像素） */
    private int width;

    /** 框高（像素） */
    private int height;

    /** 检测置信度 0~1 */
    private double confidence;

    /** 类别标签，通常为 bird / parrot */
    private String label;
}
