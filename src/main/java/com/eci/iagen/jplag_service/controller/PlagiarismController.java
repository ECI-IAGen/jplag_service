package com.eci.iagen.jplag_service.controller;

import com.eci.iagen.jplag_service.dto.PlagiarismDetectionRequest;
import com.eci.iagen.jplag_service.dto.PlagiarismDetectionResponse;
import com.eci.iagen.jplag_service.service.JPlagDetectionService;
import com.eci.iagen.jplag_service.service.comparison.ComparisonHtmlGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para la detección de plagio usando JPlag
 */
@RestController
@RequestMapping("/api/plagiarism")
@CrossOrigin(origins = "*")
public class PlagiarismController {

    private static final Logger logger = LoggerFactory.getLogger(PlagiarismController.class);

    @Autowired
    private JPlagDetectionService jplagDetectionService;

    @Autowired
    private ComparisonHtmlGenerator comparisonHtmlGenerator;

    /**
     * Endpoint principal para la detección de plagio
     */
    @PostMapping("/analyze")
    public ResponseEntity<PlagiarismDetectionResponse> analyzePlagiarism(
            @Valid @RequestBody PlagiarismDetectionRequest request) {

        logger.info("Received plagiarism analysis request for assignment: {} with {} submissions",
                request.getAssignmentId(), request.getSubmissions().size());

        try {
            PlagiarismDetectionResponse response = jplagDetectionService.detectPlagiarism(request);

            if (response.isSuccess()) {
                logger.info("Plagiarism analysis completed successfully for assignment: {}", request.getAssignmentId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Plagiarism analysis failed for assignment: {}", request.getAssignmentId());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error during plagiarism analysis for assignment: {}", request.getAssignmentId(), e);

            PlagiarismDetectionResponse errorResponse = new PlagiarismDetectionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Error durante el análisis: " + e.getMessage());
            errorResponse.setAssignmentId(request.getAssignmentId());
            errorResponse.setAssignmentTitle(request.getAssignmentTitle());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        logger.info("Health check requested");

        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "jplag-service");
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener información del servicio
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "JPlag Detection Service");
        info.put("version", "1.0.0");
        info.put("jplagVersion", "6.2.0");
        info.put("supportedLanguages", new String[] { "java" });
        info.put("description", "Microservicio para detección de plagio usando JPlag");

        return ResponseEntity.ok(info);
    }

    /**
     * Endpoint de test para generar archivos HTML de comparación para reportes existentes
     */
    @PostMapping("/test/generate-comparison-html/{sessionId}")
    public ResponseEntity<Map<String, Object>> testGenerateComparisonHtml(@PathVariable String sessionId) {
        logger.info("Test request to generate comparison HTML for session: {}", sessionId);

        try {
            java.nio.file.Path reportsDir = java.nio.file.Paths.get("./reports");
            java.nio.file.Path reportDir = reportsDir.resolve("report_" + sessionId);

            if (!java.nio.file.Files.exists(reportDir)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Report not found for session: " + sessionId);
                return ResponseEntity.notFound().build();
            }

            // Generar archivos HTML de comparación
            comparisonHtmlGenerator.generateComparisonHtmlFiles(reportDir, sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comparison HTML files generated successfully");
            response.put("sessionId", sessionId);
            response.put("reportDir", reportDir.toString());

            // Verificar si se creó el directorio comparation
            java.nio.file.Path baseComparationDir = java.nio.file.Paths.get("./comparation");
            java.nio.file.Path sessionComparationDir = baseComparationDir.resolve(sessionId);
            if (java.nio.file.Files.exists(sessionComparationDir)) {
                long htmlCount = java.nio.file.Files.list(sessionComparationDir)
                        .filter(path -> path.toString().endsWith(".html"))
                        .count();
                response.put("htmlFilesGenerated", htmlCount);
                response.put("comparationDir", sessionComparationDir.toString());
            } else {
                response.put("htmlFilesGenerated", 0);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating comparison HTML for session {}: {}", sessionId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error generating HTML: " + e.getMessage());
            response.put("sessionId", sessionId);

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para obtener el HTML de una comparación específica
     */
    @GetMapping("/comparison/{sessionId}/{submissionId1}-{submissionId2}")
    public ResponseEntity<Map<String, Object>> getComparisonHtml(
            @PathVariable String sessionId,
            @PathVariable String submissionId1,
            @PathVariable String submissionId2) {
        
        logger.info("Request for comparison HTML: session={}, submission1={}, submission2={}", 
                   sessionId, submissionId1, submissionId2);

        try {
            // Construir el nombre del archivo de comparación
            String comparisonFileName = submissionId1 + "-" + submissionId2 + ".html";
            
            // Verificar si existe el archivo HTML de comparación
            java.nio.file.Path baseComparationDir = java.nio.file.Paths.get("./comparation");
            java.nio.file.Path sessionComparationDir = baseComparationDir.resolve(sessionId);
            java.nio.file.Path comparisonFile = sessionComparationDir.resolve(comparisonFileName);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("submissionId1", submissionId1);
            response.put("submissionId2", submissionId2);
            response.put("comparisonId", submissionId1 + "-" + submissionId2);

            if (java.nio.file.Files.exists(comparisonFile)) {
                // Construir la URL para acceder al HTML
                String htmlUrl = "/reports/comparison/" + sessionId + "/" + submissionId1 + "-" + submissionId2 + ".html";
                
                response.put("success", true);
                response.put("message", "Comparación HTML disponible");
                response.put("htmlUrl", htmlUrl);
                response.put("fileName", comparisonFileName);
                response.put("exists", true);
                
                // Información adicional del archivo
                try {
                    long fileSize = java.nio.file.Files.size(comparisonFile);
                    java.time.LocalDateTime lastModified = java.time.LocalDateTime.ofInstant(
                        java.nio.file.Files.getLastModifiedTime(comparisonFile).toInstant(),
                        java.time.ZoneId.systemDefault()
                    );
                    
                    response.put("fileSize", fileSize);
                    response.put("lastModified", lastModified.toString());
                } catch (Exception fileInfoError) {
                    logger.warn("Could not get file info for {}: {}", comparisonFile, fileInfoError.getMessage());
                }
                
                logger.info("Comparison HTML found: {}", htmlUrl);
                return ResponseEntity.ok(response);
                
            } else {
                response.put("success", false);
                response.put("message", "Comparación HTML no encontrada");
                response.put("htmlUrl", null);
                response.put("fileName", comparisonFileName);
                response.put("exists", false);
                
                logger.warn("Comparison HTML not found: {}", comparisonFile);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting comparison HTML for session {} and submissions {}-{}: {}", 
                        sessionId, submissionId1, submissionId2, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener comparación HTML: " + e.getMessage());
            response.put("sessionId", sessionId);
            response.put("submissionId1", submissionId1);
            response.put("submissionId2", submissionId2);

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
