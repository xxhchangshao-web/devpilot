package com.devpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpilot.common.BusinessException;
import com.devpilot.dto.*;
import com.devpilot.entity.Tag;
import com.devpilot.mapper.NoteTagMapper;
import com.devpilot.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务 — 处理标签 CRUD 业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;

    /**
     * 创建标签
     * <p>
     * 前置校验标签名称唯一性（未删除标签范围内），通过后插入新记录。
     *
     * @param dto 创建请求参数
     * @return 新创建的标签信息
     * @throws BusinessException(409) 标签名称已存在
     */
    public TagVO create(TagCreateDTO dto) {
        // 名称唯一性校验
        checkNameUnique(dto.getName(), null);

        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());

        tagMapper.insert(tag);
        log.info("标签创建成功: id={}, name={}", tag.getId(), tag.getName());

        return toTagVO(tag);
    }

    /**
     * 编辑标签名称
     * <p>
     * 校验目标标签存在且未被删除，校验新名称唯一性（排除自身），通过后更新。
     *
     * @param dto 编辑请求参数
     * @return 更新后的标签信息
     * @throws BusinessException(404) 标签不存在
     * @throws BusinessException(409) 标签名称已被其他标签使用
     */
    public TagVO update(TagUpdateDTO dto) {
        // 查询标签是否存在（未删除）
        Tag tag = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getId, dto.getId())
        );
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }

        // 名称唯一性校验（排除自身）
        checkNameUnique(dto.getName(), dto.getId());

        tag.setName(dto.getName());
        tag.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(tag);
        log.info("标签编辑成功: id={}, name={}", tag.getId(), tag.getName());

        return toTagVO(tag);
    }

    /**
     * 删除标签（事务内执行）
     * <p>
     * 先统计关联笔记数，然后在事务内：物理删除 t_note_tag 关联记录 + 软删除 t_tag。
     *
     * @param dto 删除请求参数
     * @return 删除结果（含级联删除的关联记录数）
     * @throws BusinessException(404) 标签不存在
     */
    @Transactional
    public TagDeleteResultDTO delete(TagDeleteDTO dto) {
        // 查询标签是否存在（未删除）
        Tag tag = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getId, dto.getId())
        );
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }

        // 级联物理删除关联记录
        int deletedRows = noteTagMapper.deleteByTagId(dto.getId());
        log.info("级联删除标签关联记录: tagId={}, deletedRows={}", dto.getId(), deletedRows);

        // 软删除标签
        tag.setIsDeleted(1);
        tag.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(tag);
        log.info("标签软删除成功: id={}, name={}", tag.getId(), tag.getName());

        return new TagDeleteResultDTO(deletedRows);
    }

    /**
     * 分页查询标签列表（含关联笔记数统计）
     * <p>
     * LEFT JOIN t_note_tag 统计每个标签的关联笔记数，按创建时间倒序。
     *
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页条数（默认 10，最大 100）
     * @return 分页结果（使用 PageResultDTO 映射，确保字段名与 API 契约一致）
     */
    public PageResultDTO<TagVO> page(int pageNum, int pageSize) {
        Page<TagVO> page = new Page<>(pageNum, pageSize);
        IPage<TagVO> result = tagMapper.selectPageWithNoteCount(page);
        return new PageResultDTO<>(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                result.getRecords()
        );
    }

    /**
     * 获取全部未删除标签（仅 id + name），供下拉选择器使用
     * <p>
     * 按标签名称字母升序排列。
     *
     * @return 标签简要信息列表
     */
    public List<TagSimpleVO> all() {
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .orderByAsc(Tag::getName)
        );
        return tags.stream()
                .map(tag -> new TagSimpleVO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 检查标签名称唯一性
     * <p>
     * 查询未删除标签中是否存在同名标签。
     * 编辑场景通过 excludeId 排除自身。
     * <p>
     * {@code @TableLogic} 自动过滤 is_deleted=1 的记录。
     * 数据库 uk_tag_name 唯一索引作为兜底保障（覆盖软删除记录）。
     *
     * @param name      待校验的名称
     * @param excludeId 排除的标签 ID（创建时为 null）
     * @throws BusinessException(409) 名称已存在
     */
    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name);
        if (excludeId != null) {
            wrapper.ne(Tag::getId, excludeId);
        }
        Long count = tagMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(409, "标签名称已存在");
        }
    }

    /**
     * 将 Tag 实体转为 TagVO
     * <p>
     * Jackson 按 application.yml 中 date-format 将 LocalDateTime 序列化为 "yyyy-MM-dd HH:mm:ss"。
     */
    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCreateTime(tag.getCreatedAt());
        vo.setUpdateTime(tag.getUpdatedAt());
        return vo;
    }
}
