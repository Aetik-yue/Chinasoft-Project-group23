package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeysRequest {
    private String qwenApiKey;
    private String deepseekApiKey;
}
