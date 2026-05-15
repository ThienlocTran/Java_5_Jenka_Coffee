package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.*;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.UploadService;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BATCH 01-03 TOP 5 CRITICAL VULNERABILITY TESTS
 * Covers: RCE, DoS, Path Traversal, Privilege Escalation, Business Tampering
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Top 5 Critical Security & Business Integration Tests")
public class JenkaSecurityBusinessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;
    
    // FIX: Inject OrderDetailRepository for proper FK cleanup
    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UploadService uploadService; // Needed for TC-DATA-001

    private Product product;
    private Account customer;

    @BeforeEach
    void setUp() {
        // Delete in correct FK order to avoid constraint violations
        orderDetailRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();

        Category cat = new Category();
        cat.setId("CF");
        cat.setName("Coffee");
        categoryRepository.save(cat);

        product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("250000")); // 250k
        product.setCategory(cat);
        product.setAvailable(true);
        product.setCreateDate(LocalDateTime.now());
        product = productRepository.save(product);

        // TC-BIZ-001 FIX: Use FIXED username "customer" to match @WithMockUser(username = "customer").
        // Previously used random UUID → mismatch → checkout findById("customer") returns null → NPE → 500.
        // Safe to use fixed username because setUp() deleteAll() in correct FK order above.
        customer = new Account();
        customer.setUsername("customer"); // MUST match @WithMockUser(username = "customer")
        customer.setFullname("Test Customer");
        customer.setEmail("customer@test.com"); // fixed email
        customer.setPhone("0988888888"); // unique phone (fixed in earlier session)
        customer.setPasswordHash("hash");
        customer.setActivated(true);
        customer.setAdmin(false);
        customer = accountRepository.save(customer);

        // Cart is memory-based (CartService), not DB-based.
        // It will be populated inside the specific test via cartService.add()
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-SEC-001: Mass Assignment Attack Prevention (Admin Privilege Escalation)")
    void test_security_massAssignment_adminEscalation() throws Exception {
        // Attacker tries to inject "admin": true when creating an account
        // FIX TC-SEC-001: Add required field 'fullname' (AccountRequest @NotBlank) and fix JSON path to $.data.admin
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "hacked_admin")
                .param("fullname", "Hacker Test")  // REQUIRED field - was missing causing 400
                .param("email", "hacker@test.com")
                .param("password", "Pass@123")
                .param("admin", "true")) // ATTACK PAYLOAD
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.admin").value(false)); // FIX: path is $.data.admin not $.admin

        // Real DB Check
        Account dbUser = accountRepository.findById("hacked_admin").orElseThrow();
        assertFalse(dbUser.getAdmin(), "CRITICAL: Privilege Escalation Success! System accepted admin=true from body.");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-SEC-002: File Upload Path Traversal")
    void test_security_fileUpload_pathTraversal() throws Exception {
        // Filename contains path traversal characters
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "images", "../../etc/passwd.jpg", "image/jpeg", "fake content".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/products/" + product.getId() + "/images")
                .file(maliciousFile))
                .andExpect(status().isBadRequest()); // Validator should reject malicious filename
    }

    @Test
    @WithMockUser(username = "customer", roles = "USER")
    @DisplayName("TC-BIZ-001: Order Total Amount Tampering")
    void test_business_orderTotalTampering() throws Exception {
        // FIX: @WithMockUser username must match the actual customer username
        // But customer username is now dynamic (customer_<uuid>)
        // Solution: Use @WithMockUser with dynamic username is not possible
        // Workaround: Query order by customer object reference instead of username
        
        // Pre-condition: User has 2 items in cart
        cartService.add(product.getId()); // qty 1
        cartService.add(product.getId()); // qty 2 -> total = 500,000
        
        // Attacker tries to send a checkout request with modified totalAmount and prices
        // Note: Spring parses it to CheckoutRequest. If it ignores totalAmount, the DB calculation runs safely.
        Map<String, Object> maliciousPayload = new HashMap<>();
        maliciousPayload.put("fullname", "Test");
        maliciousPayload.put("email", customer.getEmail());  // Use actual customer email
        maliciousPayload.put("phone", customer.getPhone());  // Use actual customer phone
        maliciousPayload.put("address", "123 Street");
        maliciousPayload.put("province", "HCM");
        maliciousPayload.put("district", "Q1");
        maliciousPayload.put("ward", "P1");
        maliciousPayload.put("paymentMethod", "cod");
        maliciousPayload.put("agreeTerms", true);
        maliciousPayload.put("totalAmount", 1000); // 🚨 TAMPERED

        String responseJson = mockMvc.perform(post("/api/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maliciousPayload)))
                .andExpect(status().isOk()) // If checkout succeeds
                .andReturn().getResponse().getContentAsString();

        // FIX: Find order by customer account reference instead of username string
        Order order = orderRepository.findAll().stream()
                .filter(o -> o.getAccount() != null && o.getAccount().getUsername().equals(customer.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Order not found for customer: " + customer.getUsername()));
        
        // Assert that the order amount is 500,000 (calculated by server), NOT 1,000
        assertEquals(0, order.getTotalAmount().compareTo(new BigDecimal("500000")), 
            "CRITICAL: Order Total was Tampered! Order was saved with amount: " + order.getTotalAmount());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PERF-001: Pagination DoS Prevention (Memory Exhaustion Attack)")
    void test_performance_paginationDoS() throws Exception {
        // Request with size=999999
        mockMvc.perform(get("/api/admin/products?page=0&size=999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        // In a real load test, this asserts that the request didn't throw OutOfMemoryError and returned quickly
        // Because the controller should auto-cap size to 100 max
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-DATA-001: Transaction Rollback on Partial Failure (Product Image Upload)")
    void test_dataIntegrity_transactionRollbackOnImageFailure() throws Exception {
        long initialProductCount = productRepository.count();

        MockMultipartFile validImage = new MockMultipartFile(
                "imageFile", "valid.jpg", "image/jpeg", "image content".getBytes()
        );

        // Simulate Storage service failure AFTER product is inserted into DB
        when(uploadService.saveProductImage(any())).thenThrow(new RuntimeException("Cloudinary IO Exception"));

        mockMvc.perform(multipart("/api/admin/products")
                .file(validImage)
                .param("name", "Rolled Back Product")
                .param("price", "100000")
                .param("categoryId", "CF"))
                .andExpect(status().is5xxServerError()); // Should throw 500 due to storage failure

        // Verify that the product was completely ROLLED BACK and not saved to DB
        assertEquals(initialProductCount, productRepository.count(), 
            "CRITICAL: Data Inconsistency! Product was saved but image upload failed.");
        assertFalse(productRepository.existsBySlug("rolled-back-product"));
    }
}
