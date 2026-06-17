package com.devpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devpilot.entity.NoteTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 笔记标签关联 Mapper
 * <p>
 * 提供批量插入和按笔记/标签 ID 删除的 SQL
 */
@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    /**
     * 批量插入标签关联
     *
     * @param noteId     笔记 ID
     * @param tagIds     标签 ID 列表
     * @return 插入行数
     */
    int batchInsert(@Param("noteId") Long noteId, @Param("tagIds") List<Long> tagIds);

    /**
     * 按笔记 ID 删除所有标签关联
     *
     * @param noteId 笔记 ID
     * @return 删除行数
     */
    int deleteByNoteId(@Param("noteId") Long noteId);

    /**
     * 按标签 ID 物理删除所有关联记录（级联删除使用）
     *
     * @param tagId 标签 ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM t_note_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);
}
