package com.devpilot.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionResponse {
    /** AI 生成的排查步骤（Markdown 格式，可直接填入排查过程字段） */
    private String investigation;

    /** 可能性排序的根因列表 */
    private List<String> possibleCauses;
}
