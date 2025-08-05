package com.eci.iagen.jplag_service.controller;

import com.eci.iagen.jplag_service.dto.request.PlagiarismRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de compatibilidad para el API Gateway
 */
@RestController
@RequestMapping("/api/jplag")
@CrossOrigin(origins = "*")
public class JPlagCompatibilityController {

    private static final Logger logger = LoggerFactory.getLogger(JPlagCompatibilityController.class);

    @Autowired
    private PlagiarismController plagiarismController;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Endpoint compatible con API Gateway
     */
    @PostMapping("/detect")
    public ResponseEntity<?> detectPlagiarism(@Valid @RequestBody PlagiarismRequest request) {
        logger.info(
                "Plagiarism detection requested via compatibility endpoint /api/jplag/detect for assignment: {} with {} submissions",
                request.getAssignmentId(), request.getSubmissions().size());

        try {
            // Delegar al controlador principal
            ResponseEntity<?> response = plagiarismController.analyzePlagiarism(request);

            // Log del JSON de respuesta desde el compatibility endpoint
            if (response.hasBody()) {
                try {
                    String responseJson = objectMapper.writeValueAsString(response.getBody());
                    logger.info("=== JPlag Compatibility Endpoint Response JSON ===");
                    logger.info("Response from /api/jplag/detect to API Gateway:");
                    logger.info("{}", responseJson);
                    logger.info("=== End Compatibility Response JSON ===");
                } catch (Exception jsonException) {
                    logger.warn("Error serializing compatibility response to JSON for logging: {}",
                            jsonException.getMessage());
                }
            }

            logger.info("Plagiarism detection response: status={}, hasBody={}",
                    response.getStatusCode(), response.hasBody());
            return response;
        } catch (Exception e) {
            logger.error("Error in JPlag compatibility controller: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "JPlag compatibility controller error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Health check endpoint compatible con API Gateway
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "jplag-service");
        status.put("endpoint", "/api/jplag/health");
        return ResponseEntity.ok(status);
    }
}
