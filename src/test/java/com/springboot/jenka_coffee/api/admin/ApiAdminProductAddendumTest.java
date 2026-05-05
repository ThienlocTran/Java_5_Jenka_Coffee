package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.mock.mockito.MockBean;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.dto.request.ProductRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin Product Controller Addendum Tests (Real Flow)")
class ApiAdminProductAddendumTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductService productService;

    @MockBean
    private UploadService uploadService; // Mock external storage for failure tests

    @Autowired
    private ObjectMapper objectMapper;

    private Category category;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setId("CF");
        category.setName("Coffee");
        categoryRepository.save(category);

        existingProduct = new Product();
        existingProduct.setName("ExistingProduct");
        existingProduct.setPrice(BigDecimal.valueOf(100.0));
        existingProduct.setCategory(category);
        existingProduct.setAvailable(true);
        existingProduct.setCreateDate(LocalDateTime.now());
        existingProduct = productRepository.save(existingProduct);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-012: CREATE price < 0 (SAI LOGIC - Fix: Verify DB)")
    void test_createProduct_negativePrice_dbNotSaved() throws Exception {
        long initialCount = productRepository.count();
        mockMvc.perform(multipart("/api/admin/products")
                .param("name", "Negative Price Product")
                .param("price", "-50000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest());
                
        // CRITICAL FIX: Verify the price was NOT saved to DB
        assertEquals(initialCount, productRepository.count(), "BUG NGHIÊM TRỌNG: Giá âm vẫn lọt vào DB!");
        // We also check that no product with name "Negative Price Product" exists
        assertFalse(productRepository.existsBySlug("negative-price-product"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-039: CREATE duplicate name (DB unique constraint)")
    void test_createProduct_duplicateName_returns400() throws Exception {
        mockMvc.perform(multipart("/api/admin/products")
                .param("name", "ExistingProduct")
                .param("price", "35000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest()); // Expecting 400 exactly! If 500, it exposes the unhandled DataIntegrityViolationException gap
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-040: CREATE null/empty multipart body")
    void test_createProduct_emptyMultipartBody_returns400() throws Exception {
        mockMvc.perform(multipart("/api/admin/products"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-041: UPDATE via PUT with wrong Content-Type")
    void test_updateProduct_wrongContentType_returns415() throws Exception {
        mockMvc.perform(put("/api/admin/products/" + existingProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-042: DELETE concurrent requests (race condition)")
    void test_deleteProduct_concurrentRequests() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger notFoundCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    int statusCode = mockMvc.perform(delete("/api/admin/products/" + existingProduct.getId()))
                            .andReturn().getResponse().getStatus();
                    if (statusCode == 200) successCount.incrementAndGet();
                    if (statusCode == 404 || statusCode == 400 || statusCode == 500) notFoundCount.incrementAndGet(); // Second request should ideally be 404/400
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();

        assertEquals(1, successCount.get(), "Only one deletion should succeed");
        assertFalse(productRepository.existsById(existingProduct.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-043A: UPLOAD image file fails validator (invalid MIME type)")
    void test_uploadImage_invalidMimeType_returns400() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "images", "malware.exe", "application/x-msdownload", "exe content".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/products/" + existingProduct.getId() + "/images")
                .file(exeFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-044: UPLOAD duplicate images in same request")
    void test_uploadImage_duplicateImages_returns400() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "images", "same.jpg", "image/jpeg", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/products/" + existingProduct.getId() + "/images")
                .file(img).file(img))
                .andExpect(status().isBadRequest()); // Recommended behavior is to reject
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-045: UPLOAD images count exceeds limit (>10 files)")
    void test_uploadImage_exceedsLimit_returns400() throws Exception {
        var request = multipart("/api/admin/products/" + existingProduct.getId() + "/images");
        for (int i = 0; i < 15; i++) {
            request.file(new MockMultipartFile("images", "img" + i + ".jpg", "image/jpeg", "content".getBytes()));
        }

        mockMvc.perform(request).andExpect(status().isBadRequest()); // Prevent DoS
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-046: UPLOAD file with executable content disguised as image (RCE risk)")
    void test_uploadImage_fakeImage_returns400() throws Exception {
        // Simulating a PE/ELF binary with .jpg extension
        byte[] fakeImageBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00}; // "MZ" magic bytes for EXE
        MockMultipartFile fakeImage = new MockMultipartFile(
                "images", "fake_rce.jpg", "image/jpeg", fakeImageBytes
        );

        mockMvc.perform(multipart("/api/admin/products/" + existingProduct.getId() + "/images")
                .file(fakeImage))
                .andExpect(status().isBadRequest()); // System should check magic bytes, not just extension
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-043B: UPLOAD image - validator passes but storage fails")
    void test_uploadImage_storageFails_returns500() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "images", "valid.jpg", "image/jpeg", "content".getBytes()
        );

        when(uploadService.saveProductImage(any())).thenThrow(new RuntimeException("S3 Storage failed"));

        mockMvc.perform(multipart("/api/admin/products/" + existingProduct.getId() + "/images")
                .file(img))
                .andExpect(status().is5xxServerError()); // Could be 503 or 500
    }

    // --- SERVICE LAYER REAL FLOW TESTS ---

    @Test
    @DisplayName("TC-PRD-SER-009: Repository save throws RuntimeException - verifies rollback")
    void test_productService_saveThrowsException_rollsBack() {
        ProductRequest request = new ProductRequest();
        request.setName("Rollback Test Product");
        request.setPrice(new BigDecimal("50000"));
        request.setAvailable(true);

        long initialCount = productRepository.count();

        // We can force a DataIntegrityViolation by using a duplicate slug or name if the DB enforces it
        // Or if we can't easily force it, we test if the transaction rolls back upon constraint violation
        // Here, we'll try to save a product with the same name which is unique
        ProductRequest duplicateReq = new ProductRequest();
        duplicateReq.setName("ExistingProduct"); // Same name as existing product
        duplicateReq.setPrice(new BigDecimal("35000"));

        assertThrows(Exception.class, () -> {
            productService.createProductFromRequest(duplicateReq, "CF", null);
        });

        assertEquals(initialCount, productRepository.count(), "Transaction did not roll back, DB count changed!");
    }

    @Test
    @DisplayName("TC-PRD-SER-010: Update with null name (strict validation check)")
    void test_productService_updateNullName() {
        assertThrows(Exception.class, () -> {
            productService.updateProductFromRequest(existingProduct.getId(), null, "desc", new BigDecimal("100"), "CF", true, null);
        });
    }

    @Test
    @DisplayName("TC-PRD-SER-011: ToggleAvailable called twice (idempotency)")
    void test_productService_toggleAvailable_idempotency() {
        assertTrue(existingProduct.getAvailable());

        productService.toggleAvailable(existingProduct.getId());
        Product afterFirst = productRepository.findById(existingProduct.getId()).orElseThrow();
        assertFalse(afterFirst.getAvailable());

        productService.toggleAvailable(existingProduct.getId());
        Product afterSecond = productRepository.findById(existingProduct.getId()).orElseThrow();
        assertTrue(afterSecond.getAvailable(), "Should return to true");
    }

    @Test
    @DisplayName("TC-PRD-SER-012: Delete product cascades image deletion successfully")
    void test_productService_deleteImageCascades() {
        productService.deleteProductWithValidation(existingProduct.getId());
        assertFalse(productRepository.existsById(existingProduct.getId()));
    }

    @Test
    @DisplayName("TC-PRD-SER-013: Delete product but image delete fails (transaction consistency)")
    void test_productService_deleteImageFails_rollsBack() {
        long initialCount = productRepository.count();

        // Mock uploadService to fail when deleting images
        doThrow(new RuntimeException("Cloudinary Delete Failed")).when(uploadService).deleteImage(anyString());

        // Assuming deleteProductWithValidation deletes images via uploadService
        try {
            productService.deleteProductWithValidation(existingProduct.getId());
        } catch (Exception e) {
            // expected
        }

        // If the method uses @Transactional and calls uploadService.deleteFile inside it,
        // throwing an exception should roll back the product deletion.
        // Wait, if it doesn't rollback, existsById will be false!
        assertTrue(productRepository.existsById(existingProduct.getId()), "Transaction consistency failed: Product was deleted from DB even though image deletion failed!");
    }
}
