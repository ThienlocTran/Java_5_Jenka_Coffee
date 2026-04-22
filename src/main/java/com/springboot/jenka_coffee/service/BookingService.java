package com.springboot.jenka_coffee.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    ServiceBooking save(ServiceBooking booking);

    Page<ServiceBooking> findAll(Pageable pageable);

    void updateStatus(Long id, String status);
}