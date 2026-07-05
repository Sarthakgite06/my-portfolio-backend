package com.sarthak.portfolio.controller;

import com.sarthak.portfolio.model.ContactMessage;
import com.sarthak.portfolio.repository.ContactMessageRepository;
import com.sarthak.portfolio.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*")
public class ContactController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private NotificationService notificationService;

    // POST /api/contact - Submit new message
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitMessage(@RequestBody ContactMessage message) {
        Map<String, Object> response = new HashMap<>();
        
        if (message.getEmail() == null || message.getEmail().trim().isEmpty() ||
            message.getMessage() == null || message.getMessage().trim().isEmpty()) {
            response.put("error", "Email and message are required fields.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (message.getName() == null || message.getName().trim().isEmpty()) {
            message.setName("Anonymous");
        }
        if (message.getSubject() == null || message.getSubject().trim().isEmpty()) {
            message.setSubject("No Subject");
        }

        message.setTimestamp(LocalDateTime.now());
        ContactMessage saved = contactMessageRepository.save(message);

        // Trigger asynchronous email and SMS notifications
        try {
            notificationService.sendAlerts(saved);
        } catch (Exception e) {
            System.err.println("Notification trigger failed: " + e.getMessage());
        }

        response.put("success", true);
        response.put("message", "Message sent successfully!");
        response.put("data", saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/contact - Retrieve all messages (Admin Dashboard)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMessages() {
        Map<String, Object> response = new HashMap<>();
        List<ContactMessage> messages = contactMessageRepository.findAll();
        
        response.put("success", true);
        response.put("data", messages);
        
        return ResponseEntity.ok(response);
    }

    // DELETE /api/contact/{id} - Delete message
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        if (!contactMessageRepository.existsById(id)) {
            response.put("error", "Message not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        contactMessageRepository.deleteById(id);
        response.put("success", true);
        response.put("message", "Message deleted successfully.");
        
        return ResponseEntity.ok(response);
    }
}
