package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.entity.Contact;
import com.springboot.jenka_coffee.repository.ContactRepository;
import com.springboot.jenka_coffee.service.impl.ContactServiceImpl;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TC-CON-SER-001 to TC-CON-SER-002: Contact Service Unit Tests
 * Tests service layer logic with mocked repositories
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ContactServiceImpl contactService;

    private Contact testContact;

    @BeforeEach
    void setUp() {
        testContact = new Contact();
        testContact.setId(1L);
        testContact.setFullName("John Doe");
        testContact.setEmail("john@example.com");
        testContact.setSubject("Test Subject");
        testContact.setMessage("Test Message");
        testContact.setIsRead(false);
    }

    @Test
    @DisplayName("TC-CON-SER-001: MarkAsRead contact id not exists - silent fail (no exception)")
    void test_markAsRead_notFound_silentFail() {
        // Arrange
        when(contactRepository.findById(99999L)).thenReturn(Optional.empty());

        // Act - should not throw exception
        assertDoesNotThrow(() -> {
            contactService.markAsRead(99999L);
        });

        // Assert
        verify(contactRepository).findById(99999L);
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    @DisplayName("TC-CON-SER-002: CountUnread returns correct number")
    void test_countUnread_returnsCorrectCount() {
        // Arrange
        when(contactRepository.countByIsReadFalse()).thenReturn(3L);

        // Act
        long count = contactService.countUnread();

        // Assert
        assertEquals(3L, count);
        verify(contactRepository).countByIsReadFalse();
    }

    @Test
    @DisplayName("TC-CON-SER-003: CountUnread with zero unread - returns 0")
    void test_countUnread_zeroUnread_returnsZero() {
        // Arrange
        when(contactRepository.countByIsReadFalse()).thenReturn(0L);

        // Act
        long count = contactService.countUnread();

        // Assert
        assertEquals(0L, count);
        verify(contactRepository).countByIsReadFalse();
    }

    @Test
    @DisplayName("TC-CON-SER-004: MarkAsRead valid id - updates isRead to true")
    void test_markAsRead_validId_updatesIsRead() {
        // Arrange
        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
        when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

        // Act
        contactService.markAsRead(1L);

        // Assert
        verify(contactRepository).findById(1L);
        verify(contactRepository).save(argThat(contact -> 
            contact.getIsRead() == true
        ));
    }

    @Test
    @DisplayName("TC-CON-SER-005: MarkAllAsRead - marks all contacts as read")
    void test_markAllAsRead_updatesAllContacts() {
        // Arrange
        doNothing().when(contactRepository).markAllAsRead();

        // Act
        contactService.markAllAsRead();

        // Assert
        verify(contactRepository).markAllAsRead();
    }

    @Test
    @DisplayName("TC-CON-SER-006: SendContactEmail with XSS - sanitizes input")
    void test_sendContactEmail_xssInput_sanitizesData() {
        // Arrange
        ContactRequest request = new ContactRequest();
        request.setFullName("<script>alert('XSS')</script>John");
        request.setEmail("john@example.com");
        request.setSubject("Test");
        request.setMessage("<img src=x onerror=alert(1)>Message");

        when(contactRepository.save(any(Contact.class))).thenReturn(testContact);
        doNothing().when(emailService).sendContactConfirmation(anyString(), anyString(), anyString());

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(contactRepository).save(argThat(contact -> 
            !contact.getFullName().contains("<script>") &&
            !contact.getMessage().contains("<img")
        ));
        verify(emailService).sendContactConfirmation(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-CON-SER-007: FindAll with pagination - returns paginated results")
    void test_findAll_withPagination_returnsPaginatedResults() {
        // Arrange
        List<Contact> contacts = List.of(testContact);
        Page<Contact> page = new PageImpl<>(contacts, PageRequest.of(0, 20), 1);
        when(contactRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        // Act
        Page<Contact> result = contactService.findAll(PageRequest.of(0, 20));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(contactRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("TC-CON-SER-008: SendContactEmail email service fails - saves contact anyway")
    void test_sendContactEmail_emailFails_stillSavesContact() {
        // Arrange
        ContactRequest request = new ContactRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setSubject("Test");
        request.setMessage("Message");

        when(contactRepository.save(any(Contact.class))).thenReturn(testContact);
        doThrow(new RuntimeException("Email service down"))
                .when(emailService).sendContactConfirmation(anyString(), anyString(), anyString());

        // Act - should not throw exception
        assertDoesNotThrow(() -> {
            contactService.sendContactEmail(request);
        });

        // Assert - contact still saved even if email fails
        verify(contactRepository).save(any(Contact.class));
        verify(emailService).sendContactConfirmation(anyString(), anyString(), anyString());
    }
}
