package com.devjuice.flagman.sandbox;

import com.devjuice.flagman.dto.sandbox.response.SandboxResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SandboxController {

    private final SandboxService sandboxes;

    public SandboxController(SandboxService sandboxes) {
        this.sandboxes = sandboxes;
    }

    // Public, rate-limited per IP in AccessInterceptor, and capped in total in SandboxService.
    @PostMapping("/sandboxes")
    @ResponseStatus(HttpStatus.CREATED)
    public SandboxResponse create() {
        return sandboxes.create();
    }
}
