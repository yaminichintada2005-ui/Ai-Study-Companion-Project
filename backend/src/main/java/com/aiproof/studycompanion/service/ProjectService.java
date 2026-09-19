package com.aiproof.studycompanion.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.entity.User;
import com.aiproof.studycompanion.repository.ProjectRepository;
import com.aiproof.studycompanion.repository.SpaceRepository;
import com.aiproof.studycompanion.repository.UserRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            SpaceRepository spaceRepository,
            UserRepository userRepository) {

        this.projectRepository = projectRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
    }

    // Create project inside user's space
    @Transactional
    public Project createProject(
            Long spaceId,
            Project project,
            String email) {

        User user = getUser(email);

        Space space = spaceRepository.findById(spaceId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() ->
                        new RuntimeException("Space not found"));

        project.setSpace(space);

        return projectRepository.save(project);
    }

    // Get all projects inside user's space
    @Transactional(readOnly = true)
    public List<Project> getProjectsBySpace(
            Long spaceId,
            String email) {

        User user = getUser(email);

        Space space = spaceRepository.findById(spaceId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() ->
                        new RuntimeException("Space not found"));

        return projectRepository.findBySpaceId(space.getId());
    }

    // Get project belonging to logged-in user
    @Transactional(readOnly = true)
    public Project getProjectById(
            Long id,
            String email) {

        User user = getUser(email);

        return projectRepository.findById(id)
                .filter(project ->
                        project.getSpace()
                                .getUser()
                                .getId()
                                .equals(user.getId()))
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project not found"));
    }

    // Update project
    @Transactional
    public Project updateProject(
            Long id,
            Project updatedProject,
            String email) {

        Project existingProject =
                getProjectById(id, email);

        existingProject.setName(
                updatedProject.getName());

        existingProject.setDescription(
                updatedProject.getDescription());

        existingProject.setLearningGoal(
                updatedProject.getLearningGoal());

        return projectRepository.save(existingProject);
    }

    // Delete project
    @Transactional
    public void deleteProject(
            Long id,
            String email) {

        Project project =
                getProjectById(id, email);

        projectRepository.delete(project);
    }

    private User getUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }
}