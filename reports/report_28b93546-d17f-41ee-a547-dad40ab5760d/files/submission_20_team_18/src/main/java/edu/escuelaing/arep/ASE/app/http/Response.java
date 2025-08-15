package edu.escuelaing.arep.ASE.app.http;

import java.io.PrintWriter;

public class Response {
    private PrintWriter out;

    public Response(PrintWriter out) {
        this.out = out;
    }

    public void send(String message) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain");
        out.println();
        out.println(message);
    }
}
