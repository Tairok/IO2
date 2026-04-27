package com.app.client.patterns.observers;

public final class TransferEvent {
    private final TransferAction action;
    private final TransferStage stage;
    private final String sender;
    private final String recipient;
    private final String filename;
    private final long transferredBytes;
    private final long totalBytes;
    private final String message;

    public TransferEvent(
            TransferAction action,
            TransferStage stage,
            String sender,
            String recipient,
            String filename,
            long transferredBytes,
            long totalBytes,
            String message
    ) {
        this.action = action;
        this.stage = stage;
        this.sender = sender;
        this.recipient = recipient;
        this.filename = filename;
        this.transferredBytes = transferredBytes;
        this.totalBytes = totalBytes;
        this.message = message;
    }

    public TransferAction getAction() { return action; }
    public TransferStage getStage() { return stage; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getFilename() { return filename; }
    public long getTransferredBytes() { return transferredBytes; }
    public long getTotalBytes() { return totalBytes; }
    public String getMessage() { return message; }
}
