package com.app.client.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TestPing {
    /**
     * Opens a simple connection to the local server,
     * sends "PING" and waits for "PONG".
     */
    public static boolean testPing(String host, int port) {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream  dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("PING");
            dos.flush();
            return "PONG".equals(dis.readUTF());
        } catch (IOException e) {
            AppLogger.error("Error during PING test", e);
            return false;
        }
    }
}
