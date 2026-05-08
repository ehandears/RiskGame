package com.mycompany.riskgameproject.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {

    public static final int DEFAULT_PORT = 5000;

    private final int port;
    private final RoomManager roomManager = new RoomManager();

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Risk server started on port " + port);
            while (true) {
                // Her oyuncu baglaninca ayri handler aciyorum, boylece iki client ayni anda oynayabiliyor.
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, roomManager);
                Thread thread = new Thread(handler, "client-handler");
                thread.start();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new GameServer(port).start();
    }
}
