package com.devpilot.controller;

import com.devpilot.common.ApiResponse;
import com.devpilot.dto.*;
import com.devpilot.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签控制器 — 处理 /api/tags/** 请求
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 创建标签
     * <p>
     * 权限要求：开发者 / 管理员
     *
     * @param dto 创建请求参数（含 @Valid 校验）
     * @return 新创建的标签信息
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ApiResponse<TagVO> create(@Valid @RequestBody TagCreateDTO dto) {
        TagVO vo = tagService.create(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 编辑标签名称
     * <p>
     * 权限要求：开发者 / 管理员
     *
     * @param dto 编辑请求参数（含 @Valid 校验）
     * @return 更新后的标签信息
     */
    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ApiResponse<TagVO> update(@Valid @RequestBody TagUpdateDTO dto) {
        TagVO vo = tagService.update(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 删除标签
     * <p>
     * 权限要求：开发者 / 管理员
     *
     * @param dto 删除请求参数（含 @Valid 校验）
     * @return 删除结果（含级联删除的关联记录数）
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ApiResponse<TagDeleteResultDTO> delete(@Valid @RequestBody TagDeleteDTO dto) {
        TagDeleteResultDTO result = tagService.delete(dto);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询标签列表（含关联笔记数统计）
     * <p>
     * 权限要求：所有已登录用户
     *
     * @param dto 分页请求参数
     * @return 分页结果
     */
    @PostMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResultDTO<TagVO>> list(@RequestBody TagListDTO dto) {
        int pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 10;
        // 限制最大 pageSize
        if (pageSize > 100) {
            pageSize = 100;
        }
        PageResultDTO<TagVO> page = tagService.page(pageNum, pageSize);
        return ApiResponse.success(page);
    }

    /**
     * 获取全部未删除标签（仅 id + name），供下拉选择器使用
     * <p>
     * 权限要求：所有已登录用户
     *
     * @return 标签简要信息列表
     */
    @PostMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TagSimpleVO>> all() {
        List<TagSimpleVO> tags = tagService.all();
        return ApiResponse.success(tags);
    }
}
