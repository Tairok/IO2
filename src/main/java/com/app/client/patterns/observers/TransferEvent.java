package com.app.client.patterns.observers;

public record TransferEvent(TransferAction action, TransferStage stage, String sender, String recipient,
                            String filename, long transferredBytes, long totalBytes, String message) {
}
