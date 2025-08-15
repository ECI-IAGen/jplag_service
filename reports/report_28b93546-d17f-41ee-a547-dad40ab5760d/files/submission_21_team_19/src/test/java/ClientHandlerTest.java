import org.example.*;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

public class ClientHandlerTest {

    private Thread serverThread;
    @Test
    public void setUp() throws IOException {
        // Iniciar el servidor en un hilo separado
       try {
            serverThread = new Thread(() -> HttpServer.startServer());
            serverThread.start();

            // Esperar un poco para asegurarnos de que el servidor se inicie
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        // Detener el servidor después de cada prueba
        HttpServer.stopServer();
    }

    @Test
    public void testGetRequest() throws IOException {
        // Conecta al servidor
        try (Socket socket = new Socket("localhost", 35000)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Enviar solicitud GET
            out.write("GET /index.html HTTP/1.1\r\n".getBytes());
            out.write("Host: localhost\r\n".getBytes());
            out.write("\r\n".getBytes());
            out.flush();

            // Leer la respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            assertTrue(responseLine.contains("200 OK")); // Verificar que la respuesta contiene "200 OK"
        }
    }

    @Test
    public void testPostRequest() throws IOException {
        // Conecta al servidor
        try (Socket socket = new Socket("localhost", 35000)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Enviar solicitud POST
            String body = "name=John&age=30";
            out.write("POST /submit HTTP/1.1\r\n".getBytes());
            out.write("Host: localhost\r\n".getBytes());
            out.write("Content-Type: application/x-www-form-urlencoded\r\n".getBytes());
            out.write(("Content-Length: " + body.length() + "\r\n").getBytes());
            out.write("\r\n".getBytes());
            out.write(body.getBytes());
            out.flush();

            // Leer la respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            assertTrue(responseLine.contains("200 OK")); // Verificar que la respuesta contiene "200 OK"
        }
    }

    @Test
    public void testFileNotFound() throws IOException {
        // Conecta al servidor
        try (Socket socket = new Socket("localhost", 35000)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Enviar solicitud GET para un archivo inexistente
            out.write("GET /notfound.html HTTP/1.1\r\n".getBytes());
            out.write("Host: localhost\r\n".getBytes());
            out.write("\r\n".getBytes());
            out.flush();

            // Leer la respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            assertTrue(responseLine.contains("404 Not Found")); // Verificar que contiene "404 Not Found"
        }
    }
    @Test
    void testGetRoutesInitialization() throws IOException {
        ClientHandler.startRoutes();
        Map<String, BiConsumer<Request, Response>> routes = ClientHandler.getRoutes();

        assertTrue(routes.containsKey("/app/helloWord"));
        assertTrue(routes.containsKey("/app/hello"));
        assertTrue(routes.containsKey("/app/pi"));
    }

    @Test
    public void testHelloWorldRoute() throws IOException {
        ClientHandler.startRoutes();
        Request req = new Request("GET", "/app/helloWord");

        MockResponse res = new MockResponse();
        ClientHandler.getRoutes().get("/app/helloWord").accept(req, res);
        System.out.println(ClientHandler.getRoutes().keySet());
        System.out.println("este es el body: " + res.getBody());
        assertEquals("Hello, world!", res.getBody());
    }

    @Test
    public void testHelloPiRoute() throws IOException {
        ClientHandler.startRoutes();
        Request req = new Request("GET", "/app/app/pi");

        MockResponse res = new MockResponse();
        ClientHandler.getRoutes().get("/app/pi").accept(req, res);
        System.out.println(ClientHandler.getRoutes().keySet());
        System.out.println("este es el body: " + res.getBody());
        assertEquals("3.141592653589793", res.getBody());
    }

    @Test
    public void testHelloNameRoute() throws IOException {
        ClientHandler.startRoutes();

        // Crear la solicitud con parámetros de consulta correctos
        Request req = new Request("GET", "/app/hello", null, "Andres");
        req.setQueryParams("name", "Andres"); // Parámetro de consulta adecuado
        System.out.println("esta es la query: " + req.getValues("name"));

        MockResponse res = new MockResponse();

        // Usar la clave correcta para buscar la ruta en el mapa
        String routeKey = "/app/hello";
        assertNotNull(ClientHandler.getRoutes().get(routeKey), "La ruta no está registrada");

        ClientHandler.getRoutes().get(routeKey).accept(req, res);

        System.out.println(ClientHandler.getRoutes().keySet());
        System.out.println("Este es el body: " + res.getBody());

        assertEquals("Hola, Andres!", res.getBody()); // Ajuste del mensaje esperado
    }


}
