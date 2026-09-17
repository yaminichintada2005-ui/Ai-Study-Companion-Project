package com.aiproof.studycompanion.controller;

import com.aiproof.studycompanion.dto.SpaceRequest;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    // Create a Space
    @PostMapping
    public ResponseEntity<Space> createSpace(
            @Valid @RequestBody SpaceRequest request,
            @RequestParam Long userId
    ) {
        Space space = spaceService.createSpace(request, userId);

        return ResponseEntity.ok(space);
    }

    // Get all Spaces for a user
    @GetMapping
    public ResponseEntity<List<Space>> getUserSpaces(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                spaceService.getUserSpaces(userId)
        );
    }

    // Get Space by ID
    @GetMapping("/{id}")
    public ResponseEntity<Space> getSpace(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                spaceService.getSpace(id)
        );
    }

    // Update Space
    @PutMapping("/{id}")
    public ResponseEntity<Space> updateSpace(
            @PathVariable Long id,
            @Valid @RequestBody SpaceRequest request
    ) {
        return ResponseEntity.ok(
                spaceService.updateSpace(id, request)
        );
    }

    // Delete Space
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpace(
            @PathVariable Long id
    ) {
        spaceService.deleteSpace(id);

        return ResponseEntity.noContent().build();
    }
}