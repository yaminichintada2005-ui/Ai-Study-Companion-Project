package com.aiproof.studycompanion.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aiproof.studycompanion.dto.RegisterRequest;
import com.aiproof.studycompanion.entity.Material;
import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.entity.User;
import com.aiproof.studycompanion.repository.MaterialRepository;
import com.aiproof.studycompanion.repository.ProjectRepository;
import com.aiproof.studycompanion.repository.SpaceRepository;
import com.aiproof.studycompanion.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final ProjectRepository projectRepository;
    private final MaterialRepository materialRepository;
    private final PasswordEncoder passwordEncoder;

    /*
     * Email addresses listed in ADMIN_EMAILS will receive ADMIN role.
     *
     * Example:
     * ADMIN_EMAILS=your-email@gmail.com
     *
     * Everyone else will receive LEARNER role.
     */
    private final Set<String> adminEmails;

    public AuthService(
            UserRepository userRepository,
            SpaceRepository spaceRepository,
            ProjectRepository projectRepository,
            MaterialRepository materialRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-emails:}") String adminEmailsConfig) {

        this.userRepository = userRepository;
        this.spaceRepository = spaceRepository;
        this.projectRepository = projectRepository;
        this.materialRepository = materialRepository;
        this.passwordEncoder = passwordEncoder;

        this.adminEmails = Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /*
     * Register a new user.
     */
    public User register(RegisterRequest request) {

        // Check whether email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        /*
         * Only emails configured in ADMIN_EMAILS become ADMIN.
         * Everyone else becomes LEARNER.
         */
        String role = adminEmails.contains(
                request.email().trim().toLowerCase()
        ) ? "ADMIN" : "LEARNER";

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    /*
     * Find user by email.
     */
    public User getByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + email
                        )
                );
    }

    /*
     * Record the user's last successful login.
     */
    public void recordLogin(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {

            user.setLastLoginAt(LocalDateTime.now());

            userRepository.save(user);
        });
    }

    /*
     * Admin dashboard.
     *
     * Returns:
     * - User ID
     * - Name
     * - Email
     * - Role
     * - Registration date
     * - Last login
     * - Number of spaces
     * - Number of projects
     * - Number of materials
     */
    public List<Map<String, Object>> getAllUsersForAdmin() {

        List<User> users = userRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {

            List<Space> spaces =
                    spaceRepository.findByUserId(user.getId());

            int projectCount = 0;
            int materialCount = 0;

            for (Space space : spaces) {

                List<Project> projects =
                        projectRepository.findBySpaceId(space.getId());

                projectCount += projects.size();

                for (Project project : projects) {

                    List<Material> materials =
                            materialRepository.findByProjectId(
                                    project.getId()
                            );

                    materialCount += materials.size();
                }
            }

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put("id", user.getId());
            row.put("name", user.getName());
            row.put("email", user.getEmail());
            row.put("role", user.getRole());
            row.put("createdAt", user.getCreatedAt());
            row.put("lastLoginAt", user.getLastLoginAt());
            row.put("spaceCount", spaces.size());
            row.put("projectCount", projectCount);
            row.put("materialCount", materialCount);

            result.add(row);
        }

        /*
         * Newest registered users first.
         */
        result.sort((a, b) -> {

            LocalDateTime left =
                    (LocalDateTime) a.get("createdAt");

            LocalDateTime right =
                    (LocalDateTime) b.get("createdAt");

            if (left == null || right == null) {
                return 0;
            }

            return right.compareTo(left);
        });

        return result;
    }
}