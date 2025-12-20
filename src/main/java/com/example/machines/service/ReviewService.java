package com.example.machines.service;

import com.example.machines.dto.ReviewRequest;
import com.example.machines.dto.ReviewResponse;
import com.example.machines.entity.Product;
import com.example.machines.entity.Review;
import com.example.machines.repository.ProductRepository;
import com.example.machines.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<ReviewResponse> getReviewsForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return reviewRepository.findByProduct(product).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReviewResponse addReview(Long productId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = new Review();
        review.setProduct(product);
        review.setUserName(request.getUserName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);

        recalculateRating(product);

        return toResponse(review);
    }

    public void deleteReview(Long productId, Long reviewId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getProduct().getId().equals(product.getId())) {
            throw new RuntimeException("Review does not belong to this product");
        }

        reviewRepository.delete(review);
        recalculateRating(product);
    }

    private void recalculateRating(Product product) {
        List<Review> reviews = reviewRepository.findByProduct(product);
        if (reviews.isEmpty()) {
            product.setRating(BigDecimal.ZERO);
            product.setReviewCount(0);
        } else {
            int sum = reviews.stream().mapToInt(Review::getRating).sum();
            int count = reviews.size();
            BigDecimal avg = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            product.setRating(avg);
            product.setReviewCount(count);
        }
        productRepository.save(product);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUserName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}


