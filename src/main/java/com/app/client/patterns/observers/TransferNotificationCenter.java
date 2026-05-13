package com.app.client.patterns.observers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject (Observable) for transfer events.
 * Thread-safe: notifications may happen from background transfer threads.
 */
public final class TransferNotificationCenter {
    private static final TransferNotificationCenter INSTANCE = new TransferNotificationCenter();

    private final List<TransferObserver> observers = new CopyOnWriteArrayList<>();

    private TransferNotificationCenter() {}

    public static TransferNotificationCenter getInstance() {
        return INSTANCE;
    }

    public void addObserver(TransferObserver observer) {
        if (observer != null) observers.add(observer);
    }

    public void removeObserver(TransferObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers about a transfer event.
     *
     * <p>Observer failures are ignored to avoid breaking transfers.
     *
     * @param event event to deliver
     */
    public void notify(TransferEvent event) {
        com.app.client.utils.AppLogger.info(String.format(
            "[Observer] action=%s, stage=%s, sender=%s, recipient=%s, file=%s, transferred=%d/%d, msg=%s",
            event.action(),
            event.stage(),
            event.sender(),
            event.recipient(),
            event.filename(),
            event.transferredBytes(),
            event.totalBytes(),
            event.message()
        ));
        for (TransferObserver o : observers) {
            try {
                o.onTransferEvent(event);
            } catch (Exception ignored) {
            }
        }
    }
}
