package com.app.client.service;

import com.app.client.model.FileEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

public class FileService {
    private final CommandService cmd;
    private final TransferService tx;

    public FileService(CommandService cmd, TransferService tx) {
        this.cmd = cmd;
        this.tx  = tx;
    }

    public List<FileEntry> listFiles(String user) throws IOException {
        return cmd.list(user);
    }

    public boolean deleteFile(String user, String name) throws IOException {
        return cmd.delete(user, name);
    }


    public boolean uploadFile(String user,
                              File file,
                              BiConsumer<Long,Long> progress) throws IOException {
        return tx.upload(user, file, progress);
    }

    public boolean downloadFile(String user,
                                String name,
                                File dest,
                                BiConsumer<Long,Long> progress) throws IOException {
        return tx.download(user, name, dest, progress);
    }



    private void share(String sender, String recipient, String filename, byte[] wrappedKey) throws IOException {
        boolean ok = cmd.share(sender, recipient, filename, wrappedKey);
        if (!ok) {
            throw new IOException("Share failed");
        }
    }

    public void shareFile(String sender, String recipient, String filename) throws IOException {
        share(sender, recipient, filename, new byte[0]);
    }
}
