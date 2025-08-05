package com.eci.iagen.jplag_service.controller;

import com.eci.iagen.jplag_service.dto.request.PlagiarismRequest;
import com.eci.iagen.jplag_service.dto.response.PlagiarismResponse;
import com.eci.iagen.jplag_service.service.JPlagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para detección de plagio
 */
@RestController
@RequestMapping("/api/plagiarism")
@CrossOrigin(origins = "*")
public class PlagiarismController {

    private static final Logger logger = LoggerFactory.getLogger(PlagiarismController.class);

    @Autowired
    private JPlagService jplagService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Endpoint principal para análisis de plagio
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzePlagiarism(@Valid @RequestBody PlagiarismRequest request) {
        logger.info("Plagiarism analysis requested for assignment: {} with {} submissions",
                request.getAssignmentId(), request.getSubmissions().size());

        try {
            // Validar número mínimo de entregas
            if (request.getSubmissions().size() < 2) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "At least 2 submissions are required for comparison");
                return ResponseEntity.badRequest().body(error);
            }

            // Ejecutar análisis
            PlagiarismResponse response = jplagService.analyzePlagiarism(request);

            // Log del JSON de respuesta que se enviará al API Gateway
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                logger.info("=== JPlag Service Response JSON ===");
                logger.info("Sending response to API Gateway:");
                logger.info("{}", responseJson);
                logger.info("=== End Response JSON ===");
            } catch (Exception jsonException) {
                logger.warn("Error serializing response to JSON for logging: {}", jsonException.getMessage());
            }

            logger.info("Plagiarism analysis completed successfully for assignment: {}",
                    request.getAssignmentId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during plagiarism analysis for assignment: {} - Error: {} - StackTrace: {}",
                    request.getAssignmentId(), e.getMessage(), e.getClass().getSimpleName(), e);

            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            error.put("assignmentId", String.valueOf(request.getAssignmentId()));
            error.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "jplag-service");
        return ResponseEntity.ok(status);
    }

    /**
     * Health check endpoint compatible con API Gateway
     */
    @GetMapping("/api/jplag/health")
    public ResponseEntity<Map<String, String>> healthCompat() {
        return health();
    }

    /**
     * Manejo de errores de validación
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        logger.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }
}
