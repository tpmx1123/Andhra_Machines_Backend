package com.example.machines.service;

import com.example.machines.entity.Cart;
import com.example.machines.entity.CartItem;
import com.example.machines.entity.Product;
import com.example.machines.entity.User;
import com.example.machines.repository.CartItemRepository;
import com.example.machines.repository.CartRepository;
import com.example.machines.repository.ProductRepository;
import com.example.machines.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    private static final int MAX_QUANTITY = 50;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Cart> existingCart = cartRepository.findByUser(user);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart addItemToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Apply scheduled price changes to get current price
        productService.applyScheduledPriceChangeForSchedule(product);
        product = productRepository.findById(productId).orElse(product);

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = Math.min(item.getQuantity() + quantity, MAX_QUANTITY);
            item.setQuantity(newQuantity);
            // Update prices
            item.setPrice(product.getPrice());
            item.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(Math.min(quantity, MAX_QUANTITY));
            newItem.setPrice(product.getPrice());
            newItem.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());
            newItem.setProductName(product.getTitle());
            newItem.setProductImage(product.getMainImageUrl() != null ? product.getMainImageUrl() : product.getImageUrl());
            newItem.setBrandName(product.getBrandName());
            newItem.setBrandSlug(product.getBrandSlug());
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        
        if (quantity < 1) {
            return removeItemFromCart(userId, productId);
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        int newQuantity = Math.min(quantity, MAX_QUANTITY);
        item.setQuantity(newQuantity);
        
        // Update prices from product
        Product product = productRepository.findById(productId).orElse(item.getProduct());
        productService.applyScheduledPriceChangeForSchedule(product);
        product = productRepository.findById(productId).orElse(product);
        item.setPrice(product.getPrice());
        item.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        
        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartItemRepository.deleteByCartId(cart.getId());
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }

    @Transactional
    public void syncCartPrices(Long userId) {
        Cart cart = getOrCreateCart(userId);
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId()).orElse(null);
            if (product != null) {
                productService.applyScheduledPriceChangeForSchedule(product);
                product = productRepository.findById(item.getProduct().getId()).orElse(product);
                item.setPrice(product.getPrice());
                item.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());
            }
        }
        cartRepository.save(cart);
    }

    /**
     * Sync cart prices for a specific product across all carts
     * Called when a product price changes (e.g., scheduled price expires)
     */
    @Transactional
    public void syncCartPricesForProduct(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
        
        // Apply scheduled price changes to get current price
        productService.applyScheduledPriceChangeForSchedule(product);
        product = productRepository.findById(productId).orElse(product);
        
        // Find all cart items with this product
        List<CartItem> cartItems = cartItemRepository.findByProductId(productId);
        
        for (CartItem item : cartItems) {
            item.setPrice(product.getPrice());
            item.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());
            cartItemRepository.save(item);
        }
        
        System.out.println("Synced cart prices for product " + productId + " in " + cartItems.size() + " cart(s)");
    }
}

