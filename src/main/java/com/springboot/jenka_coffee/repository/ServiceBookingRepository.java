package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {

    // Tìm tất cả booking theo username
    List<ServiceBooking> findByUsername(String username);

    // Tìm booking theo status
    List<ServiceBooking> findByStatus(String status);

    // Tìm booking theo username và status
    List<ServiceBooking> findByUsernameAndStatus(String username, String status);
}
