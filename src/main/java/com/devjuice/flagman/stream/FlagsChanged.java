package com.devjuice.flagman.stream;

// Published when a project's flags, configs or rules change. StreamHub handles it after the commit.
public record FlagsChanged(int projectId) {
}
