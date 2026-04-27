package com.app.client.patterns.observers;

@FunctionalInterface
public interface TransferObserver {
    void onTransferEvent(TransferEvent event);
}
