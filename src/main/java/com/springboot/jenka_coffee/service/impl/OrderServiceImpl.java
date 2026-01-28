package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.service.OrderService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    final
    OrderRepository odao;

    public OrderServiceImpl(OrderRepository odao) {
        this.odao = odao;
    }

    @Override
    public Order create(Order order) {
        return odao.save(order);
    }

    @Override
    public Order findById(Long id) {
        return odao.findById(id).orElse(null);
    }

    @Override
    public List<Order> findByUsername(String username) {
        // Cách 1: Query trong DAO (List<Order> findByAccount_Username(String username))
        // return odao.findByAccount_Username(username);

        // Cách 2: Stream lọc (Nếu DAO chưa viết hàm tìm kiếm)
        return odao.findAll().stream()
                .filter(o -> o.getAccount().getUsername().equals(username))
                .collect(Collectors.toList());
    }
}