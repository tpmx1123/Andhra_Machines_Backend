package com.example.machines.repository;

import com.example.machines.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsActiveTrue();
    List<Product> findByInStockTrue();
    java.util.Optional<Product> findByBrandSlug(String brandSlug);
}

