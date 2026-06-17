package com.devpilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态变更结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStatusResultVO {

    /** 笔记 ID */
    private Long id;

    /** 变更后的状态 */
    private String status;

    /** 变更前的状态 */
    private String previousStatus;

    /** 更新时间 (yyyy-MM-dd HH:mm:ss) */
    @JsonProperty("updated_at")
    private String updatedAt;
}
