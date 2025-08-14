package com.eci.iagen.jplag_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controlador para servir los reportes HTML generados por JPlag
 */
@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Value("${jplag.reports-directory:./reports}")
    private String reportsDirectory;

    /**
     * Sirve el visor web de JPlag
     */
    @GetMapping("/viewer/{sessionId}")
    public ResponseEntity<String> getReportViewer(@PathVariable String sessionId) {
        try {
            // Crear una página HTML que carga el visor web de JPlag con nuestros datos
            String html = generateViewerHtml(sessionId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set("Cache-Control", "no-cache");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(html);
                    
        } catch (Exception e) {
            logger.error("Error serving JPlag viewer for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint específico para obtener el contenido HTML del reporte
     */
    @GetMapping("/view/{sessionId}")
    public ResponseEntity<String> getReportView(@PathVariable String sessionId) {
        try {
            Path reportPath = Paths.get(reportsDirectory, "report_" + sessionId, "index.html");
            
            if (!Files.exists(reportPath)) {
                logger.warn("Report not found for session: {}", sessionId);
                return ResponseEntity.notFound().build();
            }

            String htmlContent = Files.readString(reportPath);
            
            // Ajustar rutas relativas para que funcionen desde el frontend
            htmlContent = adjustHtmlForFrontend(htmlContent, sessionId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set("Cache-Control", "no-cache");
            headers.set("X-Frame-Options", "ALLOWALL");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(htmlContent);
                    
        } catch (Exception e) {
            logger.error("Error reading report content for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para verificar si un reporte existe
     */
    @GetMapping("/exists/{sessionId}")
    public ResponseEntity<Boolean> checkReportExists(@PathVariable String sessionId) {
        try {
            Path reportPath = Paths.get(reportsDirectory, "report_" + sessionId, "index.html");
            boolean exists = Files.exists(reportPath);
            
            return ResponseEntity.ok(exists);
            
        } catch (Exception e) {
            logger.error("Error checking report existence for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Ajusta las rutas en el HTML para que funcionen desde el frontend
     */
    private String adjustHtmlForFrontend(String htmlContent, String sessionId) {
        String baseUrl = "/reports/report_" + sessionId + "/";
        
        // Ajustar rutas de CSS
        htmlContent = htmlContent.replaceAll("href=\"([^\"]+\\.css)\"", 
                                           "href=\"" + baseUrl + "$1\"");
        
        // Ajustar rutas de JavaScript
        htmlContent = htmlContent.replaceAll("src=\"([^\"]+\\.js)\"", 
                                           "src=\"" + baseUrl + "$1\"");
        
        // Ajustar rutas de imágenes
        htmlContent = htmlContent.replaceAll("src=\"([^\"]+\\.(png|jpg|jpeg|gif|svg))\"", 
                                           "src=\"" + baseUrl + "$1\"");
        
        // Ajustar links relativos
        htmlContent = htmlContent.replaceAll("href=\"([^\"]+\\.html)\"", 
                                           "href=\"" + baseUrl + "$1\"");
        
        return htmlContent;
    }

    /**
     * Sirve archivos estáticos de reportes HTML
     * Solo para rutas que empiecen con /report_
     */
    @GetMapping("/report_*/**")
    public ResponseEntity<Resource> serveReportFiles(HttpServletRequest request) {
        try {
            // Obtener la ruta del archivo solicitado
            String requestPath = request.getRequestURI().substring("/reports".length());
            if (requestPath.startsWith("/")) {
                requestPath = requestPath.substring(1);
            }
            
            // Construir la ruta completa al archivo
            Path filePath = Paths.get(reportsDirectory, requestPath);
            File file = filePath.toFile();
            
            logger.info("Serving report file: {}", filePath);
            
            if (!file.exists() || !file.isFile()) {
                logger.warn("Report file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Verificar que el archivo esté dentro del directorio de reportes (seguridad)
            if (!file.getCanonicalPath().startsWith(Paths.get(reportsDirectory).toFile().getCanonicalPath())) {
                logger.warn("Attempt to access file outside reports directory: {}", filePath);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Determinar el tipo de contenido basado en la extensión del archivo
            String contentType = determineContentType(file.getName());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Error serving report file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera una página HTML que puede cargar y mostrar el reporte de JPlag
     */
    private String generateViewerHtml(String sessionId) {
        String baseUrl = "/reports/report_" + sessionId;
        
        return "<!DOCTYPE html>\n" +
            "<html lang=\"es\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Reporte de Análisis de Plagio - JPlag</title>\n" +
            "    <link href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css\" rel=\"stylesheet\">\n" +
            "    <style>\n" +
            "        * {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            box-sizing: border-box;\n" +
            "        }\n" +
            "        body {\n" +
            "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            min-height: 100vh;\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "        .container {\n" +
            "            max-width: 1400px;\n" +
            "            margin: 0 auto;\n" +
            "            background: white;\n" +
            "            border-radius: 15px;\n" +
            "            box-shadow: 0 20px 60px rgba(0,0,0,0.1);\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        .header {\n" +
            "            background: linear-gradient(135deg, #2c3e50, #3498db);\n" +
            "            color: white;\n" +
            "            padding: 30px;\n" +
            "            text-align: center;\n" +
            "            position: relative;\n" +
            "        }\n" +
            "        .header::before {\n" +
            "            content: '';\n" +
            "            position: absolute;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            right: 0;\n" +
            "            bottom: 0;\n" +
            "            background: url('data:image/svg+xml,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><defs><pattern id=\"grain\" width=\"100\" height=\"100\" patternUnits=\"userSpaceOnUse\"><circle cx=\"50\" cy=\"50\" r=\"1\" fill=\"%23ffffff\" opacity=\"0.1\"/></pattern></defs><rect width=\"100\" height=\"100\" fill=\"url(%23grain)\"/></svg>');\n" +
            "            opacity: 0.1;\n" +
            "        }\n" +
            "        .header h1 {\n" +
            "            font-size: 2.5em;\n" +
            "            margin-bottom: 10px;\n" +
            "            position: relative;\n" +
            "            z-index: 1;\n" +
            "        }\n" +
            "        .header p {\n" +
            "            font-size: 1.2em;\n" +
            "            opacity: 0.9;\n" +
            "            position: relative;\n" +
            "            z-index: 1;\n" +
            "        }\n" +
            "        .session-info {\n" +
            "            background: rgba(255,255,255,0.1);\n" +
            "            padding: 10px 20px;\n" +
            "            border-radius: 25px;\n" +
            "            display: inline-block;\n" +
            "            margin-top: 15px;\n" +
            "            font-family: monospace;\n" +
            "            position: relative;\n" +
            "            z-index: 1;\n" +
            "        }\n" +
            "        .loading {\n" +
            "            text-align: center;\n" +
            "            padding: 50px;\n" +
            "            color: #666;\n" +
            "        }\n" +
            "        .loading i {\n" +
            "            font-size: 3em;\n" +
            "            color: #3498db;\n" +
            "            animation: spin 2s linear infinite;\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        @keyframes spin {\n" +
            "            0% { transform: rotate(0deg); }\n" +
            "            100% { transform: rotate(360deg); }\n" +
            "        }\n" +
            "        .content {\n" +
            "            padding: 30px;\n" +
            "        }\n" +
            "        .stats-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));\n" +
            "            gap: 20px;\n" +
            "            margin-bottom: 30px;\n" +
            "        }\n" +
            "        .stat-card {\n" +
            "            background: linear-gradient(135deg, #f8f9fa, #e9ecef);\n" +
            "            padding: 25px;\n" +
            "            border-radius: 12px;\n" +
            "            box-shadow: 0 5px 15px rgba(0,0,0,0.08);\n" +
            "            text-align: center;\n" +
            "            border-left: 5px solid #3498db;\n" +
            "        }\n" +
            "        .stat-card h3 {\n" +
            "            color: #2c3e50;\n" +
            "            margin-bottom: 10px;\n" +
            "            font-size: 1.4em;\n" +
            "        }\n" +
            "        .stat-card .number {\n" +
            "            font-size: 2.5em;\n" +
            "            font-weight: bold;\n" +
            "            color: #3498db;\n" +
            "            display: block;\n" +
            "        }\n" +
            "        .comparisons-section {\n" +
            "            margin-top: 30px;\n" +
            "        }\n" +
            "        .section-title {\n" +
            "            font-size: 1.8em;\n" +
            "            color: #2c3e50;\n" +
            "            margin-bottom: 20px;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 10px;\n" +
            "        }\n" +
            "        .comparison-table {\n" +
            "            width: 100%;\n" +
            "            border-collapse: collapse;\n" +
            "            margin-top: 20px;\n" +
            "            border-radius: 12px;\n" +
            "            overflow: hidden;\n" +
            "            box-shadow: 0 5px 15px rgba(0,0,0,0.08);\n" +
            "        }\n" +
            "        .comparison-table thead {\n" +
            "            background: linear-gradient(135deg, #3498db, #2980b9);\n" +
            "            color: white;\n" +
            "        }\n" +
            "        .comparison-table th {\n" +
            "            padding: 18px 15px;\n" +
            "            text-align: left;\n" +
            "            font-weight: 600;\n" +
            "            letter-spacing: 0.5px;\n" +
            "        }\n" +
            "        .comparison-table td {\n" +
            "            padding: 15px;\n" +
            "            border-bottom: 1px solid #eee;\n" +
            "            vertical-align: middle;\n" +
            "        }\n" +
            "        .comparison-table tbody tr {\n" +
            "            transition: all 0.3s ease;\n" +
            "        }\n" +
            "        .comparison-table tbody tr:hover {\n" +
            "            background: #f8f9fa;\n" +
            "            transform: translateX(5px);\n" +
            "        }\n" +
            "        .similarity-badge {\n" +
            "            padding: 8px 15px;\n" +
            "            border-radius: 25px;\n" +
            "            font-weight: bold;\n" +
            "            font-size: 0.9em;\n" +
            "            display: inline-block;\n" +
            "            min-width: 70px;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        .similarity-high {\n" +
            "            background: #e74c3c;\n" +
            "            color: white;\n" +
            "        }\n" +
            "        .similarity-medium {\n" +
            "            background: #f39c12;\n" +
            "            color: white;\n" +
            "        }\n" +
            "        .similarity-low {\n" +
            "            background: #27ae60;\n" +
            "            color: white;\n" +
            "        }\n" +
            "        .btn {\n" +
            "            background: linear-gradient(135deg, #3498db, #2980b9);\n" +
            "            color: white;\n" +
            "            border: none;\n" +
            "            padding: 10px 20px;\n" +
            "            border-radius: 25px;\n" +
            "            cursor: pointer;\n" +
            "            font-size: 0.9em;\n" +
            "            font-weight: 500;\n" +
            "            transition: all 0.3s ease;\n" +
            "            text-decoration: none;\n" +
            "            display: inline-flex;\n" +
            "            align-items: center;\n" +
            "            gap: 8px;\n" +
            "        }\n" +
            "        .btn:hover {\n" +
            "            transform: translateY(-2px);\n" +
            "            box-shadow: 0 5px 15px rgba(52, 152, 219, 0.4);\n" +
            "        }\n" +
            "        .files-section {\n" +
            "            margin-top: 40px;\n" +
            "            background: #f8f9fa;\n" +
            "            padding: 25px;\n" +
            "            border-radius: 12px;\n" +
            "        }\n" +
            "        .files-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
            "            gap: 15px;\n" +
            "            margin-top: 20px;\n" +
            "        }\n" +
            "        .file-link {\n" +
            "            background: white;\n" +
            "            padding: 15px;\n" +
            "            border-radius: 8px;\n" +
            "            text-decoration: none;\n" +
            "            color: #2c3e50;\n" +
            "            transition: all 0.3s ease;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 10px;\n" +
            "            border: 2px solid transparent;\n" +
            "        }\n" +
            "        .file-link:hover {\n" +
            "            border-color: #3498db;\n" +
            "            color: #3498db;\n" +
            "            transform: translateY(-2px);\n" +
            "        }\n" +
            "        .error {\n" +
            "            background: linear-gradient(135deg, #e74c3c, #c0392b);\n" +
            "            color: white;\n" +
            "            padding: 20px;\n" +
            "            margin: 20px;\n" +
            "            border-radius: 12px;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        .no-data {\n" +
            "            text-align: center;\n" +
            "            padding: 40px;\n" +
            "            color: #7f8c8d;\n" +
            "        }\n" +
            "        .no-data i {\n" +
            "            font-size: 4em;\n" +
            "            margin-bottom: 20px;\n" +
            "            opacity: 0.5;\n" +
            "        }\n" +
            "        .submission-code {\n" +
            "            font-family: 'Courier New', monospace;\n" +
            "            background: #f1f2f6;\n" +
            "            padding: 5px 10px;\n" +
            "            border-radius: 4px;\n" +
            "            font-size: 0.9em;\n" +
            "        }\n" +
            "        .modal {\n" +
            "            position: fixed;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            width: 100%;\n" +
            "            height: 100%;\n" +
            "            background: rgba(0,0,0,0.8);\n" +
            "            z-index: 10000;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "        .modal-content {\n" +
            "            background: white;\n" +
            "            border-radius: 15px;\n" +
            "            max-width: 90%;\n" +
            "            max-height: 90%;\n" +
            "            overflow: auto;\n" +
            "            position: relative;\n" +
            "        }\n" +
            "        @media (max-width: 768px) {\n" +
            "            .container {\n" +
            "                margin: 10px;\n" +
            "                border-radius: 10px;\n" +
            "            }\n" +
            "            .header {\n" +
            "                padding: 20px;\n" +
            "            }\n" +
            "            .header h1 {\n" +
            "                font-size: 2em;\n" +
            "            }\n" +
            "            .content {\n" +
            "                padding: 20px;\n" +
            "            }\n" +
            "            .comparison-table {\n" +
            "                font-size: 0.9em;\n" +
            "            }\n" +
            "            .comparison-table th,\n" +
            "            .comparison-table td {\n" +
            "                padding: 10px 8px;\n" +
            "            }\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1><i class=\"fas fa-search-plus\"></i> Análisis de Originalidad</h1>\n" +
            "            <p>Reporte generado con JPlag - Detección de similitudes en código</p>\n" +
            "            <div class=\"session-info\">\n" +
            "                <i class=\"fas fa-id-card\"></i> Session: " + sessionId + "\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div id=\"content\">\n" +
            "            <div class=\"loading\">\n" +
            "                <i class=\"fas fa-cog\"></i>\n" +
            "                <h3>Cargando datos del reporte...</h3>\n" +
            "                <p>Por favor espere mientras procesamos la información</p>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "    <script>\n" +
            "        async function loadReport() {\n" +
            "            try {\n" +
            "                const baseUrl = '" + baseUrl + "';\n" +
            "                \n" +
            "                // Cargar múltiples archivos en paralelo\n" +
            "                const [topComparisons, runInfo, options] = await Promise.all([\n" +
            "                    fetch(baseUrl + '/topComparisons.json').then(r => r.ok ? r.json() : []),\n" +
            "                    fetch(baseUrl + '/runInformation.json').then(r => r.ok ? r.json() : {}),\n" +
            "                    fetch(baseUrl + '/options.json').then(r => r.ok ? r.json() : {})\n" +
            "                ]);\n" +
            "                \n" +
            "                showReportContent(topComparisons, runInfo, options, baseUrl);\n" +
            "                \n" +
            "            } catch (error) {\n" +
            "                console.error('Error loading report:', error);\n" +
            "                showError('No se pudo cargar el reporte: ' + error.message);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        function showReportContent(topComparisons, runInfo, options, baseUrl) {\n" +
            "            const content = document.getElementById('content');\n" +
            "            \n" +
            "            let html = '<div class=\"content\">';\n" +
            "            \n" +
            "            // Estadísticas generales\n" +
            "            html += '<div class=\"stats-grid\">';\n" +
            "            html += '<div class=\"stat-card\">';\n" +
            "            html += '<h3><i class=\"fas fa-layer-group\"></i> Comparaciones</h3>';\n" +
            "            html += '<span class=\"number\">' + (topComparisons.length || 0) + '</span>';\n" +
            "            html += '</div>';\n" +
            "            \n" +
            "            html += '<div class=\"stat-card\">';\n" +
            "            html += '<h3><i class=\"fas fa-users\"></i> Entregas</h3>';\n" +
            "            html += '<span class=\"number\">' + (runInfo.totalComparisons ? Math.ceil(Math.sqrt(2 * runInfo.totalComparisons)) : 'N/A') + '</span>';\n" +
            "            html += '</div>';\n" +
            "            \n" +
            "            html += '<div class=\"stat-card\">';\n" +
            "            html += '<h3><i class=\"fas fa-clock\"></i> Tiempo</h3>';\n" +
            "            html += '<span class=\"number\">' + (runInfo.executionTime ? (runInfo.executionTime / 1000).toFixed(2) + 's' : 'N/A') + '</span>';\n" +
            "            html += '</div>';\n" +
            "            \n" +
            "            if (topComparisons.length > 0) {\n" +
            "                const maxSimilarity = Math.max(...topComparisons.map(c => Object.values(c.similarities || {})[0] || 0));\n" +
            "                html += '<div class=\"stat-card\">';\n" +
            "                html += '<h3><i class=\"fas fa-exclamation-triangle\"></i> Max Similaridad</h3>';\n" +
            "                html += '<span class=\"number\" style=\"color: ' + getSimilarityColor(maxSimilarity) + '\">' + (maxSimilarity * 100).toFixed(1) + '%</span>';\n" +
            "                html += '</div>';\n" +
            "            }\n" +
            "            \n" +
            "            html += '</div>';\n" +
            "            \n" +
            "            // Sección de comparaciones\n" +
            "            html += '<div class=\"comparisons-section\">';\n" +
            "            html += '<h2 class=\"section-title\">';\n" +
            "            html += '<i class=\"fas fa-chart-line\"></i> Comparaciones Detalladas';\n" +
            "            html += '</h2>';\n" +
            "            \n" +
            "            if (topComparisons && topComparisons.length > 0) {\n" +
            "                html += '<div style=\"overflow-x: auto;\">';\n" +
            "                html += '<table class=\"comparison-table\">';\n" +
            "                html += '<thead>';\n" +
            "                html += '<tr>';\n" +
            "                html += '<th><i class=\"fas fa-file-code\"></i> Entrega 1</th>';\n" +
            "                html += '<th><i class=\"fas fa-file-code\"></i> Entrega 2</th>';\n" +
            "                html += '<th><i class=\"fas fa-percentage\"></i> Similaridad</th>';\n" +
            "                html += '<th><i class=\"fas fa-ruler\"></i> Métricas</th>';\n" +
            "                html += '<th><i class=\"fas fa-eye\"></i> Acciones</th>';\n" +
            "                html += '</tr>';\n" +
            "                html += '</thead>';\n" +
            "                html += '<tbody>';\n" +
            "                \n" +
            "                // Ordenar por similaridad descendente\n" +
            "                const sortedComparisons = topComparisons.sort((a, b) => {\n" +
            "                    const simA = Object.values(a.similarities || {})[0] || 0;\n" +
            "                    const simB = Object.values(b.similarities || {})[0] || 0;\n" +
            "                    return simB - simA;\n" +
            "                });\n" +
            "                \n" +
            "                sortedComparisons.forEach((comparison, index) => {\n" +
            "                    const similarities = comparison.similarities || {};\n" +
            "                    const avgSimilarity = similarities.AVG || 0;\n" +
            "                    const maxSimilarity = similarities.MAX || 0;\n" +
            "                    const maximumLength = similarities.MAXIMUM_LENGTH || 0;\n" +
            "                    const longestMatch = similarities.LONGEST_MATCH || 0;\n" +
            "                    \n" +
            "                    const avgPercentage = (avgSimilarity * 100).toFixed(2);\n" +
            "                    const maxPercentage = (maxSimilarity * 100).toFixed(2);\n" +
            "                    \n" +
            "                    html += '<tr>';\n" +
            "                    html += '<td><span class=\"submission-code\">' + comparison.firstSubmission + '</span></td>';\n" +
            "                    html += '<td><span class=\"submission-code\">' + comparison.secondSubmission + '</span></td>';\n" +
            "                    html += '<td>';\n" +
            "                    html += '<div style=\"text-align: center;\">';\n" +
            "                    html += '<span class=\"similarity-badge ' + getSimilarityClass(avgSimilarity) + '\" style=\"display: block; margin-bottom: 5px;\">AVG: ' + avgPercentage + '%</span>';\n" +
            "                    html += '<span class=\"similarity-badge ' + getSimilarityClass(maxSimilarity) + '\">MAX: ' + maxPercentage + '%</span>';\n" +
            "                    html += '</div></td>';\n" +
            "                    html += '<td style=\"text-align: center;\">';\n" +
            "                    html += '<div style=\"font-size: 0.9em;\">';\n" +
            "                    html += '<div style=\"margin-bottom: 3px;\"><strong>Long. Máx:</strong> ' + Math.round(maximumLength) + '</div>';\n" +
            "                    html += '<div><strong>Match Largo:</strong> ' + Math.round(longestMatch) + '</div>';\n" +
            "                    html += '</div></td>';\n" +
            "                    html += '<td>';\n" +
            "                    html += '<button class=\"btn\" onclick=\"viewDetailedComparison(\\'' + comparison.firstSubmission + '\\', \\'' + comparison.secondSubmission + '\\', \\'' + baseUrl + '\\')\">';\n" +
            "                    html += '<i class=\"fas fa-search-plus\"></i> Ver Detalles</button>';\n" +
            "                    html += '</td>';\n" +
            "                    html += '</tr>';\n" +
            "                });\n" +
            "                \n" +
            "                html += '</tbody></table></div>';\n" +
            "            } else {\n" +
            "                html += '<div class=\"no-data\">';\n" +
            "                html += '<i class=\"fas fa-search\"></i>';\n" +
            "                html += '<h3>No se encontraron comparaciones</h3>';\n" +
            "                html += '<p>No hay datos de similitud disponibles para mostrar.</p>';\n" +
            "                html += '</div>';\n" +
            "            }\n" +
            "            \n" +
            "            html += '</div>';\n" +
            "            \n" +
            "            // Sección de archivos\n" +
            "            html += '<div class=\"files-section\">';\n" +
            "            html += '<h2 class=\"section-title\">';\n" +
            "            html += '<i class=\"fas fa-folder-open\"></i> Archivos de Datos';\n" +
            "            html += '</h2>';\n" +
            "            html += '<p>Descarga los archivos JSON con datos detallados del análisis:</p>';\n" +
            "            html += '<div class=\"files-grid\">';\n" +
            "            \n" +
            "            const files = [\n" +
            "                { name: 'topComparisons.json', icon: 'fas fa-chart-bar', desc: 'Comparaciones principales' },\n" +
            "                { name: 'cluster.json', icon: 'fas fa-project-diagram', desc: 'Análisis de clusters' },\n" +
            "                { name: 'distribution.json', icon: 'fas fa-chart-pie', desc: 'Distribución de similitudes' },\n" +
            "                { name: 'options.json', icon: 'fas fa-cogs', desc: 'Configuración del análisis' },\n" +
            "                { name: 'runInformation.json', icon: 'fas fa-info-circle', desc: 'Información de ejecución' }\n" +
            "            ];\n" +
            "            \n" +
            "            files.forEach(file => {\n" +
            "                html += '<a href=\"' + baseUrl + '/' + file.name + '\" target=\"_blank\" class=\"file-link\">';\n" +
            "                html += '<i class=\"' + file.icon + '\"></i>';\n" +
            "                html += '<div>';\n" +
            "                html += '<strong>' + file.name + '</strong><br>';\n" +
            "                html += '<small>' + file.desc + '</small>';\n" +
            "                html += '</div>';\n" +
            "                html += '</a>';\n" +
            "            });\n" +
            "            \n" +
            "            html += '</div></div>';\n" +
            "            html += '</div>';\n" +
            "            \n" +
            "            content.innerHTML = html;\n" +
            "        }\n" +
            "        \n" +
            "        function getSimilarityColor(similarity) {\n" +
            "            if (similarity > 0.8) return '#e74c3c';\n" +
            "            if (similarity > 0.6) return '#f39c12';\n" +
            "            if (similarity > 0.3) return '#f1c40f';\n" +
            "            return '#27ae60';\n" +
            "        }\n" +
            "        \n" +
            "        function getSimilarityClass(similarity) {\n" +
            "            if (similarity > 0.6) return 'similarity-high';\n" +
            "            if (similarity > 0.3) return 'similarity-medium';\n" +
            "            return 'similarity-low';\n" +
            "        }\n" +
            "        \n" +
            "        function viewDetailedComparison(first, second, baseUrl) {\n" +
            "            const comparisonFile = first + '-' + second + '.json';\n" +
            "            const url = baseUrl + '/comparisons/' + comparisonFile;\n" +
            "            \n" +
            "            // Crear ventana modal para mostrar comparación detallada\n" +
            "            showComparisonModal(url, first, second);\n" +
            "        }\n" +
            "        \n" +
            "        function showComparisonModal(url, first, second) {\n" +
            "            const modal = document.createElement('div');\n" +
            "            modal.className = 'modal';\n" +
            "            \n" +
            "            const modalContent = document.createElement('div');\n" +
            "            modalContent.className = 'modal-content';\n" +
            "            \n" +
            "            modalContent.innerHTML = `\n" +
            "                <div style=\"padding: 30px; border-bottom: 1px solid #eee;\">\n" +
            "                    <h2 style=\"margin: 0; color: #2c3e50;\">\n" +
            "                        <i class=\"fas fa-code-branch\"></i> Comparación Detallada\n" +
            "                    </h2>\n" +
            "                    <p style=\"margin: 10px 0 0; color: #7f8c8d;\">` + first + ` vs ` + second + `</p>\n" +
            "                    <button onclick=\"this.closest('.modal').remove()\" style=\"\n" +
            "                        position: absolute;\n" +
            "                        top: 15px;\n" +
            "                        right: 15px;\n" +
            "                        background: none;\n" +
            "                        border: none;\n" +
            "                        font-size: 1.5em;\n" +
            "                        cursor: pointer;\n" +
            "                        color: #7f8c8d;\n" +
            "                    \">&times;</button>\n" +
            "                </div>\n" +
            "                <div id=\"modalBody\" style=\"padding: 30px;\">\n" +
            "                    <div style=\"text-align: center; padding: 40px;\">\n" +
            "                        <i class=\"fas fa-cog fa-spin\" style=\"font-size: 2em; color: #3498db;\"></i>\n" +
            "                        <p style=\"margin-top: 20px;\">Cargando datos de comparación...</p>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            `;\n" +
            "            \n" +
            "            modal.appendChild(modalContent);\n" +
            "            document.body.appendChild(modal);\n" +
            "            \n" +
            "            // Cargar datos de la comparación\n" +
            "            fetch(url)\n" +
            "                .then(response => response.json())\n" +
            "                .then(data => {\n" +
            "                    displayComparisonDetails(data, document.getElementById('modalBody'));\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    document.getElementById('modalBody').innerHTML = `\n" +
            "                        <div style=\"text-align: center; color: #e74c3c;\">\n" +
            "                            <i class=\"fas fa-exclamation-triangle\" style=\"font-size: 2em;\"></i>\n" +
            "                            <p style=\"margin-top: 20px;\">Error al cargar los detalles: ` + error.message + `</p>\n" +
            "                            <a href=\"` + url + `\" target=\"_blank\" class=\"btn\" style=\"margin-top: 15px;\">\n" +
            "                                <i class=\"fas fa-external-link-alt\"></i> Abrir JSON\n" +
            "                            </a>\n" +
            "                        </div>\n" +
            "                    `;\n" +
            "                });\n" +
            "        }\n" +
            "        \n" +
            "        function displayComparisonDetails(data, container) {\n" +
            "            const similarities = data.similarities || {};\n" +
            "            const matches = data.matches || [];\n" +
            "            \n" +
            "            let html = '<div style=\"display: grid; gap: 25px;\">';\n" +
            "            \n" +
            "            // Métricas de similaridad\n" +
            "            html += '<div style=\"background: #f8f9fa; padding: 20px; border-radius: 10px;\">';\n" +
            "            html += '<h3 style=\"margin: 0 0 15px; color: #2c3e50;\">';\n" +
            "            html += '<i class=\"fas fa-chart-bar\"></i> Métricas de Similaridad</h3>';\n" +
            "            html += '<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;\">';\n" +
            "            \n" +
            "            for (const [metric, value] of Object.entries(similarities)) {\n" +
            "                let displayValue;\n" +
            "                if (metric === 'MAXIMUM_LENGTH' || metric === 'LONGEST_MATCH') {\n" +
            "                    // Mostrar como valor absoluto\n" +
            "                    displayValue = Math.round(value);\n" +
            "                } else {\n" +
            "                    // Mostrar como porcentaje\n" +
            "                    displayValue = (value * 100).toFixed(2) + '%';\n" +
            "                }\n" +
            "                \n" +
            "                html += '<div style=\"background: white; padding: 15px; border-radius: 8px; text-align: center; border-left: 4px solid ' + getSimilarityColor(value) + ';\">';\n" +
            "                html += '<div style=\"font-size: 0.9em; color: #7f8c8d; margin-bottom: 5px;\">' + metric + '</div>';\n" +
            "                html += '<div style=\"font-size: 1.8em; font-weight: bold; color: ' + getSimilarityColor(value) + ';\">' + displayValue + '</div>';\n" +
            "                html += '</div>';\n" +
            "            }\n" +
            "            \n" +
            "            html += '</div></div>';\n" +
            "            \n" +
            "            // Información de coincidencias\n" +
            "            html += '<div style=\"background: #f8f9fa; padding: 20px; border-radius: 10px;\">';\n" +
            "            html += '<h3 style=\"margin: 0 0 15px; color: #2c3e50;\">';\n" +
            "            html += '<i class=\"fas fa-search\"></i> Coincidencias Encontradas (' + matches.length + ')</h3>';\n" +
            "            \n" +
            "            if (matches.length > 0) {\n" +
            "                html += '<div style=\"max-height: 300px; overflow-y: auto;\">';\n" +
            "                matches.slice(0, 10).forEach((match, index) => {\n" +
            "                    html += '<div style=\"background: white; margin-bottom: 10px; padding: 15px; border-radius: 8px; border-left: 4px solid #3498db;\">';\n" +
            "                    html += '<div style=\"font-weight: bold; margin-bottom: 8px;\">Coincidencia ' + (index + 1) + '</div>';\n" +
            "                    html += '<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 15px; font-size: 0.9em;\">';\n" +
            "                    html += '<div>';\n" +
            "                    html += '<strong>Archivo 1:</strong><br>';\n" +
            "                    html += '<code style=\"background: #f1f2f6; padding: 2px 6px; border-radius: 3px;\">' + (match.firstFileName || 'N/A') + '</code><br>';\n" +
            "                    html += '<small>Líneas: ' + (match.startInFirst?.line || 'N/A') + '-' + (match.endInFirst?.line || 'N/A') + '</small>';\n" +
            "                    html += '</div>';\n" +
            "                    html += '<div>';\n" +
            "                    html += '<strong>Archivo 2:</strong><br>';\n" +
            "                    html += '<code style=\"background: #f1f2f6; padding: 2px 6px; border-radius: 3px;\">' + (match.secondFileName || 'N/A') + '</code><br>';\n" +
            "                    html += '<small>Líneas: ' + (match.startInSecond?.line || 'N/A') + '-' + (match.endInSecond?.line || 'N/A') + '</small>';\n" +
            "                    html += '</div>';\n" +
            "                    html += '</div>';\n" +
            "                    if (match.tokens) {\n" +
            "                        html += '<div style=\"margin-top: 10px; font-size: 0.85em; color: #7f8c8d;\">';\n" +
            "                        html += '<i class=\"fas fa-code\"></i> Tokens: ' + match.tokens;\n" +
            "                        html += '</div>';\n" +
            "                    }\n" +
            "                    html += '</div>';\n" +
            "                });\n" +
            "                \n" +
            "                if (matches.length > 10) {\n" +
            "                    html += '<div style=\"text-align: center; margin-top: 15px; color: #7f8c8d;\">';\n" +
            "                    html += 'Mostrando 10 de ' + matches.length + ' coincidencias';\n" +
            "                    html += '</div>';\n" +
            "                }\n" +
            "                \n" +
            "                html += '</div>';\n" +
            "            } else {\n" +
            "                html += '<div style=\"text-align: center; color: #7f8c8d; padding: 20px;\">';\n" +
            "                html += '<i class=\"fas fa-info-circle\"></i> No se encontraron coincidencias detalladas';\n" +
            "                html += '</div>';\n" +
            "            }\n" +
            "            \n" +
            "            html += '</div></div>';\n" +
            "            \n" +
            "            container.innerHTML = html;\n" +
            "        }\n" +
            "        \n" +
            "        function showError(message) {\n" +
            "            const content = document.getElementById('content');\n" +
            "            content.innerHTML = '<div class=\"error\">';\n" +
            "            content.innerHTML += '<i class=\"fas fa-exclamation-triangle\" style=\"font-size: 2em; margin-bottom: 15px;\"></i>';\n" +
            "            content.innerHTML += '<h3>Error al cargar el reporte</h3>';\n" +
            "            content.innerHTML += '<p>' + message + '</p>';\n" +
            "            content.innerHTML += '</div>';\n" +
            "        }\n" +
            "        \n" +
            "        // Cargar el reporte cuando la página esté lista\n" +
            "        document.addEventListener('DOMContentLoaded', loadReport);\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * Determina el tipo de contenido basado en la extensión del archivo
     */
    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }
}
