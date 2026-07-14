package com.example.vendingmachine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.vendingmachine.model.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}