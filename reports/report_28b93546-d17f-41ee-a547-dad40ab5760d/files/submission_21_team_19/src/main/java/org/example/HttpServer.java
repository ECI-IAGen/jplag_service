package org.example;

import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    static boolean running;
    static ServerSocket serverSocket = null;
    public static void main(String[] args) throws IOException, URISyntaxException {
        boolean running = true;


        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        //Esto sera una mausquerramienta misteriosa que usaremos mas tarde
        //ExecutorService threadPool = Executors.newFixedThreadPool(10);
        System.out.println(" Listo para recibir ...");

        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                //Esto es parte de la mausquerramienta xd
                //threadPool.execute(new ClientHandler(clientSocket));
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.startRoutes();
                clientHandler.run();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
        }
    }


    public static void startServer(){
        try {
            main(null);  // Llama al método main para iniciar el servidor
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void stopServer(){
        if (!running) {
            System.out.println("El servidor no está en ejecución.");
            return;
        }

        try {
            if (serverSocket != null) {
                serverSocket.close();
                System.out.println("Servidor detenido.");
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el servidor: " + e.getMessage());
        }
        running = false;
    }
}
