package edu.escuelaing.arep.ASE.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

public class StaticFiles {
    private static String staticFilesLocation;

    public static void setLocation(String location) {
        staticFilesLocation = location;
    }

    public static void serveStaticFile(String filePath, OutputStream out) throws IOException {
        File file = new File(staticFilesLocation, filePath);
        if (file.exists()) {
            String contentType = getContentType(filePath);
            int fileLength = (int) file.length();

            if (contentType.startsWith("image")) {
                byte[] fileData = readFileData(file, fileLength);
                String base64Image = Base64.getEncoder().encodeToString(fileData);

                String htmlResponse = "<!DOCTYPE html>\r\n"
                        + "<html>\r\n"
                        + "    <head>\r\n"
                        + "        <title>Image</title>\r\n"
                        + "    </head>\r\n"
                        + "    <body>\r\n"
                        + "        <center><img src=\"data:" + contentType + ";base64," + base64Image + "\" alt=\"image\"></center>\r\n"
                        + "    </body>\r\n"
                        + "</html>";
                out.write(("HTTP/1.1 200 OK\r\n").getBytes());
                out.write(("Content-Type: text/html\r\n").getBytes());
                out.write(("Content-Length: " + htmlResponse.length() + "\r\n").getBytes());
                out.write(("\r\n").getBytes()); // Línea en blanco entre las cabeceras y el cuerpo
                out.write(htmlResponse.getBytes());
                out.flush();
            } else {
                // Para otros tipos de archivos, servir directamente
                FileInputStream fileIn = new FileInputStream(file);
                byte[] fileData = readFileData(file, fileLength);
                out.write(("HTTP/1.1 200 OK\r\n").getBytes());
                out.write(("Content-Type: " + contentType + "\r\n").getBytes());
                out.write(("Content-Length: " + fileLength + "\r\n").getBytes());
                out.write(("\r\n").getBytes()); // Línea en blanco entre las cabeceras y el cuerpo
                out.write(fileData);
                out.flush();
            }
        } else {
            out.write("HTTP/1.1 404 Not Found\r\n".getBytes());
            out.write("Content-Type: text/html\r\n".getBytes());
            out.write("\r\n".getBytes());
            out.write("<html><body><h1>File Not Found</h1></body></html>".getBytes());
            out.flush();
        }
    }

    private static byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = new FileInputStream(file);
        byte[] fileData = new byte[fileLength];
        fileIn.read(fileData);
        fileIn.close();
        return fileData;
    }

    private static String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".html")) return "text/html";
        else if (fileRequested.endsWith(".css")) return "text/css";
        else if (fileRequested.endsWith(".js")) return "application/javascript";
        else if (fileRequested.endsWith(".png")) return "image/png";
        else if (fileRequested.endsWith(".jpg") || fileRequested.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
    }
}
