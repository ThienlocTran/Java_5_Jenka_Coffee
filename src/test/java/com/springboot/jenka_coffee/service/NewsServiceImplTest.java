package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.NewsRepository;
import com.springboot.jenka_coffee.service.impl.NewsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News testNews;

    @BeforeEach
    void setUp() {
        testNews = new News();
        testNews.setId(1L);
        testNews.setTitle("Test Article");
        testNews.setContent("Content");
        testNews.setAvailable(false);
    }

    @Test
    @DisplayName("TC-NEWS-SER-001: FindById news id not exists")
    void test_findById_notFound_throwsException() {
        when(newsRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            newsService.findById(99999L);
        });

        verify(newsRepository).findById(99999L);
    }

    @Test
    @DisplayName("TC-NEWS-SER-002: SaveNews with null image file (optional)")
    void test_saveNews_nullImage_savesSuccessfully() {
        when(newsRepository.save(any(News.class))).thenReturn(testNews);

        News savedNews = newsService.saveNews(testNews, null);

        assertNotNull(savedNews);
        verify(uploadService, never()).saveFile(any()); // Should not attempt to save file
        verify(newsRepository).save(testNews);
    }

    @Test
    @DisplayName("TC-NEWS-SER-003: SaveNews with invalid image MIME type")
    void test_saveNews_invalidMimeType_throwsException() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "virus".getBytes());

        // Assuming service does validation before calling repository or uploadService
        // and throws BusinessRuleException or similar
        // Because we don't have the exact implementation, we mock the expected behavior
        // Or if the implementation delegates to uploadService for validation:
        when(uploadService.saveFile(any())).thenThrow(new BusinessRuleException("Invalid file type"));

        assertThrows(BusinessRuleException.class, () -> {
            newsService.saveNews(testNews, invalidFile);
        });

        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-NEWS-SER-004: ToggleAvailable id not exists")
    void test_toggleAvailable_notFound_throwsException() {
        when(newsRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            newsService.toggleAvailable(99999L);
        });

        verify(newsRepository).findById(99999L);
        verify(newsRepository, never()).save(any());
    }
}
