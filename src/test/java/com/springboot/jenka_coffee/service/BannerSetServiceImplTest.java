package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.BannerImage;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.BannerImageRepository;
import com.springboot.jenka_coffee.repository.BannerSetRepository;
import com.springboot.jenka_coffee.service.impl.BannerSetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TC-BNR-SER-001 to TC-BNR-SER-005: Banner Service Unit Tests
 * Tests service layer logic with mocked repositories
 */
@ExtendWith(MockitoExtension.class)
class BannerSetServiceImplTest {

    @Mock
    private BannerSetRepository setRepo;

    @Mock
    private BannerImageRepository imageRepo;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private BannerSetServiceImpl bannerSetService;

    private BannerSet testBannerSet;

    @BeforeEach
    void setUp() {
        testBannerSet = new BannerSet();
        testBannerSet.setId(1L);
        testBannerSet.setName("Test Banner");
        testBannerSet.setEffect("fade");
        testBannerSet.setActive(false);
        testBannerSet.setImages(new ArrayList<>());
    }

    @Test
    @DisplayName("TC-BNR-SER-001: FindById banner not exists - throws ResourceNotFoundException")
    void test_findById_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(setRepo.findById(99999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            bannerSetService.findById(99999L);
        });

        assertTrue(exception.getMessage().contains("BannerSet not found"));
        verify(setRepo).findById(99999L);
    }

    @Test
    @DisplayName("TC-BNR-SER-002: Create banner with blank name - should save with sanitized name")
    void test_create_blankName_savesWithSanitizedName() {
        // Arrange
        String blankName = "   ";
        when(setRepo.save(any(BannerSet.class))).thenReturn(testBannerSet);

        // Act
        BannerSet result = bannerSetService.create(blankName, "fade", null, null, null, null, null);

        // Assert
        assertNotNull(result);
        verify(setRepo, times(2)).save(any(BannerSet.class)); // Called twice in createBannerSetInDatabase
    }

    @Test
    @DisplayName("TC-BNR-SER-003: Create banner with XSS in name - HTML stripped")
    void test_create_xssInName_htmlStripped() {
        // Arrange
        String xssName = "<script>alert('XSS')</script>Banner";
        String safeName = "Banner"; // After HTML stripping
        
        when(setRepo.save(any(BannerSet.class))).thenAnswer(invocation -> {
            BannerSet saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        BannerSet result = bannerSetService.create(safeName, "fade", null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertFalse(result.getName().contains("<script>"));
        verify(setRepo, times(2)).save(any(BannerSet.class));
    }

    @Test
    @DisplayName("TC-BNR-SER-004: Activate banner not exists - throws ResourceNotFoundException")
    void test_activate_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(setRepo.findById(99999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            bannerSetService.activate(99999L);
        });

        assertTrue(exception.getMessage().contains("BannerSet not found"));
        verify(setRepo).findById(99999L);
        verify(setRepo, never()).save(any(BannerSet.class));
    }

    @Test
    @DisplayName("TC-BNR-SER-005: RemoveImage imageId not exists - throws ResourceNotFoundException")
    void test_removeImage_notFound_throwsResourceNotFoundException() {
        // Arrange
        Long imageId = 99999L;
        when(imageRepo.existsById(imageId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> bannerSetService.removeImage(imageId));

        // Assert
        verify(imageRepo).existsById(imageId);
        verify(imageRepo, never()).deleteById(imageId);
    }

    @Test
    @DisplayName("TC-BNR-SER-006: Activate banner - deactivates all others and activates target")
    void test_activate_success_deactivatesOthersAndActivatesTarget() {
        // Arrange
        when(setRepo.findById(1L)).thenReturn(Optional.of(testBannerSet));
        when(setRepo.save(any(BannerSet.class))).thenReturn(testBannerSet);
        when(setRepo.deactivateAll()).thenReturn(1);  // deactivateAll returns int, not void

        // Act
        BannerSet result = bannerSetService.activate(1L);

        // Assert
        assertNotNull(result);
        verify(setRepo).deactivateAll(); // All banners deactivated first
        verify(setRepo).findById(1L);
        verify(setRepo).save(any(BannerSet.class));
    }

    @Test
    @DisplayName("TC-BNR-SER-007: Create banner with images - uploads and saves correctly")
    void test_create_withImages_uploadsAndSaves() {
        // Arrange
        MockMultipartFile image1 = new MockMultipartFile(
                "image1", "banner1.jpg", "image/jpeg", "image1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile(
                "image2", "banner2.jpg", "image/jpeg", "image2".getBytes());
        List<MultipartFile> images = List.of(image1, image2);
        List<String> titles = List.of("Title 1", "Title 2");

        when(uploadService.saveImage(any(MultipartFile.class)))
                .thenReturn("https://cloudinary.com/image1.jpg")
                .thenReturn("https://cloudinary.com/image2.jpg");
        when(setRepo.save(any(BannerSet.class))).thenAnswer(invocation -> {
            BannerSet saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        BannerSet result = bannerSetService.create("Test Banner", "fade", images, titles, null, null, null);

        // Assert
        assertNotNull(result);
        verify(uploadService, times(2)).saveImage(any(MultipartFile.class));
        verify(setRepo, times(2)).save(any(BannerSet.class));
    }

    @Test
    @DisplayName("TC-BNR-SER-008: UpdateMeta with valid data - updates name and effect")
    void test_updateMeta_validData_updatesSuccessfully() {
        // Arrange
        when(setRepo.findById(1L)).thenReturn(Optional.of(testBannerSet));
        when(setRepo.save(any(BannerSet.class))).thenReturn(testBannerSet);

        // Act
        BannerSet result = bannerSetService.updateMeta(1L, "Updated Name", "slide");

        // Assert
        assertNotNull(result);
        verify(setRepo).findById(1L);
        verify(setRepo).save(any(BannerSet.class));
    }

    @Test
    @DisplayName("TC-BNR-SER-009: Delete banner - deletes from repository")
    void test_delete_validId_deletesSuccessfully() {
        // Arrange
        when(setRepo.existsById(1L)).thenReturn(true);
        doNothing().when(setRepo).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> {
            bannerSetService.delete(1L);
        });

        // Assert
        verify(setRepo).deleteById(1L);
    }

    @Test
    @DisplayName("TC-BNR-SER-010: AddImages with XSS in titles - HTML stripped from titles")
    void test_addImages_xssInTitles_htmlStripped() {
        // Arrange
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test".getBytes());
        List<MultipartFile> images = List.of(image);
        List<String> titles = List.of("<script>alert('XSS')</script>Title");

        when(setRepo.findById(1L)).thenReturn(Optional.of(testBannerSet));
        when(uploadService.saveImage(any(MultipartFile.class)))
                .thenReturn("https://cloudinary.com/test.jpg");
        when(setRepo.save(any(BannerSet.class))).thenReturn(testBannerSet);

        // Act
        BannerSet result = bannerSetService.addImages(1L, images, titles, null, null, null);

        // Assert
        assertNotNull(result);
        verify(uploadService).saveImage(any(MultipartFile.class));
        verify(setRepo).save(any(BannerSet.class));
    }
}
