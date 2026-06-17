package com.devpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpilot.common.BusinessException;
import com.devpilot.dto.*;
import com.devpilot.entity.Tag;
import com.devpilot.mapper.NoteTagMapper;
import com.devpilot.mapper.TagMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TagService 单元测试 — 标签 CRUD 业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagService 单元测试")
class TagServiceTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private NoteTagMapper noteTagMapper;

    @InjectMocks
    private TagService tagService;

    // ==================== create() 测试 ====================

    @Nested
    @DisplayName("创建标签")
    class Create {

        @Test
        @DisplayName("应该成功创建标签当名称未重复")
        void should_createTag_when_nameUnique() {
            // Arrange
            TagCreateDTO dto = new TagCreateDTO();
            dto.setName("Bug");

            when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
                Tag tag = invocation.getArgument(0);
                tag.setId(1L);
                return 1;
            });

            // Act
            TagVO result = tagService.create(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Bug");
            assertThat(result.getCreateTime()).isNotNull();

            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            verify(tagMapper).insert(tagCaptor.capture());
            assertThat(tagCaptor.getValue().getName()).isEqualTo("Bug");
        }

        @Test
        @DisplayName("应该抛出 BusinessException(409) 当标签名称已存在")
        void should_throwBusinessException_when_nameExists() {
            // Arrange
            TagCreateDTO dto = new TagCreateDTO();
            dto.setName("Bug");

            when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> tagService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(409);
                        assertThat(be.getMessage()).isEqualTo("标签名称已存在");
                    });

            verify(tagMapper, never()).insert(any(Tag.class));
        }
    }

    // ==================== update() 测试 ====================

    @Nested
    @DisplayName("编辑标签")
    class Update {

        @Test
        @DisplayName("应该成功编辑标签当标签存在且新名称唯一")
        void should_updateTag_when_tagExistsAndNameUnique() {
            // Arrange
            TagUpdateDTO dto = new TagUpdateDTO();
            dto.setId(1L);
            dto.setName("Feature");

            Tag existingTag = new Tag();
            existingTag.setId(1L);
            existingTag.setName("Bug");
            existingTag.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            existingTag.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingTag);
            // 名称唯一性校验（排除自身）
            when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(tagMapper.updateById(any(Tag.class))).thenReturn(1);

            // Act
            TagVO result = tagService.update(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Feature");

            verify(tagMapper).updateById(any(Tag.class));
        }

        @Test
        @DisplayName("应该抛出 BusinessException(404) 当标签不存在")
        void should_throwBusinessException_when_tagNotExists() {
            // Arrange
            TagUpdateDTO dto = new TagUpdateDTO();
            dto.setId(999L);
            dto.setName("NewName");

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> tagService.update(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(404);
                        assertThat(be.getMessage()).isEqualTo("标签不存在");
                    });

            verify(tagMapper, never()).updateById(any(Tag.class));
        }

        @Test
        @DisplayName("应该抛出 BusinessException(409) 当新名称已被其他标签使用")
        void should_throwBusinessException_when_nameUsedByOtherTag() {
            // Arrange
            TagUpdateDTO dto = new TagUpdateDTO();
            dto.setId(1L);
            dto.setName("Feature");

            Tag existingTag = new Tag();
            existingTag.setId(1L);
            existingTag.setName("Bug");

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingTag);
            // 名称校验发现已有其他标签使用
            when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> tagService.update(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(409);
                        assertThat(be.getMessage()).isEqualTo("标签名称已存在");
                    });

            verify(tagMapper, never()).updateById(any(Tag.class));
        }

        @Test
        @DisplayName("应该在名称未变更时也能成功更新（排除自身校验通过）")
        void should_updateSuccessfully_when_nameUnchanged() {
            // Arrange
            TagUpdateDTO dto = new TagUpdateDTO();
            dto.setId(1L);
            dto.setName("Bug"); // 名称不变

            Tag existingTag = new Tag();
            existingTag.setId(1L);
            existingTag.setName("Bug");
            existingTag.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            existingTag.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingTag);
            // 查出来的是自己，excludeId 排除掉了
            when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(tagMapper.updateById(any(Tag.class))).thenReturn(1);

            // Act
            TagVO result = tagService.update(dto);

            // Assert
            assertThat(result.getName()).isEqualTo("Bug");
            verify(tagMapper).updateById(any(Tag.class));
        }
    }

    // ==================== delete() 测试 ====================

    @Nested
    @DisplayName("删除标签")
    class Delete {

        @Test
        @DisplayName("应该成功软删除标签并级联删除关联记录")
        void should_softDeleteTagAndCascadeDeleteRelations_when_tagExists() {
            // Arrange
            TagDeleteDTO dto = new TagDeleteDTO();
            dto.setId(1L);

            Tag existingTag = new Tag();
            existingTag.setId(1L);
            existingTag.setName("Bug");
            existingTag.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            existingTag.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingTag);
            when(noteTagMapper.deleteByTagId(1L)).thenReturn(5);
            when(tagMapper.updateById(any(Tag.class))).thenReturn(1);

            // Act
            TagDeleteResultDTO result = tagService.delete(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDeletedNoteTags()).isEqualTo(5);

            verify(noteTagMapper).deleteByTagId(1L);

            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            verify(tagMapper).updateById(tagCaptor.capture());
            assertThat(tagCaptor.getValue().getIsDeleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该抛出 BusinessException(404) 当标签不存在")
        void should_throwBusinessException_when_tagNotExists() {
            // Arrange
            TagDeleteDTO dto = new TagDeleteDTO();
            dto.setId(999L);

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> tagService.delete(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(404);
                        assertThat(be.getMessage()).isEqualTo("标签不存在");
                    });

            verify(noteTagMapper, never()).deleteByTagId(anyLong());
            verify(tagMapper, never()).updateById(any(Tag.class));
        }

        @Test
        @DisplayName("应该在无关联笔记时返回 deletedNoteTags = 0")
        void should_returnZeroDeletedNoteTags_when_noRelations() {
            // Arrange
            TagDeleteDTO dto = new TagDeleteDTO();
            dto.setId(1L);

            Tag existingTag = new Tag();
            existingTag.setId(1L);
            existingTag.setName("Bug");
            existingTag.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            existingTag.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

            when(tagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingTag);
            when(noteTagMapper.deleteByTagId(1L)).thenReturn(0);
            when(tagMapper.updateById(any(Tag.class))).thenReturn(1);

            // Act
            TagDeleteResultDTO result = tagService.delete(dto);

            // Assert
            assertThat(result.getDeletedNoteTags()).isEqualTo(0);
        }
    }

    // ==================== page() 测试 ====================

    @Nested
    @DisplayName("分页查询标签列表")
    class PageQuery {

        @Test
        @DisplayName("应该返回分页结果当存在标签数据")
        void should_returnPageResult_when_dataExists() {
            // Arrange
            TagVO vo1 = new TagVO();
            vo1.setId(1L);
            vo1.setName("Bug");
            vo1.setNoteCount(3);
            vo1.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));

            TagVO vo2 = new TagVO();
            vo2.setId(2L);
            vo2.setName("Feature");
            vo2.setNoteCount(1);
            vo2.setCreateTime(LocalDateTime.of(2025, 2, 1, 10, 0));

            @SuppressWarnings("unchecked")
            IPage<TagVO> mockPage = mock(IPage.class);
            when(mockPage.getTotal()).thenReturn(2L);
            when(mockPage.getCurrent()).thenReturn(1L);
            when(mockPage.getSize()).thenReturn(10L);
            when(mockPage.getRecords()).thenReturn(Arrays.asList(vo1, vo2));

            when(tagMapper.selectPageWithNoteCount(any(Page.class))).thenReturn(mockPage);

            // Act
            PageResultDTO<TagVO> result = tagService.page(1, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(2L);
            assertThat(result.getPageNum()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getRecords().get(0).getName()).isEqualTo("Bug");
            assertThat(result.getRecords().get(0).getNoteCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("应该返回空列表当无数据")
        void should_returnEmptyList_when_noData() {
            // Arrange
            @SuppressWarnings("unchecked")
            IPage<TagVO> mockPage = mock(IPage.class);
            when(mockPage.getTotal()).thenReturn(0L);
            when(mockPage.getCurrent()).thenReturn(1L);
            when(mockPage.getSize()).thenReturn(10L);
            when(mockPage.getRecords()).thenReturn(Collections.emptyList());

            when(tagMapper.selectPageWithNoteCount(any(Page.class))).thenReturn(mockPage);

            // Act
            PageResultDTO<TagVO> result = tagService.page(1, 10);

            // Assert
            assertThat(result.getTotal()).isEqualTo(0L);
            assertThat(result.getRecords()).isEmpty();
        }
    }

    // ==================== all() 测试 ====================

    @Nested
    @DisplayName("获取全部标签")
    class All {

        @Test
        @DisplayName("应该返回所有未删除标签按名称升序")
        void should_returnAllTagsOrderedByNameAsc() {
            // Arrange
            Tag tag1 = new Tag();
            tag1.setId(1L);
            tag1.setName("Bug");

            Tag tag2 = new Tag();
            tag2.setId(2L);
            tag2.setName("Feature");

            when(tagMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(tag1, tag2));

            // Act
            List<TagSimpleVO> result = tagService.all();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getName()).isEqualTo("Bug");
            assertThat(result.get(1).getId()).isEqualTo(2L);
            assertThat(result.get(1).getName()).isEqualTo("Feature");
        }

        @Test
        @DisplayName("应该返回空列表当无标签")
        void should_returnEmptyList_when_noTags() {
            // Arrange
            when(tagMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<TagSimpleVO> result = tagService.all();

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
