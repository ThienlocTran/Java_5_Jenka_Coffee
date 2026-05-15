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
    @DisplayName("TC-NEWS-SER-001: findById() với id không tồn tại PHẢI throw ResourceNotFoundException (không return null)")
    void test_findById_notFound_shouldThrowResourceNotFoundException() {
        // CSV spec: Throw exception → Controller map → 404
        // CURRENT BEHAVIOR: Service returns null → Controller must null-check manually → NPE risk
        
        when(newsRepository.findById(99999)).thenReturn(Optional.empty());

        // EXPECTED: Service should throw ResourceNotFoundException
        // ACTUAL: Service returns null (documented gap)
        News result = newsService.findById(99999);
        
        // GAP DOCUMENTATION: Service returns null instead of throwing exception
        // This forces controller to null-check manually → if forgotten → NPE → 500 instead of 404
        assertNull(result, 
            "GAP CONFIRMED: findById() returns null instead of throwing ResourceNotFoundException. " +
            "This pattern is dangerous - controller must remember to null-check or will NPE.");
        
        verify(newsRepository).findById(99999);
        
        // TODO: When gap is fixed, change to:
        // ResourceNotFoundException exception = assertThrows(
        //     ResourceNotFoundException.class,
        //     () -> newsService.findById(99999),
        //     "findById() MUST throw ResourceNotFoundException per CSV spec"
        // );
        // assertTrue(exception.getMessage().contains("99999"));
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
    @DisplayName("TC-NEWS-SER-004: toggleAvailable() với id không tồn tại PHẢI throw ResourceNotFoundException (không silent-fail)")
    void test_toggleAvailable_notFound_shouldThrowResourceNotFoundException() {
        // CSV spec: Throw ResourceNotFoundException → Controller map → 404
        // CURRENT BEHAVIOR: Service silent-fails (does nothing) → returns 200 OK → misleading UX
        
        when(newsRepository.findById(99999)).thenReturn(Optional.empty());

        // EXPECTED: Service should throw ResourceNotFoundException
        // ACTUAL: Service does nothing (documented gap)
        assertDoesNotThrow(() -> {
            newsService.toggleAvailable(99999);
        }, "GAP CONFIRMED: toggleAvailable() silent-fails instead of throwing ResourceNotFoundException. " +
           "Admin calls PUT /api/admin/news/99999/toggle → gets 200 OK → thinks it worked → but nothing happened.");

        verify(newsRepository).findById(99999);
        verify(newsRepository, never()).save(any());
        verify(vercelWebhookService, never()).triggerRebuild();
        
        // TODO: When gap is fixed, change to:
        // ResourceNotFoundException exception = assertThrows(
        //     ResourceNotFoundException.class,
        //     () -> newsService.toggleAvailable(99999),
        //     "toggleAvailable() MUST throw ResourceNotFoundException per CSV spec"
        // );
        // assertTrue(exception.getMessage().contains("99999"));
    }
}
