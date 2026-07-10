package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 环境历史的一个时间点：温度 / 湿度 / 粉尘（烟雾浓度）。
 *
 * 三个值都可能为 null（该时刻某项无采样），前端据此断开折线。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentHistoryResponse {

    /** 采样时刻（已按升序排列）。 */
    private LocalDateTime time;

    /** 温度（℃），无数据为 null。 */
    private Double temperature;

    /** 湿度（%RH），无数据为 null。 */
    private Double humidity;

    /** 粉尘 / 烟雾浓度（ppm），无数据为 null。 */
    private Integer dust;
}
