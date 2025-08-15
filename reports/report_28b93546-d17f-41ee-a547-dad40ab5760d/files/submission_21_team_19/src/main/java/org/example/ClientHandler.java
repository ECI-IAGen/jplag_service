package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ClientHandler  {
    private final Socket clientSocket;
    private static final String BASE_DIRECTORY = ClientHandler.class.getClassLoader().getResource("public").getPath();
    private static final Map<String, BiConsumer<Request, Response>> getRoutes = new HashMap<>();
    private static final Map<String, BiConsumer<Request, Response>> postRoutes = new HashMap<>();

    public ClientHandler(Socket clientSocket) {
        System.out.println("Base directory: " + BASE_DIRECTORY);
        this.clientSocket = clientSocket;
    }

    public static void get(String path, BiConsumer<Request, Response> handler) {
        getRoutes.put(path, handler);
    }

    public static void post(String path, BiConsumer<Request, Response> handler) {
        postRoutes.put(path, handler);
    }

    public void run() {
        try {
            OutputStream out = clientSocket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String requestLine = in.readLine();
            if (requestLine != null) {
                System.out.println("Solicitud: " + requestLine);
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length >= 2) {
                    String method = requestParts[0];
                    String file = requestParts[1];
                    System.out.println("El file es: " +  file);
                    URI requestFile = new URI(file);
                    String path = requestFile.getPath();
                    String query = requestFile.getQuery();
                    System.out.println("El query es: " +  query);

                    if ("GET".equals(method) && getRoutes.containsKey(path) && query == null){
                        Request resquest = new Request(method, path);
                        Response response = new Response(out);
                        getRoutes.get(path).accept(resquest,response);

                    } else if ("GET".equals(method) && getRoutes.containsKey(path) && query != null) {
                        Request request = new Request(method, path, null, query);
                        Response response = new Response(out);
                        getRoutes.get(path).accept(request, response);

                    } else if ("POST".equals(method) && postRoutes.containsKey(path))
                    {
                        System.out.println("Entro aqui alguna vez: " + path);
                        Request request = new Request(method, path, handleBody(in));
                        Response response = new Response(out);
                        postRoutes.get(path).accept(request, response);

                    } else if("POST".equals(method) && !postRoutes.containsKey(path))
                    {
                        handlePost(handleBody(in), out);
                    } else {
                        fileHandler(path, query, out);
                    }


                }
            }

        } catch (Exception e) {
            System.err.println("Error en la comunicación con el cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }

    public static Map<String, BiConsumer<Request, Response>> getRoutes() {
        return getRoutes;
    }

    private String handleBody(BufferedReader in) throws IOException {
        String body = "";
        String line;
        int contentLength = 0;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }

        char[] bodyChars = new char[contentLength];
        in.read(bodyChars, 0, contentLength);
        body = new String(bodyChars);

        return body;
    }

    private void handlePost(String body, OutputStream out) throws IOException {
        System.out.println("Contenido recibido por POST: " + body);

        // Por ahora solo enviamos una respuesta confirmando que recibimos los datos
        String responseBody = "{\"message\": \"Datos recibidos\", \"contenido\": \"" + body + "\"}";

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "\r\n" +
                responseBody;

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
    private static InputStream staticFiles(String path){
        InputStream inputStream = ClientHandler.class.getClassLoader().getResourceAsStream("public" + File.separator + path);
        return inputStream;
    }

    private static void fileHandler(String path, String query, OutputStream out) throws IOException {
        try {
            InputStream inputStream = staticFiles(path);

            if (inputStream != null) {
                byte[] fileBytes = inputStream.readAllBytes();
                String contentType = Files.probeContentType(Path.of(path));
                //System.out.println("Este es el path: " + path);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // Escribir los headers
                String headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + fileBytes.length + "\r\n" +
                        "\r\n";

                // Enviar headers y contenido por separado
                out.write(headers.getBytes(StandardCharsets.UTF_8));
                out.write(fileBytes);
                out.flush();
            } else {
                inputStream = ClientHandler.class.getClassLoader().getResourceAsStream("public" + File.separator + "404RemFound.html");
                byte[] fileBytes = inputStream.readAllBytes();
                String errorResponse = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "\r\n";
                out.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                out.write(fileBytes);
                out.flush();
            }
        } catch (IOException e) {
            String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                    "Content-Type: text/html\r\n" +
                    "\r\n" +
                    "<html><body><h1>500 - Error interno del servidor</h1></body></html>";
            out.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
    public static void startRoutes() throws IOException {
        get("/app/helloWord", (req, res) -> {
            try {
                res.send("Hello, world!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        get("/app/hello", (req, res) -> {
            String name = req.getValues("name");
            try {
                res.sendJson(name == null ? "Hola, visitante!" : "Hola, " + name + "!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        get("/app/pi", (req, res) -> {
            try {
                res.send(String.valueOf(Math.PI));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}