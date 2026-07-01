package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {

    private Boolean systemOnline;

    private LocalDateTime currentTime;

    private Long onlineDeviceCount;

    private Long totalDeviceCount;
}
