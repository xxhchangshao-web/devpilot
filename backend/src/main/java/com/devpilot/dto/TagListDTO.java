package com.devpilot.dto;

import lombok.Data;

/**
 * 标签列表分页请求参数
 */
@Data
public class TagListDTO {

    /**
     * 页码（默认 1）
     */
    private Integer pageNum;

    /**
     * 每页条数（默认 10，最大 100）
     */
    private Integer pageSize;
}
