package com.devpilot.controller;

import com.devpilot.common.ApiResponse;
import com.devpilot.dto.*;
import com.devpilot.security.JwtTokenProvider;
import com.devpilot.service.IssueNoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * IssueNoteController 单元测试 — /api/notes/** 接口
 * <p>
 * 使用 @WebMvcTest 只加载 IssueNoteController，addFilters = false 跳过安全过滤器链。
 */
@WebMvcTest(IssueNoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("IssueNoteController 单元测试")
class IssueNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private IssueNoteService issueNoteService;

    // ==================== POST /api/notes/list ====================

    @Nested
    @DisplayName("分页查询 POST /api/notes/list")
    class ListTests {

        @Test
        @DisplayName("应该返回 200 和分页结果当查询成功")
        void should_return200_when_querySuccess() throws Exception {
            NotePageQuery query = new NotePageQuery();
            query.setPageNum(1);
            query.setPageSize(20);

            NoteListVO item = NoteListVO.builder()
                    .id(1L)
                    .title("测试笔记")
                    .status("OPEN")
                    .priority("P2")
                    .category("OTHER")
                    .tags(Collections.emptyList())
                    .creatorName("developer1")
                    .createdAt("2026-01-15 10:30")
                    .updatedAt("2026-01-15 14:30")
                    .build();

            PageResultVO<NoteListVO> pageResult = PageResultVO.<NoteListVO>builder()
                    .records(List.of(item))
                    .total(1L)
                    .pageNum(1)
                    .pageSize(20)
                    .build();

            when(issueNoteService.page(any(NotePageQuery.class))).thenReturn(pageResult);

            mockMvc.perform(post("/api/notes/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.records[0].title").value("测试笔记"))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.pageNum").value(1));

            verify(issueNoteService).page(any(NotePageQuery.class));
        }

        @Test
        @DisplayName("应该返回 200 和空列表当无笔记")
        void should_return200_when_emptyResult() throws Exception {
            NotePageQuery query = new NotePageQuery();
            query.setPageNum(1);
            query.setPageSize(20);

            PageResultVO<NoteListVO> pageResult = PageResultVO.<NoteListVO>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .pageNum(1)
                    .pageSize(20)
                    .build();

            when(issueNoteService.page(any(NotePageQuery.class))).thenReturn(pageResult);

            mockMvc.perform(post("/api/notes/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("应该返回 400 当 pageNum < 1")
        void should_return400_when_pageNumInvalid() throws Exception {
            NotePageQuery query = new NotePageQuery();
            query.setPageNum(0);
            query.setPageSize(20);

            mockMvc.perform(post("/api/notes/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当 pageSize > 100")
        void should_return400_when_pageSizeTooLarge() throws Exception {
            NotePageQuery query = new NotePageQuery();
            query.setPageNum(1);
            query.setPageSize(200);

            mockMvc.perform(post("/api/notes/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/notes/detail ====================

    @Nested
    @DisplayName("获取详情 POST /api/notes/detail")
    class DetailTests {

        @Test
        @DisplayName("应该返回 200 和笔记详情当笔记存在")
        void should_return200_when_noteExists() throws Exception {
            NoteDeleteDTO params = new NoteDeleteDTO();
            params.setId(1L);

            NoteDetailVO detail = NoteDetailVO.builder()
                    .id(1L)
                    .title("测试笔记")
                    .description("描述")
                    .status("OPEN")
                    .priority("P2")
                    .category("OTHER")
                    .tags(Collections.emptyList())
                    .creatorId(100L)
                    .creatorName("developer1")
                    .canEdit(true)
                    .createdAt("2026-01-15 10:30:00")
                    .updatedAt("2026-01-15 14:30:00")
                    .build();

            when(issueNoteService.getDetail(1L)).thenReturn(detail);

            mockMvc.perform(post("/api/notes/detail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.title").value("测试笔记"))
                    .andExpect(jsonPath("$.data.canEdit").value(true));
        }

        @Test
        @DisplayName("应该返回 400 当 id 为 null")
        void should_return400_when_idNull() throws Exception {
            NoteDeleteDTO params = new NoteDeleteDTO();
            params.setId(null);

            mockMvc.perform(post("/api/notes/detail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/notes/create ====================

    @Nested
    @DisplayName("创建笔记 POST /api/notes/create")
    class CreateTests {

        @Test
        @DisplayName("应该返回 200 和创建结果当请求有效")
        void should_return200_when_validRequest() throws Exception {
            NoteCreateDTO dto = new NoteCreateDTO();
            dto.setTitle("新笔记");
            dto.setDescription("问题描述");
            dto.setStatus("OPEN");
            dto.setPriority("P2");
            dto.setCategory("OTHER");

            NoteDetailVO created = NoteDetailVO.builder()
                    .id(1L)
                    .title("新笔记")
                    .status("OPEN")
                    .createdAt("2026-01-15 10:30:00")
                    .updatedAt("2026-01-15 10:30:00")
                    .build();

            when(issueNoteService.create(any(NoteCreateDTO.class))).thenReturn(created);

            mockMvc.perform(post("/api/notes/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("新笔记"))
                    .andExpect(jsonPath("$.data.status").value("OPEN"));
        }

        @Test
        @DisplayName("应该返回 400 当标题为空")
        void should_return400_when_titleBlank() throws Exception {
            NoteCreateDTO dto = new NoteCreateDTO();
            dto.setTitle("");
            dto.setDescription("描述");

            mockMvc.perform(post("/api/notes/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当标题为 null")
        void should_return400_when_titleNull() throws Exception {
            NoteCreateDTO dto = new NoteCreateDTO();
            dto.setTitle(null);
            dto.setDescription("描述");

            mockMvc.perform(post("/api/notes/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当描述为空")
        void should_return400_when_descriptionBlank() throws Exception {
            NoteCreateDTO dto = new NoteCreateDTO();
            dto.setTitle("标题");
            dto.setDescription("");

            mockMvc.perform(post("/api/notes/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当状态值非法")
        void should_return400_when_statusInvalid() throws Exception {
            NoteCreateDTO dto = new NoteCreateDTO();
            dto.setTitle("标题");
            dto.setDescription("描述");
            dto.setStatus("INVALID_STATUS");

            mockMvc.perform(post("/api/notes/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/notes/update ====================

    @Nested
    @DisplayName("编辑笔记 POST /api/notes/update")
    class UpdateTests {

        @Test
        @DisplayName("应该返回 200 和更新结果当请求有效")
        void should_return200_when_validRequest() throws Exception {
            NoteUpdateDTO dto = new NoteUpdateDTO();
            dto.setId(1L);
            dto.setTitle("更新后的标题");
            dto.setDescription("更新后的描述");
            dto.setPriority("P1");
            dto.setCategory("PERFORMANCE");

            NoteDetailVO updated = NoteDetailVO.builder()
                    .id(1L)
                    .title("更新后的标题")
                    .updatedAt("2026-01-15 15:00:00")
                    .build();

            when(issueNoteService.update(any(NoteUpdateDTO.class))).thenReturn(updated);

            mockMvc.perform(post("/api/notes/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.title").value("更新后的标题"));
        }

        @Test
        @DisplayName("应该返回 400 当 id 为 null")
        void should_return400_when_idNull() throws Exception {
            NoteUpdateDTO dto = new NoteUpdateDTO();
            dto.setId(null);
            dto.setTitle("标题");
            dto.setDescription("描述");

            mockMvc.perform(post("/api/notes/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/notes/delete ====================

    @Nested
    @DisplayName("删除笔记 POST /api/notes/delete")
    class DeleteTests {

        @Test
        @DisplayName("应该返回 200 当删除成功")
        void should_return200_when_deleteSuccess() throws Exception {
            NoteDeleteDTO params = new NoteDeleteDTO();
            params.setId(1L);

            doNothing().when(issueNoteService).delete(1L);

            mockMvc.perform(post("/api/notes/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"));

            verify(issueNoteService).delete(1L);
        }

        @Test
        @DisplayName("应该返回 400 当 id 为 null")
        void should_return400_when_idNull() throws Exception {
            NoteDeleteDTO params = new NoteDeleteDTO();
            params.setId(null);

            mockMvc.perform(post("/api/notes/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/notes/changeStatus ====================

    @Nested
    @DisplayName("变更状态 POST /api/notes/changeStatus")
    class ChangeStatusTests {

        @Test
        @DisplayName("应该返回 200 和变更结果当流转合法")
        void should_return200_when_validTransition() throws Exception {
            ChangeStatusDTO dto = new ChangeStatusDTO();
            dto.setId(1L);
            dto.setStatus("IN_PROGRESS");

            ChangeStatusResultVO result = new ChangeStatusResultVO(1L, "IN_PROGRESS", "OPEN", "2026-01-15 15:00:00");

            when(issueNoteService.changeStatus(any(ChangeStatusDTO.class))).thenReturn(result);

            mockMvc.perform(post("/api/notes/changeStatus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.previousStatus").value("OPEN"));
        }

        @Test
        @DisplayName("应该返回 400 当状态值为空")
        void should_return400_when_statusBlank() throws Exception {
            ChangeStatusDTO dto = new ChangeStatusDTO();
            dto.setId(1L);
            dto.setStatus("");

            mockMvc.perform(post("/api/notes/changeStatus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当 id 为 null")
        void should_return400_when_idNull() throws Exception {
            ChangeStatusDTO dto = new ChangeStatusDTO();
            dto.setId(null);
            dto.setStatus("OPEN");

            mockMvc.perform(post("/api/notes/changeStatus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }
}
