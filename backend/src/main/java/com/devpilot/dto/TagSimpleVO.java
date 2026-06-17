package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签简要视图对象（仅 id + name），供下拉选择器使用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagSimpleVO {

    private Long id;

    private String name;
}
