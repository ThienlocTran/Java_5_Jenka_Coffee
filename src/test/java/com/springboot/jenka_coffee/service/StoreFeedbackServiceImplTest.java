package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.repository.StoreFeedbackRepository;
import com.springboot.jenka_coffee.service.impl.StoreFeedbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TC-FBK-SER-001 to TC-FBK-SER-002: Feedback Service Unit Tests
 * Tests service layer logic with mocked repositories
 */
@ExtendWith(MockitoExtension.class)
class StoreFeedbackServiceImplTest {

    @Mock
    private StoreFeedbackRepository feedbackRepository;

    @InjectMocks
    private StoreFeedbackServiceImpl feedbackService;

    private StoreFeedback testFeedback;

    @BeforeEach
    void setUp() {
        testFeedback = new StoreFeedback();
        testFeedback.setId(1L);
        testFeedback.setBranch("HN");
        testFeedback.setFullname("John Doe");
        testFeedback.setPhone("0123456789");
        testFeedback.setComment("Great service");
        testFeedback.setStoreRating(5);
        testFeedback.setStaffRating(5);
    }

    @Test
    @DisplayName("TC-FBK-SER-001: Delete feedback id not exists - throws BusinessRuleException")
    void test_delete_notFound_throwsBusinessRuleException() {
        // Arrange
        when(feedbackRepository.existsById(99999L)).thenReturn(false);

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            feedbackService.delete(99999L);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy đánh giá"));
        verify(feedbackRepository).existsById(99999L);
        verify(feedbackRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("TC-FBK-SER-002: FindByBranch valid branch - returns only that branch feedbacks")
    void test_findByBranch_validBranch_returnsOnlyThatBranch() {
        // Arrange
        StoreFeedback hanoiFeedback = new StoreFeedback();
        hanoiFeedback.setId(1L);
        hanoiFeedback.setBranch("HANOI");  // Service converts "Hanoi" → "HANOI" via toUpperCase()
        hanoiFeedback.setFullname("John");
        hanoiFeedback.setStoreRating(5);
        hanoiFeedback.setStaffRating(5);

        List<StoreFeedback> feedbacks = List.of(hanoiFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        
        when(feedbackRepository.findByBranchOrderByCreatedAtDesc(eq("HANOI"), any(Pageable.class)))
                .thenReturn(page);

        // Act
        Page<StoreFeedback> result = feedbackService.findByBranch("Hanoi", PageRequest.of(0, 20));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("HANOI", result.getContent().get(0).getBranch());
        verify(feedbackRepository).findByBranchOrderByCreatedAtDesc(eq("HANOI"), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-FBK-SER-003: Create feedback with XSS - sanitizes input")
    void test_create_xssInput_sanitizesData() {
        // Arrange
        StoreFeedbackRequest request = new StoreFeedbackRequest();
        request.setBranch("HN");
        request.setFullname("<script>alert('XSS')</script>John");
        request.setPhone("0123456789");
        request.setComment("<img src=x onerror=alert(1)>Great");
        request.setStoreRating(5);
        request.setStaffRating(5);

        when(feedbackRepository.save(any(StoreFeedback.class))).thenReturn(testFeedback);

        // Act
        StoreFeedback result = feedbackService.create(request);

        // Assert
        assertNotNull(result);
        verify(feedbackRepository).save(argThat(feedback -> 
            !feedback.getFullname().contains("<script>") &&
            !feedback.getComment().contains("<img")
        ));
    }

    @Test
    @DisplayName("TC-FBK-SER-004: Delete valid id - deletes successfully")
    void test_delete_validId_deletesSuccessfully() {
        // Arrange
        when(feedbackRepository.existsById(1L)).thenReturn(true);
        doNothing().when(feedbackRepository).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> {
            feedbackService.delete(1L);
        });

        // Assert
        verify(feedbackRepository).existsById(1L);
        verify(feedbackRepository).deleteById(1L);
    }

    @Test
    @DisplayName("TC-FBK-SER-005: FindAll with pagination - returns paginated results")
    void test_findAll_withPagination_returnsPaginatedResults() {
        // Arrange
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        // Act
        Page<StoreFeedback> result = feedbackService.findAll(PageRequest.of(0, 20));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(feedbackRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("TC-FBK-SER-006: Create feedback - branch converted to uppercase")
    void test_create_branchConvertedToUppercase() {
        // Arrange
        StoreFeedbackRequest request = new StoreFeedbackRequest();
        request.setBranch("hanoi");
        request.setFullname("John Doe");
        request.setPhone("0123456789");
        request.setComment("Good");
        request.setStoreRating(5);
        request.setStaffRating(5);

        when(feedbackRepository.save(any(StoreFeedback.class))).thenReturn(testFeedback);

        // Act
        feedbackService.create(request);

        // Assert
        verify(feedbackRepository).save(argThat(feedback -> 
            "HANOI".equals(feedback.getBranch())
        ));
    }

    @Test
    @DisplayName("TC-FBK-SER-007: FindByBranch case insensitive - converts to uppercase")
    void test_findByBranch_caseInsensitive_convertsToUppercase() {
        // Arrange
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackRepository.findByBranchOrderByCreatedAtDesc(eq("HCM"), any(Pageable.class)))
                .thenReturn(page);

        // Act
        feedbackService.findByBranch("hcm", PageRequest.of(0, 20));

        // Assert - service converts "hcm" to "HCM"
        verify(feedbackRepository).findByBranchOrderByCreatedAtDesc(eq("HCM"), any(Pageable.class));
    }
}
