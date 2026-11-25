package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.service.EvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EvaluateController {

    private final EvaluationService evaluationService;

    public EvaluateController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    public EvaluateResponse evaluate(@RequestBody EvaluateRequest request) {
        return evaluationService.evaluate(request);
    }

    // Several flags for one user in one call: {"dark-mode": {"enabled": true, "reason": "IN_ROLLOUT"}, ...}
    @PostMapping("/evaluate/flags")
    public Map<String, EvaluateResponse> evaluateFlags(@RequestBody EvaluateRequest request) {
        return evaluationService.evaluateFlags(request);
    }

}
