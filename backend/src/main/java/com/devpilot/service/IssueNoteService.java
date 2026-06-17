package com.devpilot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpilot.common.BusinessException;
import com.devpilot.dto.*;
import com.devpilot.entity.IssueNote;
import com.devpilot.entity.NoteTag;
import com.devpilot.entity.User;
import com.devpilot.mapper.IssueNoteMapper;
import com.devpilot.mapper.NoteTagMapper;
import com.devpilot.mapper.TagMapper;
import com.devpilot.mapper.UserMapper;
import com.devpilot.security.SecurityUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 问题笔记服务 — 处理笔记 CRUD 业务逻辑
 * <p>
 * 包含权限校验（创建者或管理员）、状态流转校验、事务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueNoteService {

    private final IssueNoteMapper issueNoteMapper;
    private final NoteTagMapper noteTagMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;

    private static final DateTimeFormatter DATE_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 状态流转规则矩阵 ====================
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "OPEN", Set.of("IN_PROGRESS", "ARCHIVED"),
            "IN_PROGRESS", Set.of("RESOLVED", "OPEN"),
            "RESOLVED", Set.of("OPEN", "ARCHIVED")
    );

    /**
     * 分页查询笔记列表
     * <p>
     * 支持关键词模糊搜索（title + description）、标签筛选、状态/优先级/分类多条件组合筛选。
     * 默认按更新时间倒序排列。所有已登录用户均可访问。
     *
     * @param query 分页查询参数
     * @return 分页结果（records + total + pageNum + pageSize）
     */
    public PageResultVO<NoteListVO> page(NotePageQuery query) {
        // 构建分页对象
        Page<IssueNote> page = new Page<>(query.getPageNum(), query.getPageSize());

        // 执行分页查询（手写 SQL）
        Page<IssueNote> result = issueNoteMapper.selectPageWithConditions(
                page,
                query.getKeyword(),
                query.getStatus(),
                query.getPriority(),
                query.getCategory(),
                query.getTagIds());

        List<IssueNote> notes = result.getRecords();
        if (CollectionUtils.isEmpty(notes)) {
            return PageResultVO.<NoteListVO>builder()
                    .records(Collections.emptyList())
                    .total(result.getTotal())
                    .pageNum((int) result.getCurrent())
                    .pageSize((int) result.getSize())
                    .build();
        }

        // 批量获取标签
        List<Long> noteIds = notes.stream().map(IssueNote::getId).collect(Collectors.toList());
        Map<Long, List<TagVO>> tagsMap = batchQueryTags(noteIds);

        // 批量获取创建者用户名
        Map<Long, String> creatorNameMap = batchQueryCreatorNames(notes);

        // 组装 VO 列表
        List<NoteListVO> records = notes.stream().map(note -> NoteListVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .status(note.getStatus())
                .priority(note.getPriority())
                .category(note.getCategory())
                .tags(tagsMap.getOrDefault(note.getId(), Collections.emptyList()))
                .creatorName(creatorNameMap.getOrDefault(note.getCreatorId(), ""))
                .createdAt(formatDateTime(note.getCreatedAt(), DATE_MINUTE_FORMATTER))
                .updatedAt(formatDateTime(note.getUpdatedAt(), DATE_MINUTE_FORMATTER))
                .build()).collect(Collectors.toList());

        return PageResultVO.<NoteListVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .build();
    }

    /**
     * 获取笔记详情
     * <p>
     * 返回完整字段 + 关联标签 + 创建者用户名 + canEdit 权限标识。
     * 笔记不存在或已被逻辑删除返回 404。
     *
     * @param id 笔记 ID
     * @return 笔记详情 VO
     */
    public NoteDetailVO getDetail(Long id) {
        IssueNote note = findNoteById(id);
        SecurityUser currentUser = getCurrentUser();

        // 查询关联标签
        List<TagVO> tags = queryTagsByNoteId(note.getId());

        // 查询创建者用户名
        String creatorName = "";
        User creator = userMapper.selectById(note.getCreatorId());
        if (creator != null) {
            creatorName = creator.getUsername();
        }

        // 判断编辑权限：创建者或管理员
        boolean canEdit = note.getCreatorId().equals(currentUser.getUserId())
                || "ADMIN".equals(currentUser.getRole());

        return NoteDetailVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .description(note.getDescription())
                .investigation(Optional.ofNullable(note.getInvestigation()).orElse(""))
                .solution(Optional.ofNullable(note.getSolution()).orElse(""))
                .status(note.getStatus())
                .priority(note.getPriority())
                .category(note.getCategory())
                .tags(tags)
                .creatorId(note.getCreatorId())
                .creatorName(creatorName)
                .createdAt(formatDateTime(note.getCreatedAt(), DATE_SECOND_FORMATTER))
                .updatedAt(formatDateTime(note.getUpdatedAt(), DATE_SECOND_FORMATTER))
                .canEdit(canEdit)
                .build();
    }

    /**
     * 创建笔记
     * <p>
     * 自动填充 creator_id、created_at、updated_at，同时关联标签。
     * 需要 DEVELOPER 或 ADMIN 角色。整个操作在同一事务内。
     *
     * @param dto 创建请求参数
     * @return 创建的笔记基本信息
     */
    @Transactional
    public NoteDetailVO create(NoteCreateDTO dto) {
        SecurityUser currentUser = getCurrentUser();

        // 角色权限校验：仅 DEVELOPER 或 ADMIN 可创建
        checkRoleForCreate(currentUser);

        // 构建实体，设置默认值
        IssueNote note = new IssueNote();
        note.setTitle(dto.getTitle());
        note.setDescription(dto.getDescription());
        note.setInvestigation(dto.getInvestigation());
        note.setSolution(dto.getSolution());
        note.setStatus(Optional.ofNullable(dto.getStatus()).orElse("OPEN"));
        note.setPriority(Optional.ofNullable(dto.getPriority()).orElse("P2"));
        note.setCategory(Optional.ofNullable(dto.getCategory()).orElse("OTHER"));
        note.setCreatorId(currentUser.getUserId());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        issueNoteMapper.insert(note);
        log.info("笔记创建成功: id={}, title={}", note.getId(), note.getTitle());

        // 批量插入标签关联
        if (!CollectionUtils.isEmpty(dto.getTagIds())) {
            noteTagMapper.batchInsert(note.getId(), dto.getTagIds());
        }

        // 查询标签用于返回
        List<TagVO> tags = queryTagsByNoteId(note.getId());

        return NoteDetailVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .status(note.getStatus())
                .tags(tags)
                .createdAt(formatDateTime(note.getCreatedAt(), DATE_SECOND_FORMATTER))
                .updatedAt(formatDateTime(note.getUpdatedAt(), DATE_SECOND_FORMATTER))
                .build();
    }

    /**
     * 编辑笔记
     * <p>
     * 仅创建者或管理员可操作。不包含 status 字段的修改（使用 changeStatus 接口）。
     * 标签关联采用先删后插策略，确保原子性。
     *
     * @param dto 编辑请求参数
     * @return 更新后的笔记基本信息
     */
    @Transactional
    public NoteDetailVO update(NoteUpdateDTO dto) {
        IssueNote note = findNoteById(dto.getId());
        SecurityUser currentUser = getCurrentUser();

        // 权限校验：创建者或管理员
        checkOwnershipOrAdmin(note, currentUser);

        // 更新字段
        note.setTitle(dto.getTitle());
        note.setDescription(dto.getDescription());
        note.setInvestigation(dto.getInvestigation());
        note.setSolution(dto.getSolution());
        note.setPriority(dto.getPriority());
        note.setCategory(dto.getCategory());
        note.setUpdatedAt(LocalDateTime.now());

        issueNoteMapper.updateById(note);
        log.info("笔记更新成功: id={}", note.getId());

        // 先删后插标签关联
        noteTagMapper.deleteByNoteId(note.getId());
        if (!CollectionUtils.isEmpty(dto.getTagIds())) {
            noteTagMapper.batchInsert(note.getId(), dto.getTagIds());
        }

        return NoteDetailVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .updatedAt(formatDateTime(note.getUpdatedAt(), DATE_SECOND_FORMATTER))
                .build();
    }

    /**
     * 删除笔记（逻辑删除）
     * <p>
     * 设置 is_deleted=1，同时清理 t_note_tag 关联记录。
     * 仅创建者或管理员可操作。此操作不可撤销。
     *
     * @param id 笔记 ID
     */
    @Transactional
    public void delete(Long id) {
        IssueNote note = findNoteById(id);
        SecurityUser currentUser = getCurrentUser();

        // 权限校验：创建者或管理员
        checkOwnershipOrAdmin(note, currentUser);

        // 逻辑删除
        note.setIsDeleted(1);
        note.setUpdatedAt(LocalDateTime.now());
        issueNoteMapper.updateById(note);
        log.info("笔记逻辑删除成功: id={}", note.getId());

        // 清理标签关联
        noteTagMapper.deleteByNoteId(note.getId());
    }

    /**
     * 更改笔记状态
     * <p>
     * 支持状态流转规则：OPEN → IN_PROGRESS/ARCHIVED，IN_PROGRESS → RESOLVED/OPEN，
     * RESOLVED → OPEN/ARCHIVED。ARCHIVED 为终态不可变更。
     * 仅创建者或管理员可操作。
     *
     * @param dto 状态变更请求参数
     * @return 变更结果（新状态 + 变更前状态 + 更新时间）
     */
    @Transactional
    public ChangeStatusResultVO changeStatus(ChangeStatusDTO dto) {
        IssueNote note = findNoteById(dto.getId());
        SecurityUser currentUser = getCurrentUser();

        // 权限校验
        checkOwnershipOrAdmin(note, currentUser);

        String currentStatus = note.getStatus();
        String targetStatus = dto.getStatus();

        // 状态流转规则校验
        validateStatusTransition(currentStatus, targetStatus);

        // 更新状态
        String previousStatus = note.getStatus();
        note.setStatus(targetStatus);
        note.setUpdatedAt(LocalDateTime.now());
        issueNoteMapper.updateById(note);
        log.info("笔记状态变更成功: id={}, {} → {}", note.getId(), previousStatus, targetStatus);

        return ChangeStatusResultVO.builder()
                .id(note.getId())
                .status(targetStatus)
                .previousStatus(previousStatus)
                .updatedAt(formatDateTime(note.getUpdatedAt(), DATE_SECOND_FORMATTER))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据 ID 查询笔记，不存在或已删除则抛出 BusinessException(404)
     */
    private IssueNote findNoteById(Long id) {
        IssueNote note = issueNoteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在或已被删除");
        }
        return note;
    }

    /**
     * 获取当前登录用户信息
     */
    private SecurityUser getCurrentUser() {
        return (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * 校验创建权限：仅 DEVELOPER 或 ADMIN 角色可创建
     */
    private void checkRoleForCreate(SecurityUser user) {
        String role = user.getRole();
        if (!"DEVELOPER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "无权创建笔记，仅后端开发者和管理员可操作");
        }
    }

    /**
     * 校验拥有者或管理员权限
     */
    private void checkOwnershipOrAdmin(IssueNote note, SecurityUser user) {
        if (!note.getCreatorId().equals(user.getUserId()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "无权操作该笔记");
        }
    }

    /**
     * 校验状态流转规则
     * <p>
     * ARCHIVED 为终态，不可变更为其他状态。
     * 其他状态变更需遵循流转矩阵。
     */
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equals(targetStatus)) {
            return; // 同状态变更，幂等，允许
        }
        if ("ARCHIVED".equals(currentStatus)) {
            throw new BusinessException(400,
                    "状态流转不允许：当前状态 ARCHIVED 为终态，不可变更为其他状态");
        }
        Set<String> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new BusinessException(400,
                    "状态流转不允许：" + currentStatus + " → " + targetStatus);
        }
    }

    /**
     * 批量查询笔记的标签
     *
     * @param noteIds 笔记 ID 列表
     * @return noteId → 标签列表 的映射
     */
    private Map<Long, List<TagVO>> batchQueryTags(List<Long> noteIds) {
        if (CollectionUtils.isEmpty(noteIds)) {
            return Collections.emptyMap();
        }
        // 通过 t_note_tag JOIN t_tag 查询标签名称
        List<NoteTag> noteTags = noteTagMapper.selectList(
                new LambdaQueryWrapper<NoteTag>().in(NoteTag::getNoteId, noteIds));
        if (CollectionUtils.isEmpty(noteTags)) {
            return Collections.emptyMap();
        }

        List<Long> tagIds = noteTags.stream().map(NoteTag::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> tagNameMap = queryTagNameMap(tagIds);

        // 按 noteId 分组
        Map<Long, List<TagVO>> result = new HashMap<>();
        for (NoteTag nt : noteTags) {
            result.computeIfAbsent(nt.getNoteId(), k -> new ArrayList<>())
                    .add(new TagVO(nt.getTagId(), tagNameMap.getOrDefault(nt.getTagId(), "未知标签")));
        }
        return result;
    }

    /**
     * 查询单条笔记的标签
     */
    private List<TagVO> queryTagsByNoteId(Long noteId) {
        List<NoteTag> noteTags = noteTagMapper.selectList(
                new LambdaQueryWrapper<NoteTag>().eq(NoteTag::getNoteId, noteId));
        if (CollectionUtils.isEmpty(noteTags)) {
            return Collections.emptyList();
        }
        List<Long> tagIds = noteTags.stream().map(NoteTag::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> tagNameMap = queryTagNameMap(tagIds);
        return noteTags.stream()
                .map(nt -> new TagVO(nt.getTagId(), tagNameMap.getOrDefault(nt.getTagId(), "未知标签")))
                .collect(Collectors.toList());
    }

    private Map<Long, String> queryTagNameMap(List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyMap();
        }
        return tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(tag -> tag.getId(), tag -> tag.getName(), (a, b) -> a));
    }

    /**
     * 批量查询创建者用户名
     */
    private Map<Long, String> batchQueryCreatorNames(List<IssueNote> notes) {
        List<Long> creatorIds = notes.stream()
                .map(IssueNote::getCreatorId)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(creatorIds)) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(creatorIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime != null ? dateTime.format(formatter) : "";
    }
}
