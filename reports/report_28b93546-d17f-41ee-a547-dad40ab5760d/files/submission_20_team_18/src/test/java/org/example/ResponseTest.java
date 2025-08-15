package org.example;

import edu.escuelaing.arep.ASE.app.http.Response;
import org.junit.jupiter.api.Test;


import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class ResponseTest {

    @Test
    public void testSend() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true);
        Response response = new Response(writer);

        response.send("Test Message");

        String output = baos.toString();
        assertTrue(output.contains("HTTP/1.1 200 OK"));
        assertTrue(output.contains("Content-Type: text/plain"));
        assertTrue(output.contains("Test Message"));
    }
}
