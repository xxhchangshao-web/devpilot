package com.devpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpilot.entity.IssueNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 问题笔记 Mapper
 * <p>
 * 提供分页查询的自定义 SQL，支持关键词模糊搜索 + 多条件组合筛选
 */
@Mapper
public interface IssueNoteMapper extends BaseMapper<IssueNote> {

    /**
     * 分页查询笔记列表（手写 SQL）
     * <p>
     * 支持：keyword LIKE title/description、eq status/priority/category、inSql tagIds 子查询
     *
     * @param page     分页参数
     * @param keyword  关键词（可选，模糊匹配 title + description）
     * @param status   状态筛选（可选）
     * @param priority 优先级筛选（可选）
     * @param category 分类筛选（可选）
     * @param tagIds   标签 ID 列表（可选，AND 逻辑，通过 t_note_tag 子查询）
     * @return 分页结果
     */
    Page<IssueNote> selectPageWithConditions(
            Page<IssueNote> page,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("category") String category,
            @Param("tagIds") java.util.List<Long> tagIds);
}
