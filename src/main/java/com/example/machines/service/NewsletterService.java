package com.example.machines.service;

import com.example.machines.entity.NewsletterSubscriber;
import com.example.machines.repository.NewsletterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsletterService {

    @Autowired
    private NewsletterRepository newsletterRepository;

    public void subscribe(String email) {
        if (!newsletterRepository.existsByEmail(email)) {
            NewsletterSubscriber subscriber = new NewsletterSubscriber();
            subscriber.setEmail(email);
            newsletterRepository.save(subscriber);
        }
    }

    public List<NewsletterSubscriber> getAllSubscribers() {
        return newsletterRepository.findAll();
    }

    public void deleteSubscriber(Long id) {
        newsletterRepository.deleteById(id);
    }
}

