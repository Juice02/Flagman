package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.service.EvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

}
