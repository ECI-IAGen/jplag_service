package co.edu.eci.arep.microspring.server;

import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MicroServer {

    // Mapa que asocia rutas (URIs) a los métodos que las manejan.
    public static Map<String, Method> services = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Escanea automáticamente el classpath para cargar controladores anotados con
        // @RestController.
        loadComponentsAutomatically();
        // Inicia el servidor en el puerto 8080.
        startServer();
    }

    /**
     * Escanea el paquete base para encontrar clases anotadas con @RestController
     * y registra los métodos anotados con @GetMapping.
     */
    private static void loadComponentsAutomatically() throws Exception {
        // Ajusta el paquete base según la estructura de tu proyecto.
        Reflections reflections = new Reflections("co.edu.eci.arep");
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(RestController.class);
        for (Class<?> clazz : controllers) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(GetMapping.class)) {
                    GetMapping mapping = m.getAnnotation(GetMapping.class);
                    services.put(mapping.value(), m);
                    System.out.println("Registrado: " + mapping.value() + " -> " + m.getName());
                }
            }
        }
    }

    /**
     * Inicia el servidor, aceptando conexiones de forma secuencial.
     */
    private static void startServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Servidor corriendo en el puerto 8080...");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            handleClient(clientSocket);
        }
    }

    /**
     * Maneja la conexión de un cliente, determinando si se trata de un recurso
     * estático o de un endpoint.
     */
    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                clientSocket.close();
                return;
            }
            System.out.println("Solicitud: " + requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendResponse(out, "400 Bad Request", "text/plain", "Bad Request");
                clientSocket.close();
                return;
            }

            String method = parts[0];
            String pathWithParams = parts[1];

            if (!method.equals("GET")) {
                sendResponse(out, "405 Method Not Allowed", "text/plain", "Method Not Allowed");
                clientSocket.close();
                return;
            }

            String path;
            String query = null;
            if (pathWithParams.contains("?")) {
                String[] pathParts = pathWithParams.split("\\?");
                path = pathParts[0];
                query = pathParts[1];
            } else {
                path = pathWithParams;
            }

            // Si se solicita la raíz, redirige a index.html
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Si se solicita un recurso estático (.html o .png)
            if (isStaticResource(path)) {
                serveStaticResource(path, out);
            }
            // Si se trata de un endpoint dinámico
            else if (services.containsKey(path)) {
                Method serviceMethod = services.get(path);
                Map<String, String> queryParams = parseQuery(query);
                Object[] methodParams = resolveMethodParameters(serviceMethod, queryParams);
                Object result = serviceMethod.invoke(null, methodParams);
                String responseBody = result.toString();
                sendResponse(out, "200 OK", "text/plain", responseBody);
            } else {
                sendResponse(out, "404 Not Found", "text/plain", "Not Found");
            }
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Determina si el recurso solicitado es estático (por extensión).
     */
    private static boolean isStaticResource(String path) {
        return path.endsWith(".html") || path.endsWith(".png");
    }

    /**
     * Sirve archivos estáticos. Primero busca en el sistema de archivos y, si no se
     * encuentra,
     * intenta cargarlo desde el classpath (por ejemplo, en resources/static).
     */
    private static void serveStaticResource(String path, OutputStream out) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        byte[] fileData = null;
        // Intenta cargar el archivo desde el sistema de archivos.
        File file = new File(path);
        if (file.exists() && !file.isDirectory()) {
            fileData = Files.readAllBytes(file.toPath());
        } else {
            // Si no existe, intenta cargarlo desde el classpath (resources/static).
            InputStream is = MicroServer.class.getClassLoader().getResourceAsStream("static/" + path);
            if (is != null) {
                fileData = is.readAllBytes();
                is.close();
            }
        }

        if (fileData == null) {
            sendResponse(out, "404 Not Found", "text/plain", "File Not Found");
            return;
        }

        String contentType = "application/octet-stream";
        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        }

        sendResponse(out, "200 OK", contentType, fileData);
    }

    /**
     * Parsea la query string de la URL y devuelve un mapa de parámetros.
     */
    private static Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty())
            return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(URLDecoder.decode(keyValue[0], "UTF-8"),
                        URLDecoder.decode(keyValue[1], "UTF-8"));
            }
        }
        return params;
    }

    /**
     * Resuelve los parámetros del método usando la información de la query string y
     * la anotación @RequestParam. Se asume que los parámetros son de tipo String.
     */
    private static Object[] resolveMethodParameters(Method method, Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam reqParam = param.getAnnotation(RequestParam.class);
                String name = reqParam.value();
                String value = queryParams.getOrDefault(name, reqParam.defaultValue());
                values[i] = value;
            } else {
                values[i] = null;
            }
        }
        return values;
    }

    /**
     * Envía la respuesta HTTP, sobrecargando para enviar tanto texto como datos
     * binarios.
     */
    private static void sendResponse(OutputStream out, String status, String contentType, String body)
            throws IOException {
        byte[] bodyBytes = body.getBytes();
        sendResponse(out, status, contentType, bodyBytes);
    }

    private static void sendResponse(OutputStream out, String status, String contentType, byte[] bodyBytes)
            throws IOException {
        PrintWriter writer = new PrintWriter(out);
        writer.print("HTTP/1.1 " + status + "\r\n");
        writer.print("Content-Type: " + contentType + "\r\n");
        writer.print("Content-Length: " + bodyBytes.length + "\r\n");
        writer.print("\r\n");
        writer.flush();
        out.write(bodyBytes);
        out.flush();
    }
}
