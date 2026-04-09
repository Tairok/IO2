// com/capp/client/service/FileService.java
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



    // shareFile method removed
}
