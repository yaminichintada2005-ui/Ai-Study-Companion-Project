package com.aiproof.studycompanion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.aiproof.studycompanion.entity.Material;
import com.aiproof.studycompanion.service.MaterialService;
import com.aiproof.studycompanion.service.MaterialUploadService;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialUploadService materialUploadService;

    public MaterialController(
            MaterialService materialService,
            MaterialUploadService materialUploadService) {

        this.materialService = materialService;
        this.materialUploadService = materialUploadService;
    }

    /*
     * Upload PDF or TXT study material.
     *
     * File is stored, text is extracted,
     * and the Material record is saved.
     */
    @PostMapping(
            value = "/project/{projectId}/upload",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<Material> uploadMaterial(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {

        Material material =
                materialUploadService.uploadMaterial(
                        projectId,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(material);
    }

    /*
     * Create Material record manually.
     */
    @PostMapping("/project/{projectId}")
    public ResponseEntity<Material> createMaterial(
            @PathVariable Long projectId,
            @RequestBody Material material) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        materialService.createMaterial(
                                projectId,
                                material
                        )
                );
    }

    /*
     * Get all materials belonging to a project.
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Material>> getMaterials(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                materialService.getMaterialsByProject(
                        projectId
                )
        );
    }

    /*
     * Get a single material.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Material> getMaterial(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                materialService.getMaterialById(id)
        );
    }

    /*
     * Update material metadata.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Material> updateMaterial(
            @PathVariable Long id,
            @RequestBody Material material) {

        return ResponseEntity.ok(
                materialService.updateMaterial(
                        id,
                        material
                )
        );
    }

    /*
     * Delete material.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long id) {

        materialService.deleteMaterial(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}