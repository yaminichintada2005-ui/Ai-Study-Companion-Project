package com.aiproof.studycompanion.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiproof.studycompanion.entity.Material;
import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.entity.User;
import com.aiproof.studycompanion.repository.MaterialRepository;
import com.aiproof.studycompanion.repository.ProjectRepository;
import com.aiproof.studycompanion.repository.UserRepository;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {

        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // Create Material
    @Transactional
    public Material createMaterial(
            Long projectId,
            Material material,
            String email) {

        Project project = getUserProject(projectId, email);

        material.setProject(project);

        return materialRepository.save(material);
    }

    // Get all materials of a project
    @Transactional(readOnly = true)
    public List<Material> getMaterialsByProject(
            Long projectId,
            String email) {

        Project project = getUserProject(projectId, email);

        return materialRepository.findByProjectId(project.getId());
    }

    // Get one material
    @Transactional(readOnly = true)
    public Material getMaterialById(
            Long id,
            String email) {

        User user = getUser(email);

        return materialRepository.findById(id)
                .filter(material ->
                        material.getProject()
                                .getSpace()
                                .getUser()
                                .getId()
                                .equals(user.getId()))
                .orElseThrow(() ->
                        new RuntimeException(
                                "Material not found"));
    }

    // Update Material
    @Transactional
    public Material updateMaterial(
            Long id,
            Material updatedMaterial,
            String email) {

        Material existingMaterial =
                getMaterialById(id, email);

        existingMaterial.setFileName(
                updatedMaterial.getFileName());

        existingMaterial.setFileType(
                updatedMaterial.getFileType());

        existingMaterial.setFileSize(
                updatedMaterial.getFileSize());

        existingMaterial.setFilePath(
                updatedMaterial.getFilePath());

        existingMaterial.setStatus(
                updatedMaterial.getStatus());

        existingMaterial.setErrorMessage(
                updatedMaterial.getErrorMessage());

        existingMaterial.setContent(
                updatedMaterial.getContent());

        return materialRepository.save(existingMaterial);
    }

    // Delete Material
    @Transactional
    public void deleteMaterial(
            Long id,
            String email) {

        Material material =
                getMaterialById(id, email);

        materialRepository.delete(material);
    }

    // Find project only if it belongs to logged-in user
    private Project getUserProject(
            Long projectId,
            String email) {

        User user = getUser(email);

        return projectRepository.findById(projectId)
                .filter(project ->
                        project.getSpace()
                                .getUser()
                                .getId()
                                .equals(user.getId()))
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project not found"));
    }

    private User getUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));
    }
}