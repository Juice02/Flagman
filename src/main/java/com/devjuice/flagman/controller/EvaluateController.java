package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.security.AccessInterceptor;
import com.devjuice.flagman.service.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EvaluateController {

    private final EvaluationService evaluationService;
    private final AccessInterceptor access;

    public EvaluateController(EvaluationService evaluationService, AccessInterceptor access) {
        this.evaluationService = evaluationService;
        this.access = access;
    }

    @PostMapping("/evaluate")
    public EvaluateResponse evaluate(@RequestBody EvaluateRequest request) {
        return evaluationService.evaluate(request);
    }

    // Several flags for one user in one call: {"dark-mode": {"enabled": true, "reason": "IN_ROLLOUT"}, ...}
    @PostMapping("/evaluate/flags")
    public Map<String, EvaluateResponse> evaluateFlags(@RequestBody EvaluateRequest request, HttpServletRequest http) {
        int flags = request.getFlagKeys() == null ? 0 : Math.min(request.getFlagKeys().size(), EvaluationService.MAX_FLAGS);
        access.countMoreEvaluations(http, flags - 1); // more than MAX_FLAGS is refused below anyway
        return evaluationService.evaluateFlags(request);
    }

}
