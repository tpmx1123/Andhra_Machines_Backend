package com.example.machines.repository;

import com.example.machines.entity.Review;
import com.example.machines.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct(Product product);

    void deleteByProduct(Product product);
}


