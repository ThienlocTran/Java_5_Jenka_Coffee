package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Repository này sẽ được inject vào BookingServiceImpl của bạn
}