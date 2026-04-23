package com.app.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShareService {
    private final CommandService commandService;

    public ShareService(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Walidacja nazwy odbiorcy
     */
    public boolean validateRecipient(String recipient) {
        return recipient != null && !recipient.trim().isEmpty();
    }

    /**
     * Walidacja listy plików
     */
    public boolean validateFiles(List<String> filenames) {
        return filenames != null && !filenames.isEmpty();
    }

    /**
     * Główna metoda udostępniająca pliki.
     * Zwraca listę nazw plików, które zostały pomyślnie udostępnione.
     */
    public List<String> shareFiles(String sender, String recipient, List<String> filenames) throws IOException {
        if (!validateRecipient(recipient)) {
            throw new IllegalArgumentException("Wprowadzono niepoprawną nazwę odbiorcy.");
        }
        if (!validateFiles(filenames)) {
            throw new IllegalArgumentException("Nie wybrano żadnych plików do udostępnienia.");
        }

        List<String> successfullyShared = new ArrayList<>();

        for (String filename : filenames) {
            boolean ok = commandService.share(sender, recipient, filename);
            if (ok) {
                successfullyShared.add(filename);
            }
        }

        return successfullyShared;
    }
}