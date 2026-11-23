package com.devjuice.flagman.dto.sandbox.response;

import com.devjuice.flagman.entity.Environment;

// Returned once, when a sandbox is created. FlagMan keeps only a hash of the token.
public record SandboxResponse(int projectId, Environment environment, String token) {
}
