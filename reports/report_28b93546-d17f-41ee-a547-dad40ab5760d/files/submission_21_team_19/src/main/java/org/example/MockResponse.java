package org.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MockResponse extends Response{

    private final ByteArrayOutputStream outputStream  = new ByteArrayOutputStream();
    public MockResponse() {
        super(null);
    }

    @Override
    public void send(String data) throws IOException {
        outputStream.write(data.getBytes());
    }

    @Override
    public void sendJson(String data) throws IOException {
        outputStream.write(data.getBytes());
    }

    public String getBody(){
        return outputStream.toString();
    }
}
