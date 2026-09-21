package com.aiproof.studycompanion.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiproof.studycompanion.dto.LoginRequest;
import com.aiproof.studycompanion.dto.RegisterRequest;
import com.aiproof.studycompanion.entity.User;
import com.aiproof.studycompanion.security.JwtService;
import com.aiproof.studycompanion.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        return ResponseEntity.status(201).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtService.generateToken(
                request.getEmail()
        );

        // Record this login so the admin dashboard can show last-active time.
        authService.recordLogin(request.getEmail());

        User user = authService.getByEmail(request.getEmail());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Login successful",
                        "token", token,
                        "email", request.getEmail(),
                        "userId", user.getId(),
                        "role", user.getRole()
                )
        );
    }

    /**
     * Admin-only endpoint. Returns every registered user along with how many
     * spaces/projects/materials they have created, so an admin can see who
     * has joined and what they are doing.
     *
     * Access is restricted to users with role ADMIN in SecurityConfig
     * (requestMatchers("/api/auth/admin/**").hasRole("ADMIN")).
     */
    @GetMapping("/admin/users")
    public ResponseEntity<List<Map<String, Object>>> adminListUsers() {
        return ResponseEntity.ok(authService.getAllUsersForAdmin());
    }
}