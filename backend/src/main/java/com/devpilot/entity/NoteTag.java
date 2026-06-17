package com.devpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记标签关联持久化对象，对应 t_note_tag 表
 * <p>
 * 实现笔记与标签的多对多关联
 */
@Data
@TableName("t_note_tag")
public class NoteTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 笔记ID，逻辑外键关联 t_issue_note.id */
    private Long noteId;

    /** 标签ID，逻辑外键关联 t_tag.id */
    private Long tagId;

    /** 关联创建时间 */
    private LocalDateTime createdAt;
}
