package com.example.musescoreback.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ScoreResponse(
        String id,
        String title,
        String composer,
        Integer tempo,
        JsonNode timeSignature,
        String keySignature,
        JsonNode notes,
        JsonNode measures,
        String createdAt,
        String updatedAt,
        Boolean isDraft,
        JsonNode tags,
        String description,
        String visibility
) {
}
