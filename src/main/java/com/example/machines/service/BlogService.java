package com.example.machines.service;

import com.example.machines.dto.BlogRequest;
import com.example.machines.dto.BlogResponse;
import com.example.machines.entity.Blog;
import com.example.machines.entity.NewsletterSubscriber;
import com.example.machines.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private EmailService emailService;

    public List<BlogResponse> getAllBlogs() {
        return blogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public BlogResponse getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Blog not found with slug: " + slug));
        return convertToResponse(blog);
    }

    public BlogResponse createBlog(BlogRequest request) {
        if (blogRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new RuntimeException("Blog with this slug already exists");
        }

        Blog blog = new Blog();
        mapRequestToEntity(request, blog);
        blog = blogRepository.save(blog);

        // Notify subscribers
        List<NewsletterSubscriber> subscribers = newsletterService.getAllSubscribers();
        for (NewsletterSubscriber subscriber : subscribers) {
            emailService.sendNewBlogNotification(subscriber.getEmail(), blog.getTitle(), blog.getSlug());
        }

        return convertToResponse(blog);
    }

    public BlogResponse updateBlog(Long id, BlogRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found with id: " + id));

        // Check slug uniqueness if it changed
        if (!blog.getSlug().equals(request.getSlug()) && 
            blogRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new RuntimeException("Blog with this slug already exists");
        }

        mapRequestToEntity(request, blog);
        blog = blogRepository.save(blog);
        return convertToResponse(blog);
    }

    public void deleteBlog(Long id) {
        if (!blogRepository.existsById(id)) {
            throw new RuntimeException("Blog not found with id: " + id);
        }
        blogRepository.deleteById(id);
    }

    private void mapRequestToEntity(BlogRequest request, Blog blog) {
        blog.setTitle(request.getTitle());
        blog.setSlug(request.getSlug());
        blog.setExcerpt(request.getExcerpt());
        blog.setContent(request.getContent());
        blog.setImageUrl(request.getImageUrl());
        blog.setCategory(request.getCategory());
    }

    private BlogResponse convertToResponse(Blog blog) {
        return new BlogResponse(
                blog.getId(),
                blog.getTitle(),
                blog.getSlug(),
                blog.getExcerpt(),
                blog.getContent(),
                blog.getImageUrl(),
                blog.getCategory(),
                blog.getCreatedAt(),
                blog.getUpdatedAt()
        );
    }
}

