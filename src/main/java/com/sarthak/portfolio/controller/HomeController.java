package com.sarthak.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getHome() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "Online");
        response.put("message", "Sarthak Gite's Portfolio Spring Boot REST API is running successfully!");
        return ResponseEntity.ok(response);
    }
}
