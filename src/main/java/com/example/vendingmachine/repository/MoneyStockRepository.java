package com.example.vendingmachine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.vendingmachine.model.MoneyStock;

public interface MoneyStockRepository
        extends JpaRepository<MoneyStock, Integer> {
}