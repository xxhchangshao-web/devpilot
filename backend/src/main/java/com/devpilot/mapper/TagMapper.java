package com.devpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpilot.dto.TagVO;
import com.devpilot.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;

/**
 * 标签 Mapper，提供分页查询（含关联笔记数统计）
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 分页查询标签列表，LEFT JOIN 统计每个标签的关联笔记数
     * <p>
     * 仅查询未删除标签（is_deleted = 0），按创建时间倒序。
     * 对于无关联笔记的标签，noteCount 为 0。
     *
     * @param page 分页参数
     * @return 分页结果，records 为 TagVO 列表
     */
    @Select("SELECT t.id, t.name, t.created_at, t.updated_at, COUNT(nt.id) AS note_count " +
            "FROM t_tag t " +
            "LEFT JOIN t_note_tag nt ON t.id = nt.tag_id " +
            "WHERE t.is_deleted = 0 " +
            "GROUP BY t.id, t.name, t.created_at, t.updated_at " +
            "ORDER BY t.created_at DESC")
    @Results({
        @Result(column = "created_at", property = "createTime"),
        @Result(column = "updated_at", property = "updateTime"),
        @Result(column = "note_count", property = "noteCount")
    })
    IPage<TagVO> selectPageWithNoteCount(Page<TagVO> page);
}
