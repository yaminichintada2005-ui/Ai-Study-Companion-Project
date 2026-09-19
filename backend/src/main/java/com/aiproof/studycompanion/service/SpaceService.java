package com.aiproof.studycompanion.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiproof.studycompanion.dto.SpaceRequest;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.entity.User;
import com.aiproof.studycompanion.repository.SpaceRepository;
import com.aiproof.studycompanion.repository.UserRepository;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;

    public SpaceService(
            SpaceRepository spaceRepository,
            UserRepository userRepository) {

        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
    }

    // Create a new Space for the logged-in user
    @Transactional
    public Space createSpace(SpaceRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Space space = Space.builder()
                .name(request.name())
                .description(request.description())
                .user(user)
                .build();

        return spaceRepository.save(space);
    }

    // Get all Spaces belonging to the logged-in user
    @Transactional(readOnly = true)
    public List<Space> getUserSpaces(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return spaceRepository.findByUserId(user.getId());
    }

    // Get a Space only if it belongs to the logged-in user
    @Transactional(readOnly = true)
    public Space getSpace(Long id, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return spaceRepository.findById(id)
                .filter(space ->
                        space.getUser().getId().equals(user.getId()))
                .orElseThrow(() ->
                        new RuntimeException("Space not found"));
    }

    // Update a Space belonging to the logged-in user
    @Transactional
    public Space updateSpace(
            Long id,
            SpaceRequest request,
            String email) {

        Space space = getSpace(id, email);

        space.setName(request.name());
        space.setDescription(request.description());

        return spaceRepository.save(space);
    }

    // Delete a Space belonging to the logged-in user
    @Transactional
    public void deleteSpace(Long id, String email) {

        Space space = getSpace(id, email);

        spaceRepository.delete(space);
    }
}