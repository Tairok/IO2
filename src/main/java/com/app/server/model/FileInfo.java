package com.app.server.model;

import java.time.LocalDateTime;

public record FileInfo(String name, long size, LocalDateTime modified) {
}
