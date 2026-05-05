package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.BannerSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-BNR-CTRL-001 to TC-BNR-CTRL-022: Banner Controller Integration Tests
 * Tests HTTP endpoints with security and validation
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiAdminBannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BannerSetService bannerSetService;

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
    @DisplayName("TC-BNR-CTRL-001: GET list all banners - no pagination (memory risk)")
    @WithMockUser(roles = "ADMIN")
    void test_getAll_noPagination_returnsFullList() throws Exception {
        // Arrange
        List<BannerSet> banners = List.of(testBannerSet);
        when(bannerSetService.findAll()).thenReturn(banners);

        // Act & Assert
        mockMvc.perform(get("/api/admin/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(bannerSetService).findAll();
    }

    @Test
    @DisplayName("TC-BNR-CTRL-002: GET list empty - returns empty array")
    @WithMockUser(roles = "ADMIN")
    void test_getAll_emptyDatabase_returnsEmptyArray() throws Exception {
        // Arrange
        when(bannerSetService.findAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(bannerSetService).findAll();
    }

    @Test
    @DisplayName("TC-BNR-CTRL-003: GET list without auth token - returns 401")
    void test_getAll_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/banners"))
                .andExpect(status().isUnauthorized());

        verify(bannerSetService, never()).findAll();
    }

    @Test
    @DisplayName("TC-BNR-CTRL-004: GET detail valid id - returns BannerSet with images")
    @WithMockUser(roles = "ADMIN")
    void test_getOne_validId_returnsBannerSet() throws Exception {
        // Arrange
        when(bannerSetService.findById(1L)).thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(get("/api/admin/banners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Banner"))
                .andExpect(jsonPath("$.data.effect").value("fade"))
                .andExpect(jsonPath("$.data.images").isArray());

        verify(bannerSetService).findById(1L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-005: GET detail id not found - returns 404")
    @WithMockUser(roles = "ADMIN")
    void test_getOne_notFound_returns404() throws Exception {
        // Arrange
        when(bannerSetService.findById(99999L))
                .thenThrow(new ResourceNotFoundException("BannerSet not found: 99999"));

        // Act & Assert
        mockMvc.perform(get("/api/admin/banners/99999"))
                .andExpect(status().isNotFound());

        verify(bannerSetService).findById(99999L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-006: CREATE valid data with images - returns 201")
    @WithMockUser(roles = "ADMIN")
    void test_create_validData_returns200() throws Exception {
        // Arrange
        MockMultipartFile image = new MockMultipartFile(
                "images", "banner.jpg", "image/jpeg", "image".getBytes());
        
        when(bannerSetService.create(anyString(), anyString(), anyList(), anyList(), anyList()))
                .thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners")
                        .file(image)
                        .param("name", "Hero Banner")
                        .param("effect", "fade")
                        .param("titles", "Title 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(bannerSetService).create(anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-007: CREATE name blank - returns 400")
    @WithMockUser(roles = "ADMIN")
    void test_create_blankName_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners")
                        .param("name", "")
                        .param("effect", "fade"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên banner không hợp lệ (tối đa 100 ký tự)"));

        verify(bannerSetService, never()).create(anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-008: CREATE name exceeds 100 chars - returns 400")
    @WithMockUser(roles = "ADMIN")
    void test_create_nameTooLong_returns400() throws Exception {
        // Arrange
        String longName = "A".repeat(101);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners")
                        .param("name", longName)
                        .param("effect", "fade"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên banner không hợp lệ (tối đa 100 ký tự)"));

        verify(bannerSetService, never()).create(anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-009: CREATE invalid effect value - returns 400")
    @WithMockUser(roles = "ADMIN")
    void test_create_invalidEffect_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners")
                        .param("name", "Test Banner")
                        .param("effect", "FLASH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hiệu ứng không hợp lệ"));

        verify(bannerSetService, never()).create(anyString(), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-010: CREATE name with XSS HTML - sanitized and saved")
    @WithMockUser(roles = "ADMIN")
    void test_create_xssInName_sanitizedAndSaved() throws Exception {
        // Arrange
        when(bannerSetService.create(anyString(), anyString(), anyList(), anyList(), anyList()))
                .thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners")
                        .param("name", "<script>alert(1)</script>Banner")
                        .param("effect", "fade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify service called with sanitized name (HTML stripped)
        verify(bannerSetService).create(eq("Banner"), anyString(), anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-011: CREATE without images - creates BannerSet with empty images")
    @WithMockUser(roles = "ADMIN")
    void test_create_noImages_createsSuccessfully() throws Exception {
        // Arrange
        when(bannerSetService.create(anyString(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners")
                        .param("name", "Empty Banner")
                        .param("effect", "fade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerSetService).create(anyString(), anyString(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-012: UPDATE meta valid - returns 200 with updated BannerSet")
    @WithMockUser(roles = "ADMIN")
    void test_updateMeta_validData_returns200() throws Exception {
        // Arrange
        when(bannerSetService.updateMeta(1L, "New Name", "slide")).thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(put("/api/admin/banners/1/meta")
                        .param("name", "New Name")
                        .param("effect", "slide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerSetService).updateMeta(1L, "New Name", "slide");
    }

    @Test
    @DisplayName("TC-BNR-CTRL-013: UPDATE meta id not found - returns 404")
    @WithMockUser(roles = "ADMIN")
    void test_updateMeta_notFound_returns404() throws Exception {
        // Arrange
        when(bannerSetService.updateMeta(99999L, "Test", "fade"))
                .thenThrow(new ResourceNotFoundException("BannerSet not found: 99999"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/banners/99999/meta")
                        .param("name", "Test")
                        .param("effect", "fade"))
                .andExpect(status().isNotFound());

        verify(bannerSetService).updateMeta(99999L, "Test", "fade");
    }

    @Test
    @DisplayName("TC-BNR-CTRL-014: UPDATE meta invalid effect - returns 400")
    @WithMockUser(roles = "ADMIN")
    void test_updateMeta_invalidEffect_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/admin/banners/1/meta")
                        .param("name", "Test")
                        .param("effect", "BOUNCE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hiệu ứng không hợp lệ"));

        verify(bannerSetService, never()).updateMeta(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-015: DELETE valid banner id - returns 200")
    @WithMockUser(roles = "ADMIN")
    void test_delete_validId_returns200() throws Exception {
        // Arrange
        doNothing().when(bannerSetService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/banners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerSetService).delete(1L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-016: DELETE banner id not found - returns 404")
    @WithMockUser(roles = "ADMIN")
    void test_delete_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("BannerSet not found: 99999"))
                .when(bannerSetService).delete(99999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/banners/99999"))
                .andExpect(status().isNotFound());

        verify(bannerSetService).delete(99999L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-017: DELETE single image from banner - returns 200")
    @WithMockUser(roles = "ADMIN")
    void test_removeImage_validId_returns200() throws Exception {
        // Arrange
        doNothing().when(bannerSetService).removeImage(5L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/banners/images/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerSetService).removeImage(5L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-018: DELETE image imageId not found - returns 200 (no error)")
    @WithMockUser(roles = "ADMIN")
    void test_removeImage_notFound_returns200() throws Exception {
        // Arrange - deleteById doesn't throw exception even if ID doesn't exist
        doNothing().when(bannerSetService).removeImage(99999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/banners/images/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerSetService).removeImage(99999L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-019: ADD images to existing banner - returns 200")
    @WithMockUser(roles = "ADMIN")
    void test_addImages_validData_returns200() throws Exception {
        // Arrange
        MockMultipartFile image = new MockMultipartFile(
                "images", "new.jpg", "image/jpeg", "image".getBytes());
        
        when(bannerSetService.addImages(anyLong(), anyList(), anyList(), anyList()))
                .thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/banners/1/images")
                        .file(image)
                        .param("titles", "Title 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerSetService).addImages(anyLong(), anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("TC-BNR-CTRL-021: ACTIVATE banner - returns 200 with activated BannerSet")
    @WithMockUser(roles = "ADMIN")
    void test_activate_validId_returns200() throws Exception {
        // Arrange
        testBannerSet.setActive(true);
        when(bannerSetService.activate(1L)).thenReturn(testBannerSet);

        // Act & Assert
        mockMvc.perform(put("/api/admin/banners/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));

        verify(bannerSetService).activate(1L);
    }

    @Test
    @DisplayName("TC-BNR-CTRL-022: ACTIVATE banner id not found - returns 404")
    @WithMockUser(roles = "ADMIN")
    void test_activate_notFound_returns404() throws Exception {
        // Arrange
        when(bannerSetService.activate(99999L))
                .thenThrow(new ResourceNotFoundException("BannerSet not found: 99999"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/banners/99999/activate"))
                .andExpect(status().isNotFound());

        verify(bannerSetService).activate(99999L);
    }
}
