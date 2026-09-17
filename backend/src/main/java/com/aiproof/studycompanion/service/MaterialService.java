package com.aiproof.studycompanion.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aiproof.studycompanion.entity.Material;
import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.repository.MaterialRepository;
import com.aiproof.studycompanion.repository.ProjectRepository;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            ProjectRepository projectRepository) {

        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
    }

    public Material createMaterial(
            Long projectId,
            Material material) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project not found with id: " + projectId));

        material.setProject(project);

        return materialRepository.save(material);
    }

    public List<Material> getMaterialsByProject(
            Long projectId) {

        return materialRepository.findByProjectId(projectId);
    }

    public Material getMaterialById(Long id) {

        return materialRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Material not found with id: " + id));
    }

    public Material updateMaterial(
            Long id,
            Material updatedMaterial) {

        Material existingMaterial =
                getMaterialById(id);

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

        return materialRepository.save(existingMaterial);
    }

    public void deleteMaterial(Long id) {

        Material material = getMaterialById(id);

        materialRepository.delete(material);
    }
}