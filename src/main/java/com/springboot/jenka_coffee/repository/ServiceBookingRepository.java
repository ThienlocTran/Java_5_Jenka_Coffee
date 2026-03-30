package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {

    List<ServiceBooking> findByUsername(String username);

    List<ServiceBooking> findByStatus(String status);
}
