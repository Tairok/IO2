// src/main/java/com/capp/client/network/NetworkConnection.java
package com.app.client.network;

import com.app.client.Config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetworkConnection {

    private Socket sock;
    private DataInputStream  dis;
    private DataOutputStream dos;

    /** Must be called before any in()/out() use */
    public void open() throws IOException {
        // or your server host
        String host = Config.SERVER_HOST;
        // or your server port
        int port = Config.SERVER_PORT;
        this.sock = new Socket(host, port);
        this.dis  = new DataInputStream(sock.getInputStream());
        this.dos  = new DataOutputStream(sock.getOutputStream());
    }

    public DataInputStream in()  { return dis; }
    public DataOutputStream out(){ return dos; }

    public void close() throws IOException {
        if (dos != null) {
            dos.writeUTF("QUIT");
            dos.flush();
        }
        if (sock != null) {
            sock.close();
        }
    }
}
