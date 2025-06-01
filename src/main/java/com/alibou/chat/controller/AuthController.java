package com.alibou.chat.controller;

import com.alibou.chat.DTO.AuthRequest;
import com.alibou.chat.DTO.AuthResponse;
import com.alibou.chat.DTO.SignupRequest;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            // Find user by username
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            
            // Check if user exists and password matches
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // In a real application, use password encryption!
                if (user.getPassword().equals(request.getPassword())) {
                    // Login successful
                    AuthResponse response = new AuthResponse();
                    response.setId(user.getUsername());
                    response.setUsername(user.getUsername());
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            // Invalid credentials
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid username or password"));
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(null, null, "Server error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        try {
            // Check if username already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthResponse(null, null, "Username already in use"));
            }
            
            // Create new user
            User newUser = new User();
            newUser.setId(request.getUsername()); // Assuming ID is provided in request
            newUser.setUsername(request.getUsername());
            newUser.setPassword(request.getPassword()); // In production, encrypt this!
            System.out.println("New user created: " + newUser.getUsername());
            // Save user
            User savedUser = userRepository.save(newUser);
            
            // Create response
            AuthResponse response = new AuthResponse();
            response.setId(savedUser.getId());
            response.setUsername(savedUser.getUsername());
            response.setMessage("User registered successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(null, null, "Server error: " + e.getMessage()));
        }
    }
}