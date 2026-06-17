package com.devpilot.controller;

import com.devpilot.common.ApiResponse;
import com.devpilot.common.BusinessException;
import com.devpilot.dto.*;
import com.devpilot.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TagController 单元测试 — 标签 API 端点
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagController 单元测试")
class TagControllerTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    // ==================== create() 测试 ====================

    @Nested
    @DisplayName("POST /api/tags/create")
    class Create {

        @Test
        @DisplayName("应该返回成功响应包含创建的标签")
        void should_returnSuccessWithCreatedTag() {
            // Arrange
            TagCreateDTO dto = new TagCreateDTO();
            dto.setName("Bug");

            TagVO vo = new TagVO();
            vo.setId(1L);
            vo.setName("Bug");
            vo.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));

            when(tagService.create(any(TagCreateDTO.class))).thenReturn(vo);

            // Act
            ApiResponse<TagVO> response = tagController.create(dto);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getId()).isEqualTo(1L);
            assertThat(response.getData().getName()).isEqualTo("Bug");

            verify(tagService).create(dto);
        }

        @Test
        @DisplayName("应该传播 BusinessException 当标签名称重复")
        void should_propagateBusinessException_when_nameDuplicate() {
            // Arrange
            TagCreateDTO dto = new TagCreateDTO();
            dto.setName("ExistingTag");

            when(tagService.create(any(TagCreateDTO.class)))
                    .thenThrow(new BusinessException(409, "标签名称已存在"));

            // Act & Assert
            assertThatThrownBy(() -> tagController.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(409);
                    });
        }
    }

    // ==================== update() 测试 ====================

    @Nested
    @DisplayName("POST /api/tags/update")
    class Update {

        @Test
        @DisplayName("应该返回成功响应包含更新后的标签")
        void should_returnSuccessWithUpdatedTag() {
            // Arrange
            TagUpdateDTO dto = new TagUpdateDTO();
            dto.setId(1L);
            dto.setName("Feature");

            TagVO vo = new TagVO();
            vo.setId(1L);
            vo.setName("Feature");
            vo.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));

            when(tagService.update(any(TagUpdateDTO.class))).thenReturn(vo);

            // Act
            ApiResponse<TagVO> response = tagController.update(dto);

            // Assert
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData().getName()).isEqualTo("Feature");

            verify(tagService).update(dto);
        }

        @Test
        @DisplayName("应该传播 BusinessException(404) 当标签不存在")
        void should_propagateBusinessException_when_tagNotFound() {
            // Arrange
            TagUpdateDTO dto = new TagUpdateDTO();
            dto.setId(999L);
            dto.setName("NewName");

            when(tagService.update(any(TagUpdateDTO.class)))
                    .thenThrow(new BusinessException(404, "标签不存在"));

            // Act & Assert
            assertThatThrownBy(() -> tagController.update(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
        }
    }

    // ==================== delete() 测试 ====================

    @Nested
    @DisplayName("POST /api/tags/delete")
    class Delete {

        @Test
        @DisplayName("应该返回成功响应包含删除结果")
        void should_returnSuccessWithDeleteResult() {
            // Arrange
            TagDeleteDTO dto = new TagDeleteDTO();
            dto.setId(1L);

            TagDeleteResultDTO result = new TagDeleteResultDTO(5);

            when(tagService.delete(any(TagDeleteDTO.class))).thenReturn(result);

            // Act
            ApiResponse<TagDeleteResultDTO> response = tagController.delete(dto);

            // Assert
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData().getDeletedNoteTags()).isEqualTo(5);

            verify(tagService).delete(dto);
        }

        @Test
        @DisplayName("应该传播 BusinessException(404) 当标签不存在")
        void should_propagateBusinessException_when_tagNotFound() {
            // Arrange
            TagDeleteDTO dto = new TagDeleteDTO();
            dto.setId(999L);

            when(tagService.delete(any(TagDeleteDTO.class)))
                    .thenThrow(new BusinessException(404, "标签不存在"));

            // Act & Assert
            assertThatThrownBy(() -> tagController.delete(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
        }
    }

    // ==================== list() 测试 ====================

    @Nested
    @DisplayName("POST /api/tags/list")
    class ListTags {

        @Test
        @DisplayName("应该使用默认分页参数当 DTO 参数为 null")
        void should_useDefaultParams_when_dtoParamsAreNull() {
            // Arrange
            TagListDTO dto = new TagListDTO(); // pageNum and pageSize are null

            PageResultDTO<TagVO> pageResult = new PageResultDTO<>();
            pageResult.setTotal(2L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setRecords(Collections.emptyList());

            when(tagService.page(1, 10)).thenReturn(pageResult);

            // Act
            ApiResponse<PageResultDTO<TagVO>> response = tagController.list(dto);

            // Assert
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData().getPageNum()).isEqualTo(1);
            assertThat(response.getData().getPageSize()).isEqualTo(10);

            verify(tagService).page(1, 10);
        }

        @Test
        @DisplayName("应该使用自定义分页参数")
        void should_useCustomParams_when_provided() {
            // Arrange
            TagListDTO dto = new TagListDTO();
            dto.setPageNum(2);
            dto.setPageSize(20);

            PageResultDTO<TagVO> pageResult = new PageResultDTO<>();
            pageResult.setTotal(30L);
            pageResult.setPageNum(2);
            pageResult.setPageSize(20);
            pageResult.setRecords(Collections.emptyList());

            when(tagService.page(2, 20)).thenReturn(pageResult);

            // Act
            ApiResponse<PageResultDTO<TagVO>> response = tagController.list(dto);

            // Assert
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData().getPageNum()).isEqualTo(2);
            assertThat(response.getData().getPageSize()).isEqualTo(20);

            verify(tagService).page(2, 20);
        }

        @Test
        @DisplayName("应该限制 pageSize 最大为 100")
        void should_capPageSize_when_exceedsMax() {
            // Arrange
            TagListDTO dto = new TagListDTO();
            dto.setPageNum(1);
            dto.setPageSize(200); // exceeds max

            PageResultDTO<TagVO> pageResult = new PageResultDTO<>();
            pageResult.setTotal(0L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(100);
            pageResult.setRecords(Collections.emptyList());

            when(tagService.page(1, 100)).thenReturn(pageResult);

            // Act
            ApiResponse<PageResultDTO<TagVO>> response = tagController.list(dto);

            // Assert
            assertThat(response.getData().getPageSize()).isEqualTo(100);

            verify(tagService).page(1, 100);
        }

        @Test
        @DisplayName("应该返回分页数据包含标签列表")
        void should_returnPageDataWithTagList() {
            // Arrange
            TagListDTO dto = new TagListDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);

            TagVO vo1 = new TagVO();
            vo1.setId(1L);
            vo1.setName("Bug");
            vo1.setNoteCount(3);

            PageResultDTO<TagVO> pageResult = new PageResultDTO<>();
            pageResult.setTotal(1L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setRecords(Arrays.asList(vo1));

            when(tagService.page(1, 10)).thenReturn(pageResult);

            // Act
            ApiResponse<PageResultDTO<TagVO>> response = tagController.list(dto);

            // Assert
            assertThat(response.getData().getRecords()).hasSize(1);
            assertThat(response.getData().getRecords().get(0).getName()).isEqualTo("Bug");
            assertThat(response.getData().getRecords().get(0).getNoteCount()).isEqualTo(3);
        }
    }

    // ==================== all() 测试 ====================

    @Nested
    @DisplayName("POST /api/tags/all")
    class All {

        @Test
        @DisplayName("应该返回所有标签简要信息")
        void should_returnAllTagSimpleInfo() {
            // Arrange
            TagSimpleVO vo1 = new TagSimpleVO(1L, "Bug");
            TagSimpleVO vo2 = new TagSimpleVO(2L, "Feature");
            List<TagSimpleVO> tags = Arrays.asList(vo1, vo2);

            when(tagService.all()).thenReturn(tags);

            // Act
            ApiResponse<List<TagSimpleVO>> response = tagController.all();

            // Assert
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData().get(0).getName()).isEqualTo("Bug");
            assertThat(response.getData().get(1).getName()).isEqualTo("Feature");

            verify(tagService).all();
        }

        @Test
        @DisplayName("应该返回空列表当无标签数据")
        void should_returnEmptyList_when_noTags() {
            // Arrange
            when(tagService.all()).thenReturn(Collections.emptyList());

            // Act
            ApiResponse<List<TagSimpleVO>> response = tagController.all();

            // Assert
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData()).isEmpty();
        }
    }
}
