package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PetWeightRequest {
    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal weightGrams;
    /** 省略时由后端写入当前时间；传入历史时间时由业务层校验。 */
    private LocalDateTime measuredAt;
    @Size(max = 32) private String source;
    @Size(max = 500) private String remark;
}
