package com.app.client.network;

import com.app.client.service.CommandService;

import java.io.IOException;

public class Client {

    private final NetworkConnection connection = new NetworkConnection();
    private final CommandService    service    = new CommandService(connection);

    public Client() { }

    /** Opens TCP connection and prepares command service */
    public void connect() throws IOException {
        connection.open();
    }

    /** Closes connection and sends QUIT */
    public void disconnect() throws IOException {
        connection.close();
    }

    /** Simple PING/PONG health-check */
    public boolean ping() throws IOException {
        connection.out().writeUTF("PING");
        connection.out().flush();
        return "PONG".equals(connection.in().readUTF());
    }

    /** Allows to get service with all commands */
    public CommandService getService() {
        return service;
    }

    public static CommandService connectAndGetService() throws IOException {
        Client c = new Client();
        c.connect();
        return c.getService();
    }
}
