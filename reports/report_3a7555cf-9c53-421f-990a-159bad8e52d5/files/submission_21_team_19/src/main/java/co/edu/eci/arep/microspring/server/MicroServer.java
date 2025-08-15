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
import java.util.concurrent.*;

public class MicroServer {

    public static Map<String, MethodHandler> services = new HashMap<>();
    private static volatile boolean isRunning = true;
    private static ExecutorService threadPool;

    private static class MethodHandler {
        private final Object controllerInstance;
        private final Method method;

        public MethodHandler(Object controllerInstance, Method method) {
            this.controllerInstance = controllerInstance;
            this.method = method;
        }

        public Object invoke(Object[] params) throws Exception {
            return method.invoke(controllerInstance, params);
        }
    }

    public static void main(String[] args) throws Exception {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        threadPool = new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100));

        Runtime.getRuntime().addShutdownHook(new Thread(MicroServer::gracefulShutdown));
        loadComponentsAutomatically();
        startServer();
    }

    private static void loadComponentsAutomatically() throws Exception {
        Reflections reflections = new Reflections("co.edu.eci.arep.microspring.server");
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(RestController.class);

        // Log para verificar cuántos controladores se encontraron
        System.out.println("Controladores encontrados: " + controllers.size());

        for (Class<?> clazz : controllers) {
            // Log para verificar el nombre de cada controlador
            System.out.println("Registrando controlador: " + clazz.getName());

            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    GetMapping mapping = method.getAnnotation(GetMapping.class);
                    services.put(mapping.value(), new MethodHandler(controllerInstance, method));
                    System.out.printf("Registrado: %-15s -> %s%n", mapping.value(), method.getName());
                }
            }
        }
    }

    private static void startServer() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Servidor corriendo en el puerto 8080...");

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(5000);
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } finally {
            gracefulShutdown();
        }
    }

    public static void gracefulShutdown() {
        System.out.println("\nIniciando apagado elegante...");
        isRunning = false;
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Forzando cierre de tareas pendientes...");
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Servidor detenido exitosamente.");
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty())
                return;

            // Log para depurar la línea de solicitud
            System.out.println("Request Line: " + requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !parts[0].equals("GET")) {
                sendErrorResponse(out, 405, "Método no permitido");
                return;
            }

            String pathWithParams = parts[1];
            String path = pathWithParams.split("\\?")[0];

            // Log para depurar la ruta solicitada
            System.out.println("Ruta solicitada: " + path);

            if (path.equals("/"))
                path = "/index.html";

            if (isStaticResource(path)) {
                serveStaticResource(path, out);
            } else if (services.containsKey(path)) {
                handleDynamicRequest(path, pathWithParams, out);
            } else {
                sendErrorResponse(out, 404, "Recurso no encontrado");
            }
        } catch (SocketTimeoutException ste) {
            System.err.println("Timeout en conexión: " + ste.getMessage());
        } catch (Exception e) {
            System.err.println("Error manejando cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando socket: " + e.getMessage());
            }
        }
    }

    private static void handleDynamicRequest(String path, String pathWithParams, OutputStream out) {
        try {
            MethodHandler handler = services.get(path);
            Map<String, String> queryParams = parseQuery(
                    pathWithParams.contains("?") ? pathWithParams.split("\\?")[1] : null);

            Object[] methodParams = resolveMethodParameters(handler.method, queryParams);
            Object result = handler.invoke(methodParams);

            String contentType = result instanceof Greeting ? "application/json" : "text/plain";
            sendResponse(out, 200, contentType, result.toString());
        } catch (Exception e) {
            sendErrorResponse(out, 500, "Error interno del servidor: " + e.getMessage());
        }
    }

    private static boolean isStaticResource(String path) {
        return path.endsWith(".html") || path.endsWith(".png") || path.endsWith(".js") || path.endsWith(".css");
    }

    private static void serveStaticResource(String path, OutputStream out) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        byte[] fileData = null;
        File file = new File("static/" + path);
        if (file.exists() && !file.isDirectory()) {
            fileData = Files.readAllBytes(file.toPath());
        } else {
            InputStream is = MicroServer.class.getClassLoader().getResourceAsStream("static/" + path);
            if (is != null) {
                fileData = is.readAllBytes();
                is.close();
            }
        }

        if (fileData == null) {
            sendErrorResponse(out, 404, "Archivo no encontrado");
            return;
        }

        String contentType = "application/octet-stream";
        if (path.endsWith(".html"))
            contentType = "text/html";
        else if (path.endsWith(".css"))
            contentType = "text/css";
        else if (path.endsWith(".js"))
            contentType = "application/javascript";
        else if (path.endsWith(".png"))
            contentType = "image/png";

        sendResponse(out, 200, contentType, fileData);
    }

    private static Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty())
            return params;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                params.put(key, value);
            }
        }
        return params;
    }

    private static Object[] resolveMethodParameters(Method method, Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam reqParam = param.getAnnotation(RequestParam.class);
                String value = queryParams.getOrDefault(reqParam.value(), reqParam.defaultValue());
                values[i] = convertType(value, param.getType());
            } else {
                values[i] = null;
            }
        }
        return values;
    }

    private static Object convertType(String value, Class<?> targetType) {
        if (targetType == String.class)
            return value;
        if (targetType == int.class || targetType == Integer.class)
            return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class)
            return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class)
            return Double.parseDouble(value);
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.parseBoolean(value);
        return value;
    }

    private static void sendErrorResponse(OutputStream out, int code, String message) {
        String body = String.format("{\"error\": {\"code\": %d, \"message\": \"%s\"}}", code, message);
        sendResponse(out, code, "application/json", body);
    }

    private static void sendResponse(OutputStream out, int statusCode, String contentType, String body) {
        try {
            byte[] bodyBytes = body.getBytes("UTF-8");
            sendResponse(out, statusCode, contentType, bodyBytes);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error de codificación: " + e.getMessage());
        }
    }

    private static void sendResponse(OutputStream out, int statusCode, String contentType, byte[] bodyBytes) {
        try {
            PrintWriter writer = new PrintWriter(out);
            writer.printf("HTTP/1.1 %d %s%n", statusCode, getStatusMessage(statusCode));
            writer.printf("Content-Type: %s%n", contentType);
            writer.printf("Content-Length: %d%n%n", bodyBytes.length);
            writer.flush();
            out.write(bodyBytes);
            out.flush();
        } catch (Exception e) {
            System.err.println("Error enviando respuesta: " + e.getMessage());
        }
    }

    private static String getStatusMessage(int statusCode) {
        switch (statusCode) {
            case 200:
                return "OK";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 500:
                return "Internal Server Error";
            default:
                return "Unknown Status";
        }
    }
}
