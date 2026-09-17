package com.aiproof.studycompanion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/space/{spaceId}")
    public ResponseEntity<Project> createProject(
            @PathVariable Long spaceId,
            @RequestBody Project project) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(projectService.createProject(spaceId, project));
    }

    @GetMapping("/space/{spaceId}")
    public ResponseEntity<List<Project>> getProjectsBySpace(
            @PathVariable Long spaceId) {

        return ResponseEntity.ok(
                projectService.getProjectsBySpace(spaceId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                projectService.getProjectById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @RequestBody Project project) {

        return ResponseEntity.ok(
                projectService.updateProject(id, project)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id) {

        projectService.deleteProject(id);

        return ResponseEntity.noContent().build();
    }
}