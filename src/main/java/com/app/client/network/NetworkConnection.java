package com.app.client.network;

/**
 * Client-side network connection to the server.
 *
 * <p>After {@link #open()} the connection exposes {@link java.io.DataInputStream} and
 * {@link java.io.DataOutputStream} for protocol communication.
 */

import com.app.client.Config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetworkConnection {

    private Socket sock;
    private DataInputStream  dis;
    private DataOutputStream dos;

    /**
     * Opens a TCP socket to the server using host/port from {@link Config}.
     *
     * @throws IOException when the socket cannot be opened
     */
    public void open() throws IOException {
        String host = Config.SERVER_HOST;
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
