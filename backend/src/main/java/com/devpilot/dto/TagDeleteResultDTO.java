package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除标签结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagDeleteResultDTO {

    /**
     * 级联删除的关联记录数
     */
    private int deletedNoteTags;
}
