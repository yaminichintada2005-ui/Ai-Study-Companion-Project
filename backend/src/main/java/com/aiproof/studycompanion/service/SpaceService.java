package com.aiproof.studycompanion.service;

import com.aiproof.studycompanion.dto.SpaceRequest;
import com.aiproof.studycompanion.entity.Space;
import com.aiproof.studycompanion.entity.User;
import com.aiproof.studycompanion.repository.SpaceRepository;
import com.aiproof.studycompanion.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;

    public SpaceService(
            SpaceRepository spaceRepository,
            UserRepository userRepository
    ) {
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
    }

    // Create a new Space
    public Space createSpace(SpaceRequest request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        Space space = Space.builder()
                .name(request.name())
                .description(request.description())
                .user(user)
                .build();

        return spaceRepository.save(space);
    }

    // Get all Spaces belonging to a user
    public List<Space> getUserSpaces(Long userId) {

        return spaceRepository.findByUserId(userId);
    }

    // Get a Space by ID
    public Space getSpace(Long id) {

        return spaceRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Space not found")
                );
    }

    // Update a Space
    public Space updateSpace(
            Long id,
            SpaceRequest request
    ) {

        Space space = getSpace(id);

        space.setName(request.name());
        space.setDescription(request.description());

        return spaceRepository.save(space);
    }

    // Delete a Space
    public void deleteSpace(Long id) {

        Space space = getSpace(id);

        spaceRepository.delete(space);
    }
}