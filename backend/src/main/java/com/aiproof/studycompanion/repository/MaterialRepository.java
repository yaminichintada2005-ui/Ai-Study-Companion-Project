package com.aiproof.studycompanion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiproof.studycompanion.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByProjectId(Long projectId);
}