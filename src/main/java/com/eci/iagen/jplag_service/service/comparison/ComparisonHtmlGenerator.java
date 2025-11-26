package com.eci.iagen.jplag_service.service.comparison;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servicio para generar archivos HTML individuales de comparaciones JPlag
 */
@Service
public class ComparisonHtmlGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonHtmlGenerator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jplag.comparation-directory:./comparation}")
    private String comparationDirectory;

    /**
     * Genera archivos HTML individuales para todas las comparaciones en un reporte
     * 
     * @param reportDir El directorio del reporte JPlag
     * @param sessionId ID de la sesión para identificar el reporte
     */
    public void generateComparisonHtmlFiles(Path reportDir, String sessionId) {
        try {
            // Usar directorio independiente organizado por sessionId
            Path baseComparationDir = java.nio.file.Paths.get(comparationDirectory).toAbsolutePath().normalize();
            Path sessionComparationDir = baseComparationDir.resolve(sessionId);

            // Crear directorio de salida si no existe
            Files.createDirectories(sessionComparationDir);

            Path comparisonsDir = reportDir.resolve("comparisons");
            if (!Files.exists(comparisonsDir)) {
                logger.warn("Comparisons directory not found: {}", comparisonsDir);
                return;
            }

            // Procesar todos los archivos JSON de comparación
            Files.list(comparisonsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(comparisonFile -> {
                        try {
                            generateSingleComparisonHtml(comparisonFile, sessionComparationDir, sessionId);
                        } catch (Exception e) {
                            logger.error("Error generating HTML for comparison file: {}", comparisonFile, e);
                        }
                    });

            logger.info("Generated comparison HTML files in: {}", sessionComparationDir);

        } catch (Exception e) {
            logger.error("Error generating comparison HTML files for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * Genera un archivo HTML individual para una comparación específica
     */
    private void generateSingleComparisonHtml(Path comparisonFile, Path outputDir, String sessionId)
            throws IOException {
        // Leer el archivo JSON de comparación
        String jsonContent = Files.readString(comparisonFile);
        JsonNode comparisonData = objectMapper.readTree(jsonContent);

        // Extraer información básica
        String firstSubmissionId = comparisonData.get("firstSubmissionId").asText();
        String secondSubmissionId = comparisonData.get("secondSubmissionId").asText();
        JsonNode similarities = comparisonData.get("similarities");
        JsonNode matches = comparisonData.get("matches");

        // Extraer solo los IDs numéricos de los nombres de directorio
        // Los nombres vienen como "submission_19_team_17", queremos solo "19"
        String numericId1 = extractNumericId(firstSubmissionId);
        String numericId2 = extractNumericId(secondSubmissionId);

        // Generar el contenido HTML
        String htmlContent = generateHtmlContent(
                firstSubmissionId,
                secondSubmissionId,
                similarities,
                matches,
                sessionId);

        // Crear nombre de archivo basado en los IDs numéricos
        String fileName = numericId1 + "-" + numericId2 + ".html";
        Path outputFile = outputDir.resolve(fileName);

        // Escribir el archivo HTML
        Files.writeString(outputFile, htmlContent);

        logger.info("Generated comparison HTML: {}", fileName);
    }

    /**
     * Genera el contenido HTML para una comparación
     */
    private String generateHtmlContent(String firstSubmissionId, String secondSubmissionId,
            JsonNode similarities, JsonNode matches, String sessionId) {

        StringBuilder html = new StringBuilder();

        // Header HTML
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"es\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Comparación de Plagio: ").append(firstSubmissionId).append(" vs ")
                .append(secondSubmissionId).append("</title>\n");
        html.append(
                "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n");
        html.append(
                "    <link href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\" rel=\"stylesheet\">\n");
        html.append("    <style>\n");
        html.append(getCustomCss());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header de la página
        html.append("    <div class=\"container-fluid py-4\">\n");
        html.append("        <div class=\"row\">\n");
        html.append("            <div class=\"col-12\">\n");
        html.append("                <div class=\"card shadow-sm\">\n");
        html.append("                    <div class=\"card-header bg-primary text-white\">\n");
        html.append("                        <h1 class=\"h3 mb-0\">\n");
        html.append("                            <i class=\"fas fa-search-plus me-2\"></i>\n");
        html.append("                            Análisis Detallado de Similitud\n");
        html.append("                        </h1>\n");
        html.append("                        <small>Reporte generado: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .append("</small>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"card-body\">\n");

        // Información de las submissions
        html.append(generateSubmissionInfo(firstSubmissionId, secondSubmissionId));

        // Métricas de similitud
        html.append(generateSimilarityMetrics(similarities));

        // Lista de coincidencias
        html.append(generateMatchesList(matches));

        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");

        // Scripts JavaScript
        html.append(
                "    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js\"></script>\n");
        html.append("    <script>\n");
        html.append(getCustomJavaScript());
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Genera la sección de información de las submissions
     */
    private String generateSubmissionInfo(String firstSubmissionId, String secondSubmissionId) {
        StringBuilder html = new StringBuilder();

        html.append("                        <div class=\"row mb-4\">\n");
        html.append("                            <div class=\"col-md-6\">\n");
        html.append("                                <div class=\"card bg-light\">\n");
        html.append("                                    <div class=\"card-body\">\n");
        html.append("                                        <h5 class=\"card-title text-primary\">\n");
        html.append(
                "                                            <i class=\"fas fa-file-code me-2\"></i>Submission 1\n");
        html.append("                                        </h5>\n");
        html.append("                                        <p class=\"card-text\">\n");
        html.append("                                            <strong>ID:</strong> ").append(firstSubmissionId)
                .append("<br>\n");
        html.append("                                            <strong>Equipo:</strong> ")
                .append(extractTeamFromId(firstSubmissionId)).append("\n");
        html.append("                                        </p>\n");
        html.append("                                    </div>\n");
        html.append("                                </div>\n");
        html.append("                            </div>\n");
        html.append("                            <div class=\"col-md-6\">\n");
        html.append("                                <div class=\"card bg-light\">\n");
        html.append("                                    <div class=\"card-body\">\n");
        html.append("                                        <h5 class=\"card-title text-info\">\n");
        html.append(
                "                                            <i class=\"fas fa-file-code me-2\"></i>Submission 2\n");
        html.append("                                        </h5>\n");
        html.append("                                        <p class=\"card-text\">\n");
        html.append("                                            <strong>ID:</strong> ").append(secondSubmissionId)
                .append("<br>\n");
        html.append("                                            <strong>Equipo:</strong> ")
                .append(extractTeamFromId(secondSubmissionId)).append("\n");
        html.append("                                        </p>\n");
        html.append("                                    </div>\n");
        html.append("                                </div>\n");
        html.append("                            </div>\n");
        html.append("                        </div>\n");

        return html.toString();
    }

    /**
     * Genera la sección de métricas de similitud
     */
    private String generateSimilarityMetrics(JsonNode similarities) {
        StringBuilder html = new StringBuilder();

        double avgSimilarity = similarities.get("AVG").asDouble();
        double maxSimilarity = similarities.get("MAX").asDouble();
        double maxLength = similarities.get("MAXIMUM_LENGTH").asDouble();
        double longestMatch = similarities.get("LONGEST_MATCH").asDouble();

        // Determinar el color basado en el nivel de similitud
        String colorClass = getSimilarityColorClass(avgSimilarity);
        String riskLevel = getSimilarityRiskLevel(avgSimilarity);

        html.append("                        <div class=\"row mb-4\">\n");
        html.append("                            <div class=\"col-12\">\n");
        html.append("                                <div class=\"card border-").append(colorClass).append("\">\n");
        html.append("                                    <div class=\"card-header bg-").append(colorClass)
                .append(" text-white\">\n");
        html.append("                                        <h5 class=\"mb-0\">\n");
        html.append(
                "                                            <i class=\"fas fa-chart-bar me-2\"></i>Métricas de Similitud\n");
        html.append("                                            <span class=\"badge bg-light text-dark ms-2\">")
                .append(riskLevel).append("</span>\n");
        html.append("                                        </h5>\n");
        html.append("                                    </div>\n");
        html.append("                                    <div class=\"card-body\">\n");
        html.append("                                        <div class=\"row\">\n");
        html.append("                                            <div class=\"col-md-3\">\n");
        html.append("                                                <div class=\"metric-card text-center\">\n");
        html.append("                                                    <h3 class=\"text-").append(colorClass)
                .append("\">").append(String.format("%.1f%%", avgSimilarity * 100)).append("</h3>\n");
        html.append(
                "                                                    <p class=\"mb-0\"><strong>Similitud Promedio</strong></p>\n");
        html.append("                                                </div>\n");
        html.append("                                            </div>\n");
        html.append("                                            <div class=\"col-md-3\">\n");
        html.append("                                                <div class=\"metric-card text-center\">\n");
        html.append("                                                    <h3 class=\"text-").append(colorClass)
                .append("\">").append(String.format("%.1f%%", maxSimilarity * 100)).append("</h3>\n");
        html.append(
                "                                                    <p class=\"mb-0\"><strong>Similitud Máxima</strong></p>\n");
        html.append("                                                </div>\n");
        html.append("                                            </div>\n");
        html.append("                                            <div class=\"col-md-3\">\n");
        html.append("                                                <div class=\"metric-card text-center\">\n");
        html.append("                                                    <h3 class=\"text-secondary\">")
                .append(String.format("%.0f", maxLength)).append("</h3>\n");
        html.append(
                "                                                    <p class=\"mb-0\"><strong>Longitud Máxima</strong></p>\n");
        html.append("                                                </div>\n");
        html.append("                                            </div>\n");
        html.append("                                            <div class=\"col-md-3\">\n");
        html.append("                                                <div class=\"metric-card text-center\">\n");
        html.append("                                                    <h3 class=\"text-secondary\">")
                .append(String.format("%.0f", longestMatch)).append("</h3>\n");
        html.append(
                "                                                    <p class=\"mb-0\"><strong>Coincidencia Más Larga</strong></p>\n");
        html.append("                                                </div>\n");
        html.append("                                            </div>\n");
        html.append("                                        </div>\n");
        html.append("                                    </div>\n");
        html.append("                                </div>\n");
        html.append("                            </div>\n");
        html.append("                        </div>\n");

        return html.toString();
    }

    /**
     * Genera la lista de coincidencias encontradas
     */
    private String generateMatchesList(JsonNode matches) {
        StringBuilder html = new StringBuilder();

        html.append("                        <div class=\"row\">\n");
        html.append("                            <div class=\"col-12\">\n");
        html.append("                                <div class=\"card\">\n");
        html.append("                                    <div class=\"card-header bg-secondary text-white\">\n");
        html.append("                                        <h5 class=\"mb-0\">\n");
        html.append(
                "                                            <i class=\"fas fa-list me-2\"></i>Coincidencias Encontradas\n");
        html.append("                                            <span class=\"badge bg-light text-dark ms-2\">")
                .append(matches.size()).append(" coincidencias</span>\n");
        html.append("                                        </h5>\n");
        html.append("                                    </div>\n");
        html.append("                                    <div class=\"card-body\">\n");

        if (matches.size() == 0) {
            html.append(
                    "                                        <p class=\"text-muted text-center\">No se encontraron coincidencias.</p>\n");
        } else {
            html.append("                                        <div class=\"table-responsive\">\n");
            html.append(
                    "                                            <table class=\"table table-striped table-hover\">\n");
            html.append("                                                <thead class=\"table-dark\">\n");
            html.append("                                                    <tr>\n");
            html.append("                                                        <th width=\"35%\">Archivo 1</th>\n");
            html.append("                                                        <th width=\"35%\">Archivo 2</th>\n");
            html.append("                                                        <th width=\"15%\">Líneas 1</th>\n");
            html.append("                                                        <th width=\"15%\">Líneas 2</th>\n");
            html.append("                                                    </tr>\n");
            html.append("                                                </thead>\n");
            html.append("                                                <tbody>\n");

            for (JsonNode match : matches) {
                String fileName1 = match.get("firstFileName").asText();
                String fileName2 = match.get("secondFileName").asText();
                int startLine1 = match.get("startInFirst").get("line").asInt();
                int endLine1 = match.get("endInFirst").get("line").asInt();
                int startLine2 = match.get("startInSecond").get("line").asInt();
                int endLine2 = match.get("endInSecond").get("line").asInt();

                html.append("                                                    <tr>\n");
                html.append("                                                        <td><small class=\"text-break\">")
                        .append(getShortFileName(fileName1)).append("</small></td>\n");
                html.append("                                                        <td><small class=\"text-break\">")
                        .append(getShortFileName(fileName2)).append("</small></td>\n");
                html.append(
                        "                                                        <td><span class=\"badge bg-primary\">")
                        .append(startLine1).append("-").append(endLine1).append("</span></td>\n");
                html.append(
                        "                                                        <td><span class=\"badge bg-info\">")
                        .append(startLine2).append("-").append(endLine2).append("</span></td>\n");
                html.append("                                                    </tr>\n");
            }

            html.append("                                                </tbody>\n");
            html.append("                                            </table>\n");
            html.append("                                        </div>\n");
        }

        html.append("                                    </div>\n");
        html.append("                                </div>\n");
        html.append("                            </div>\n");
        html.append("                        </div>\n");

        return html.toString();
    }

    /**
     * Obtiene el CSS personalizado para el HTML
     */
    private String getCustomCss() {
        return """
                    .metric-card {
                        background: #f8f9fa;
                        border-radius: 10px;
                        padding: 15px;
                        margin-bottom: 10px;
                    }

                    .metric-card h3 {
                        margin-bottom: 5px;
                        font-weight: bold;
                    }

                    .text-break {
                        word-break: break-all;
                    }

                    .table-hover tbody tr:hover {
                        background-color: rgba(0,123,255,.075);
                    }

                    .badge {
                        font-size: 0.8em;
                    }

                    .card {
                        border-radius: 10px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }

                    .card-header {
                        border-radius: 10px 10px 0 0;
                    }
                """;
    }

    /**
     * Obtiene el JavaScript personalizado para el HTML
     */
    private String getCustomJavaScript() {
        return """
                    // Funcionalidad para tooltips de Bootstrap
                    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
                    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
                        return new bootstrap.Tooltip(tooltipTriggerEl)
                    });

                    // Funcionalidad para copiar al portapapeles
                    function copyToClipboard(text) {
                        navigator.clipboard.writeText(text).then(function() {
                            console.log('Texto copiado al portapapeles');
                        }, function(err) {
                            console.error('Error al copiar texto: ', err);
                        });
                    }
                """;
    }

    /**
     * Utilities methods
     */
    private String extractTeamFromId(String submissionId) {
        if (submissionId.contains("team_")) {
            return submissionId.substring(submissionId.lastIndexOf("team_"));
        }
        return "Unknown Team";
    }

    private String getSimilarityColorClass(double similarity) {
        if (similarity >= 0.8)
            return "danger";
        if (similarity >= 0.5)
            return "warning";
        if (similarity >= 0.3)
            return "info";
        return "success";
    }

    private String getSimilarityRiskLevel(double similarity) {
        if (similarity >= 0.8)
            return "Alto Riesgo";
        if (similarity >= 0.5)
            return "Riesgo Medio";
        if (similarity >= 0.3)
            return "Riesgo Bajo";
        return "Sin Riesgo";
    }

    private String getShortFileName(String fullPath) {
        if (fullPath.contains("\\")) {
            String[] parts = fullPath.split("\\\\");
            return parts[parts.length - 1];
        } else if (fullPath.contains("/")) {
            String[] parts = fullPath.split("/");
            return parts[parts.length - 1];
        }
        return fullPath;
    }

    /**
     * Extrae el ID numérico de un nombre de directorio de submission
     * Ejemplo: "submission_19_team_17" -> "19"
     */
    private String extractNumericId(String submissionName) {
        if (submissionName == null) {
            return "unknown";
        }

        // Buscar patrón submission_XX_team_YY
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("submission_(\\d+)_team_\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(submissionName);

        if (matcher.find()) {
            return matcher.group(1); // Retorna el primer grupo capturado (el ID de submission)
        }

        // Si no encuentra el patrón, intentar extraer cualquier número
        pattern = java.util.regex.Pattern.compile("(\\d+)");
        matcher = pattern.matcher(submissionName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Si no encuentra números, retornar el nombre original
        return submissionName;
    }
}
