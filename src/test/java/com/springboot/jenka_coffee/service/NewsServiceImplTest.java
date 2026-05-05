package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
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

    @Mock
    private VercelWebhookService vercelWebhookService;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News testNews;

    @BeforeEach
    void setUp() {
        testNews = new News();
        testNews.setId(1);
        testNews.setTitle("Test Article");
        testNews.setContent("Content");
        testNews.setAvailable(false);
    }

    @Test
    @DisplayName("TC-NEWS-SER-001: FindById news id not exists")
    void test_findById_notFound_returnsNull() {
        // Service returns null when ID not found, doesn't throw exception
        when(newsRepository.findById(99999)).thenReturn(Optional.empty());

        News result = newsService.findById(99999);

        assertNull(result, "Should return null when news ID not found");
        verify(newsRepository).findById(99999);
    }

    @Test
    @DisplayName("TC-NEWS-SER-002: SaveNews with null image file (optional)")
    void test_saveNews_nullImage_savesSuccessfully() {
        when(newsRepository.save(any(News.class))).thenReturn(testNews);

        // saveNews returns void, not News
        newsService.saveNews(testNews, null);

        verify(uploadService, never()).saveNewsImage(any()); // Should not attempt to save file
        verify(newsRepository).save(testNews);
        verify(vercelWebhookService).triggerRebuild(); // Should trigger rebuild
    }

    @Test
    @DisplayName("TC-NEWS-SER-003: SaveNews with invalid image MIME type")
    void test_saveNews_invalidMimeType_throwsException() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "virus".getBytes());

        // uploadService.saveNewsImage() should throw exception for invalid file type
        when(uploadService.saveNewsImage(any(MultipartFile.class)))
                .thenThrow(new BusinessRuleException("Invalid file type"));

        assertThrows(BusinessRuleException.class, () -> {
            newsService.saveNews(testNews, invalidFile);
        });

        verify(newsRepository, never()).save(any());
        verify(vercelWebhookService, never()).triggerRebuild();
    }

    @Test
    @DisplayName("TC-NEWS-SER-004: ToggleAvailable id not exists")
    void test_toggleAvailable_notFound_doesNothing() {
        // toggleAvailable() uses Integer, not Long
        // Service doesn't throw exception, just does nothing when ID not found
        when(newsRepository.findById(99999)).thenReturn(Optional.empty());

        // Should not throw exception
        assertDoesNotThrow(() -> {
            newsService.toggleAvailable(99999);
        });

        verify(newsRepository).findById(99999);
        verify(newsRepository, never()).save(any());
        verify(vercelWebhookService, never()).triggerRebuild();
    }
}
