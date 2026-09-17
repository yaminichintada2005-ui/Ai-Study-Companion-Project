package com.aiproof.studycompanion.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.repository.ProjectRepository;
import com.aiproof.studycompanion.repository.SpaceRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SpaceRepository spaceRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            SpaceRepository spaceRepository) {

        this.projectRepository = projectRepository;
        this.spaceRepository = spaceRepository;
    }

    public Project createProject(Long spaceId, Project project) {

        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Space not found with id: " + spaceId));

        project.setSpace(space);

        return projectRepository.save(project);
    }

    public List<Project> getProjectsBySpace(Long spaceId) {

        return projectRepository.findBySpaceId(spaceId);
    }

    public Project getProjectById(Long id) {

        return projectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project not found with id: " + id));
    }

    public Project updateProject(
            Long id,
            Project updatedProject) {

        Project existingProject = getProjectById(id);

        existingProject.setName(
                updatedProject.getName());

        existingProject.setDescription(
                updatedProject.getDescription());

        existingProject.setLearningGoal(
                updatedProject.getLearningGoal());

        return projectRepository.save(existingProject);
    }

    public void deleteProject(Long id) {

        Project project = getProjectById(id);

        projectRepository.delete(project);
    }
}