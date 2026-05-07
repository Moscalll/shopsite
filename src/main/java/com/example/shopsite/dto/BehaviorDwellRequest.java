package com.example.shopsite.dto;

import lombok.Data;

@Data
public class BehaviorDwellRequest {
    private Long productId;
    /** 页面停留时长（秒），合法范围由服务端校验 */
    private Integer durationSeconds;
}
