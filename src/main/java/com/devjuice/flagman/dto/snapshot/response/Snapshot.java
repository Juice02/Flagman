package com.devjuice.flagman.dto.snapshot.response;

import com.devjuice.flagman.entity.Environment;

import java.util.List;

// Every flag of a project in one environment. Sent by GET .../snapshot and on the stream.
public record Snapshot(int projectId, Environment environment, long revision, List<FlagState> flags) {
}
