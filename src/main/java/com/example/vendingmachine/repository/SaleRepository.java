package com.example.vendingmachine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.vendingmachine.model.Sale;

public interface SaleRepository extends JpaRepository<Sale, Integer> {
}