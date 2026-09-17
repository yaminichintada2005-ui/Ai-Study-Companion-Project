package com.aiproof.studycompanion.service;

import com.aiproof.studycompanion.entity.Material;
import com.aiproof.studycompanion.entity.Project;
import com.aiproof.studycompanion.repository.MaterialRepository;
import com.aiproof.studycompanion.repository.ProjectRepository;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class MaterialUploadService {

    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;

    public MaterialUploadService(
            MaterialRepository materialRepository,
            ProjectRepository projectRepository,
            FileStorageService fileStorageService) {

        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
    }

    public Material uploadMaterial(
            Long projectId,
            MultipartFile file) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project not found with id: " + projectId
                        )
                );

        validateFile(file);

        String filePath =
                fileStorageService.storeFile(file);

        String fileName =
                file.getOriginalFilename() == null
                        ? "unknown-file"
                        : file.getOriginalFilename();

        String fileType =
                file.getContentType() == null
                        ? "application/octet-stream"
                        : file.getContentType();

        String content;

        try {
            content = extractText(file, fileName);
        } catch (Exception e) {

            fileStorageService.deleteFile(filePath);

            throw new RuntimeException(
                    "Could not extract text from file: " + fileName,
                    e
            );
        }

        Material material = Material.builder()
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(filePath)
                .status(Material.ProcessingStatus.READY)
                .content(content)
                .project(project)
                .build();

        return materialRepository.save(material);
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file"
            );
        }

        String fileName =
                file.getOriginalFilename() == null
                        ? ""
                        : file.getOriginalFilename().toLowerCase();

        if (!fileName.endsWith(".pdf")
                && !fileName.endsWith(".txt")) {

            throw new IllegalArgumentException(
                    "Only PDF and TXT files are supported"
            );
        }
    }

    private String extractText(
            MultipartFile file,
            String fileName) throws IOException {

        if (fileName.toLowerCase().endsWith(".txt")) {

            return new String(
                    file.getBytes(),
                    StandardCharsets.UTF_8
            );
        }

        if (fileName.toLowerCase().endsWith(".pdf")) {

            byte[] pdfBytes = file.getBytes();

            try (PDDocument document =
                         Loader.loadPDF(pdfBytes)) {

                PDFTextStripper stripper =
                        new PDFTextStripper();

                return stripper.getText(document);
            }
        }

        throw new IllegalArgumentException(
                "Unsupported file type"
        );
    }
}