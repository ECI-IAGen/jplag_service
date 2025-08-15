package com.eci.iagen.jplag_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controlador para servir reportes HTML generados por JPlag
 */
@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Value("${jplag.reports-directory:./reports}")
    private String reportsDirectory;

    @Value("${jplag.comparation-directory:./comparation}")
    private String comparationDirectory;

    /**
     * Sirve el reporte principal HTML de JPlag
     */
    @GetMapping("/viewer/{sessionId}")
    public ResponseEntity<Resource> getMainReport(@PathVariable String sessionId) {
        try {
            Path reportDir = Paths.get(reportsDirectory).resolve("report_" + sessionId);
            Path indexFile = reportDir.resolve("index.html");

            if (!Files.exists(indexFile)) {
                logger.warn("Report index.html not found for session: {}", sessionId);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(indexFile.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"jplag-report.html\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving main report for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sirve archivos estáticos del reporte JPlag (CSS, JS, etc.)
     */
    @GetMapping("/viewer/{sessionId}/**")
    public ResponseEntity<Resource> getReportAsset(
            @PathVariable String sessionId,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI();
            String assetPath = requestPath.substring(requestPath.indexOf(sessionId) + sessionId.length() + 1);

            Path reportDir = Paths.get(reportsDirectory).resolve("report_" + sessionId);
            Path assetFile = reportDir.resolve(assetPath);

            // Verificar que el archivo esté dentro del directorio del reporte (seguridad)
            if (!assetFile.normalize().startsWith(reportDir.normalize())) {
                logger.warn("Path traversal attempt blocked for session {}: {}", sessionId, assetPath);
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(assetFile)) {
                logger.debug("Asset not found for session {}: {}", sessionId, assetPath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(assetFile.toUri());
            String contentType = determineContentType(assetPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving asset for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sirve archivos HTML individuales de comparación
     */
    @GetMapping("/comparison/{sessionId}/{comparisonId}.html")
    public ResponseEntity<Resource> getComparisonReport(
            @PathVariable String sessionId,
            @PathVariable String comparisonId) {
        try {
            // Usar el directorio independiente de comparación
            Path baseComparationDir = Paths.get(comparationDirectory);
            Path sessionComparationDir = baseComparationDir.resolve(sessionId);
            Path comparisonFile = sessionComparationDir.resolve(comparisonId + ".html");

            if (!Files.exists(comparisonFile)) {
                logger.warn("Comparison HTML not found for session {} and comparison {}: {}",
                        sessionId, comparisonId, comparisonFile);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(comparisonFile.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"comparison-" + comparisonId + ".html\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving comparison report for session {} and comparison {}: {}",
                    sessionId, comparisonId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista todas las comparaciones disponibles para una sesión
     */
    @GetMapping("/comparison/{sessionId}/list")
    public ResponseEntity<?> listComparisons(@PathVariable String sessionId) {
        try {
            // Usar el directorio independiente de comparación
            Path baseComparationDir = Paths.get(comparationDirectory);
            Path sessionComparationDir = baseComparationDir.resolve(sessionId);

            if (!Files.exists(sessionComparationDir)) {
                logger.warn("Comparation directory not found for session: {}", sessionId);
                return ResponseEntity.notFound().build();
            }

            java.util.List<String> comparisons = Files.list(sessionComparationDir)
                    .filter(path -> path.toString().endsWith(".html"))
                    .map(path -> path.getFileName().toString().replace(".html", ""))
                    .collect(java.util.stream.Collectors.toList());

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("sessionId", sessionId);
            response.put("totalComparisons", comparisons.size());
            response.put("comparisons", comparisons);
            response.put("comparationDirectory", sessionComparationDir.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing comparisons for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check para verificar el directorio de reportes
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Path reportsDir = Paths.get(reportsDirectory);
            boolean exists = Files.exists(reportsDir);
            boolean writable = Files.isWritable(reportsDir);

            java.util.Map<String, Object> health = new java.util.HashMap<>();
            health.put("status", exists && writable ? "UP" : "DOWN");
            health.put("reportsDirectory", reportsDir.toAbsolutePath().toString());
            health.put("exists", exists);
            health.put("writable", writable);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Error checking reports health: {}", e.getMessage(), e);

            java.util.Map<String, Object> health = new java.util.HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(health);
        }
    }

    /**
     * Determina el tipo de contenido basado en la extensión del archivo
     */
    private String determineContentType(String filename) {
        if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".js")) {
            return "application/javascript";
        } else if (filename.endsWith(".html")) {
            return "text/html";
        } else if (filename.endsWith(".json")) {
            return "application/json";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "application/octet-stream";
        }
    }
}
