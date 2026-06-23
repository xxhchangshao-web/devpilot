package com.devpilot.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiSuggestionRequest {
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 2000, message = "问题描述不能超过2000字符")
    private String description;

    /** 问题分类（可选，帮助 AI 聚焦） */
    private String category;
}
