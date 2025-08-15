package edu.escuelaing.arep.ASE.app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "src/main/resources/";
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(new ClientHandler(clientSocket, WEB_ROOT));
        }
    }
}

