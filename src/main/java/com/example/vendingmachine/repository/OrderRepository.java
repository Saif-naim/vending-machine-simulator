package com.example.vendingmachine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.vendingmachine.model.Order;

public interface OrderRepository
        extends JpaRepository<Order, Integer> {
}