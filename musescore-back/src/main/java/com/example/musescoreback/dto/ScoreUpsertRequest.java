package com.example.musescoreback.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ScoreUpsertRequest(
        String title,
        String composer,
        Integer tempo,
        JsonNode timeSignature,
        String keySignature,
        JsonNode notes,
        JsonNode measures,
        Boolean isDraft,
        JsonNode tags,
        String description,
        String visibility
) {
}
