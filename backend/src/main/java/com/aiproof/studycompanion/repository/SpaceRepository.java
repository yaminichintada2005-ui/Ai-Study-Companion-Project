package com.aiproof.studycompanion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aiproof.studycompanion.entity.Space;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByUserId(Long userId);

}