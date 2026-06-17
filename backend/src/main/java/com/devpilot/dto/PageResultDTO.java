package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果 DTO
 * <p>
 * 将 MyBatis-Plus IPage 的 current 字段映射为 pageNum，与 API 契约保持一致。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultDTO<T> {

    private long total;

    private int pageNum;

    private int pageSize;

    private List<T> records;
}
