package com.aiproof.studycompanion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // Create project inside a space
    @PostMapping("/space/{spaceId}")
    public ResponseEntity<Project> createProject(
            @PathVariable Long spaceId,
            @RequestBody Project project,
            Authentication authentication) {

        String email = authentication.getName();

        Project createdProject =
                projectService.createProject(
                        spaceId,
                        project,
                        email
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProject);
    }

    // Get projects inside a space
    @GetMapping("/space/{spaceId}")
    public ResponseEntity<List<Project>> getProjectsBySpace(
            @PathVariable Long spaceId,
            Authentication authentication) {

        String email = authentication.getName();

        List<Project> projects =
                projectService.getProjectsBySpace(
                        spaceId,
                        email
                );

        return ResponseEntity.ok(projects);
    }

    // Get one project
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        Project project =
                projectService.getProjectById(
                        id,
                        email
                );

        return ResponseEntity.ok(project);
    }

    // Update project
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @RequestBody Project project,
            Authentication authentication) {

        String email = authentication.getName();

        Project updatedProject =
                projectService.updateProject(
                        id,
                        project,
                        email
                );

        return ResponseEntity.ok(updatedProject);
    }

    // Delete project
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        projectService.deleteProject(
                id,
                email
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}