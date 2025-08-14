package com.eci.iagen.jplag_service.controller;

import com.eci.iagen.jplag_service.dto.PlagiarismDetectionRequest;
import com.eci.iagen.jplag_service.dto.PlagiarismDetectionResponse;
import com.eci.iagen.jplag_service.service.JPlagDetectionService;
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
        info.put("supportedLanguages", new String[]{"java"});
        info.put("description", "Microservicio para detección de plagio usando JPlag");
        
        return ResponseEntity.ok(info);
    }
}
