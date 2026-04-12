package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * VULN-VOUCHER-SIPHON FIX: Track voucher usage per user
 * Ngăn 1 user sử dụng cùng 1 voucher nhiều lần
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "VoucherUsage", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"voucher_code", "username"}))
public class VoucherUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "voucher_code", nullable = false, length = 20)
    private String voucherCode;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_code", insertable = false, updatable = false)
    private Voucher voucher;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", insertable = false, updatable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
}
