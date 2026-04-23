package com.springboot.jenka_coffee.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookingServiceImpl implements BookingService {

    private final ServiceBookingRepository bookingRepository;

    public BookingServiceImpl(ServiceBookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public ServiceBooking save(ServiceBooking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public Page<ServiceBooking> findAll(Pageable pageable) {
        return bookingRepository.findAll(pageable);
    }

    @Override
    public void updateStatus(Long id, String status) {
        ServiceBooking booking = bookingRepository.findById(id).orElse(null);
        if (booking != null) {
            booking.setStatus(status);
            bookingRepository.save(booking);
        }
    }
}
