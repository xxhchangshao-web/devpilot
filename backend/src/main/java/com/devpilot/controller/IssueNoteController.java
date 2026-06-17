package com.devpilot.controller;

import com.devpilot.common.ApiResponse;
import com.devpilot.dto.*;
import com.devpilot.service.IssueNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问题笔记控制器 — 处理 /api/notes/** 请求
 * <p>
 * 提供笔记的分页查询、详情查看、创建、编辑、删除、状态变更共 6 个接口。
 * 所有接口需 JWT Token 鉴权（由 SecurityConfig 统一配置）。
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class IssueNoteController {

    private final IssueNoteService issueNoteService;

    /**
     * 分页查询笔记列表
     * <p>
     * 支持关键词模糊搜索（title + description）、标签筛选、状态/优先级/分类多条件筛选。
     * 默认按更新时间倒序，每页 20 条。
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @PostMapping("/list")
    public ApiResponse<PageResultVO<NoteListVO>> list(@Valid @RequestBody NotePageQuery query) {
        PageResultVO<NoteListVO> result = issueNoteService.page(query);
        return ApiResponse.success(result);
    }

    /**
     * 获取笔记详情
     * <p>
     * 返回完整字段 + 关联标签 + 创建者信息 + 编辑权限标识。
     * 笔记不存在返回 404。
     *
     * @param params 请求参数（id: 笔记 ID）
     * @return 笔记详情
     */
    @PostMapping("/detail")
    public ApiResponse<NoteDetailVO> detail(@Valid @RequestBody NoteDetailDTO params) {
        NoteDetailVO detail = issueNoteService.getDetail(params.getId());
        return ApiResponse.success(detail);
    }

    /**
     * 创建笔记
     * <p>
     * 需要 DEVELOPER 或 ADMIN 角色。自动填充创建者信息和时间戳。
     * 支持同时关联标签。整个操作在同一事务内。
     *
     * @param dto 创建请求参数
     * @return 创建的笔记基本信息
     */
    @PostMapping("/create")
    public ApiResponse<NoteDetailVO> create(@Valid @RequestBody NoteCreateDTO dto) {
        NoteDetailVO result = issueNoteService.create(dto);
        return ApiResponse.success(result);
    }

    /**
     * 编辑笔记
     * <p>
     * 仅创建者或管理员可操作。不包含 status 字段修改（使用 changeStatus）。
     * 标签关联采用先删后插策略。整个操作在同一事务内。
     *
     * @param dto 编辑请求参数
     * @return 更新后的笔记基本信息
     */
    @PostMapping("/update")
    public ApiResponse<NoteDetailVO> update(@Valid @RequestBody NoteUpdateDTO dto) {
        NoteDetailVO result = issueNoteService.update(dto);
        return ApiResponse.success(result);
    }

    /**
     * 删除笔记（逻辑删除）
     * <p>
     * 设置 is_deleted=1，同时清理关联标签。仅创建者或管理员可操作。此操作不可撤销。
     *
     * @param params 请求参数（id: 笔记 ID）
     * @return 成功响应（无数据）
     */
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody NoteDeleteDTO params) {
        issueNoteService.delete(params.getId());
        return ApiResponse.success();
    }

    /**
     * 更改笔记状态
     * <p>
     * 支持状态流转规则校验。ARCHIVED 为终态不可变更。
     * 仅创建者或管理员可操作。
     *
     * @param dto 状态变更请求参数
     * @return 变更结果（新状态 + 变更前状态 + 更新时间）
     */
    @PostMapping("/changeStatus")
    public ApiResponse<ChangeStatusResultVO> changeStatus(@Valid @RequestBody ChangeStatusDTO dto) {
        ChangeStatusResultVO result = issueNoteService.changeStatus(dto);
        return ApiResponse.success(result);
    }
}
