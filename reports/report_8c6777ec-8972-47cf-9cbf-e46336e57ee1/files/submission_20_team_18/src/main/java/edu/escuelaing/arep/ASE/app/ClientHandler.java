package edu.escuelaing.arep.ASE.app;

import edu.escuelaing.arep.ASE.app.http.Request;
import edu.escuelaing.arep.ASE.app.http.Response;
import edu.escuelaing.arep.ASE.app.routes.Route;
import edu.escuelaing.arep.ASE.app.routes.RouteManager;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String webRoot;

    public ClientHandler(Socket socket, String webRoot) {
        this.clientSocket = socket;
        this.webRoot = webRoot;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {

            String requestLine = in.readLine();
            if (requestLine == null) return;
            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String path = tokens[1];

            System.out.println(requestLine);

            if (method.equals("GET")) {
                handleGetRequest(path, out, dataOut);
            } else if (method.equals("POST")) {
                handlePostRequest(path, in, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(String path, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String[] pathAndQuery = path.split("\\?");
        String pathOnly = pathAndQuery[0];
        Request request = new Request("GET", pathOnly);

        if (pathAndQuery.length > 1) {
            String queryString = pathAndQuery[1];
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    request.addQueryParam(keyValue[0], keyValue[1]);
                }
            }
        }

        Route route = RouteManager.getRoute(pathOnly);
        if (route != null) {
            Response response = new Response(out);
            String result = route.handle(request, response);
            response.send(result);
        } else {
            StaticFiles.serveStaticFile(pathOnly, dataOut);
        }
    }



    private void handlePostRequest(String path, BufferedReader in, PrintWriter out) throws IOException {
        StringBuilder payload = new StringBuilder();
        String line;
        while (!(line = in.readLine()).isEmpty()) {}
        while (in.ready() && (line = in.readLine()) != null) {
            payload.append(line);
        }
        String body = payload.toString();
        Route route = RouteManager.getRoute(path);
        if (route != null) {
            Request request = new Request("POST", path);
            Response response = new Response(out);
            String result = route.handle(request, response);
            response.send(result);
        } else {
            saveToFile(body, out);
        }
    }

    private void serveStaticFile(String fileRequested, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        File file = new File(webRoot, fileRequested);
        if (file.exists()) {
            String contentType = getContentType(fileRequested);
            byte[] fileData = readFileData(file);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-type: " + contentType);
            out.println("Content-length: " + fileData.length);
            out.println();
            out.flush();
            dataOut.write(fileData, 0, fileData.length);
            dataOut.flush();
        } else {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-type: text/html");
            out.println();
            out.flush();
            out.println("<html><body><h1>File Not Found</h1></body></html>");
            out.flush();
        }
    }

    private void saveToFile(String body, PrintWriter out) throws IOException {
        String fileName = "received_data.txt";
        try (FileWriter fileWriter = new FileWriter(new File(webRoot, fileName))) {
            fileWriter.write(body);
        }
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain");
        out.println();
        out.println("File created: " + fileName);
        out.flush();
    }

    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".html")) return "text/html";
        else if (fileRequested.endsWith(".css")) return "text/css";
        else if (fileRequested.endsWith(".js")) return "application/javascript";
        else if (fileRequested.endsWith(".png")) return "image/png";
        else if (fileRequested.endsWith(".jpg")) return "image/jpeg";
        return "text/plain";
    }

    private byte[] readFileData(File file) throws IOException {
        byte[] fileData = new byte[(int) file.length()];
        try (FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(fileData);
        }
        return fileData;
    }
}

