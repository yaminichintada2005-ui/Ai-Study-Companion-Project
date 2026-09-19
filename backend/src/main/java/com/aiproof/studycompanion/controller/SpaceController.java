package com.aiproof.studycompanion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiproof.studycompanion.dto.SpaceRequest;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.service.SpaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    // Create a new study space
    @PostMapping
    public ResponseEntity<Space> createSpace(
            @Valid @RequestBody SpaceRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        Space space =
                spaceService.createSpace(request, email);

        return ResponseEntity
                .status(201)
                .body(space);
    }

    // Get all spaces of logged-in user
    @GetMapping
    public ResponseEntity<List<Space>> getUserSpaces(
            Authentication authentication) {

        String email = authentication.getName();

        List<Space> spaces =
                spaceService.getUserSpaces(email);

        return ResponseEntity.ok(spaces);
    }

    // Get one space
    @GetMapping("/{id}")
    public ResponseEntity<Space> getSpace(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        Space space =
                spaceService.getSpace(id, email);

        return ResponseEntity.ok(space);
    }

    // Update a space
    @PutMapping("/{id}")
    public ResponseEntity<Space> updateSpace(
            @PathVariable Long id,
            @Valid @RequestBody SpaceRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        Space space =
                spaceService.updateSpace(
                        id,
                        request,
                        email
                );

        return ResponseEntity.ok(space);
    }

    // Delete a space
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpace(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        spaceService.deleteSpace(id, email);

        return ResponseEntity.noContent().build();
    }
}