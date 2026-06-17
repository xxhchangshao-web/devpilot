package com.devpilot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 删除标签请求参数
 */
@Data
public class TagDeleteDTO {

    @NotNull(message = "标签ID不能为空")
    private Long id;
}
