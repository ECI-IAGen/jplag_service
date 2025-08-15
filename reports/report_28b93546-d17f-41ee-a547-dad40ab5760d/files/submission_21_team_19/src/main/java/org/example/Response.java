package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Response {
    private final OutputStream out;
    
    public Response (OutputStream out){
        this.out = out;
    }

    public void send(String body) throws IOException{
        try {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                    "\r\n" + body;
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar la respuesta JSON: " + e.getMessage());
        }

    }

    public void sendJson(String jsonBody) throws IOException {
        try {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + jsonBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                    "\r\n" +
                    jsonBody;

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar la respuesta JSON: " + e.getMessage());
        }
    }
}
