package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    Page<Contact> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByIsReadFalse();
    
    @Modifying
    @Query("UPDATE Contact c SET c.isRead = true WHERE c.isRead = false")
    void markAllAsRead();
}
