package com.devpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求参数
 */
@Data
public class TagCreateDTO {

    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 30, message = "标签名称长度必须在1到30个字符之间")
    private String name;
}
