package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    Booking save(Booking booking);
    Page<Booking> findAll(Pageable pageable);
    void updateStatus(Long id, Integer status);
}