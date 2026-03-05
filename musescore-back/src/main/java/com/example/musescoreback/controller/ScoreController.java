package com.example.musescoreback.controller;

import com.example.musescoreback.dto.ScoreResponse;
import com.example.musescoreback.dto.ScoreUpsertRequest;
import com.example.musescoreback.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/my")
    public List<ScoreResponse> myScores(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return scoreService.myScores(authorization);
    }

    @GetMapping("/{id}")
    public ScoreResponse getById(@RequestHeader(name = "Authorization", required = false) String authorization,
                                 @PathVariable String id) {
        return scoreService.getById(authorization, id);
    }

    @PostMapping
    public ScoreResponse create(@RequestHeader(name = "Authorization", required = false) String authorization,
                                @Valid @RequestBody ScoreUpsertRequest request) {
        return scoreService.create(authorization, request);
    }

    @PutMapping("/{id}")
    public ScoreResponse update(@RequestHeader(name = "Authorization", required = false) String authorization,
                                @PathVariable String id,
                                @Valid @RequestBody ScoreUpsertRequest request) {
        return scoreService.update(authorization, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader(name = "Authorization", required = false) String authorization,
                       @PathVariable String id) {
        scoreService.delete(authorization, id);
    }
}
