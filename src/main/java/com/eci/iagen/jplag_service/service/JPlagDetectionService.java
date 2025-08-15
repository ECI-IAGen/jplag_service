package com.eci.iagen.jplag_service.service;

import com.eci.iagen.jplag_service.dto.PlagiarismDetectionRequest;
import com.eci.iagen.jplag_service.dto.PlagiarismDetectionResponse;
import com.eci.iagen.jplag_service.dto.SubmissionDto;
import com.eci.iagen.jplag_service.service.comparison.ComparisonHtmlGenerator;
import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.options.JPlagOptions;
import de.jplag.java.JavaLanguage;
import de.jplag.reporting.reportobject.ReportObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;

/**
 * Servicio principal para la detección de plagio usando JPlag
 */
@Service
public class JPlagDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(JPlagDetectionService.class);

    @Autowired
    private GitService gitService;

    @Autowired
    private ComparisonHtmlGenerator comparisonHtmlGenerator;

    @Value("${jplag.temp-directory:./temp}")
    private String tempDirectory;

    @Value("${jplag.reports-directory:./reports}")
    private String reportsDirectory;

    @Value("${jplag.minimum.similarity:0.0}")
    private double minimumSimilarity;

    /**
     * Detecta plagio entre las entregas proporcionadas
     */
    public PlagiarismDetectionResponse detectPlagiarism(PlagiarismDetectionRequest request) {
        logger.info("Starting plagiarism detection for assignment: {} with {} submissions",
                request.getAssignmentId(), request.getSubmissions().size());

        PlagiarismDetectionResponse response = new PlagiarismDetectionResponse();
        response.setAssignmentId(request.getAssignmentId());
        response.setAssignmentTitle(request.getAssignmentTitle());

        try {
            // Validar que hay suficientes entregas
            if (request.getSubmissions().size() < 2) {
                response.setSuccess(false);
                response.setMessage("Se requieren al menos 2 entregas para detectar plagio");
                response.setComparisons(new ArrayList<>());
                return response;
            }

            // Crear directorio temporal único para esta detección
            String sessionId = UUID.randomUUID().toString();
            Path sessionTempDir = createSessionDirectory(sessionId);

            // Clonar repositorios
            List<File> clonedRepositories = cloneRepositories(request.getSubmissions(), sessionTempDir);

            if (clonedRepositories.size() < 2) {
                response.setSuccess(false);
                response.setMessage("No se pudieron clonar suficientes repositorios válidos");
                response.setComparisons(new ArrayList<>());
                return response;
            }

            // Ejecutar JPlag
            JPlagResult jplagResult = runJPlagAnalysis(clonedRepositories);

            // Generar reporte HTML usando JPlag nativo
            String reportUrl = saveReportBundle(jplagResult, sessionId);

            // Convertir resultados a formato de respuesta
            List<PlagiarismDetectionResponse.ComparisonResult> comparisons = convertJPlagResultToComparisons(
                    jplagResult, request.getSubmissions(), sessionId);

            // Calcular estadísticas
            PlagiarismDetectionResponse.Statistics statistics = calculateStatistics(comparisons,
                    request.getSubmissions().size());

            response.setSuccess(true);
            response.setMessage("Análisis de plagio completado exitosamente");
            response.setComparisons(comparisons);
            response.setReportUrl(reportUrl);
            response.setStatistics(statistics);

            logger.info("Plagiarism detection completed successfully for assignment: {}", request.getAssignmentId());

            // Limpiar directorios temporales
            cleanupTemporaryDirectories(sessionTempDir);

        } catch (Exception e) {
            logger.error("Error during plagiarism detection for assignment: {}", request.getAssignmentId(), e);
            response.setSuccess(false);
            response.setMessage("Error durante el análisis de plagio: " + e.getMessage());
            response.setComparisons(new ArrayList<>());
        }

        return response;
    }

    /**
     * Crea un directorio temporal único para esta sesión
     */
    private Path createSessionDirectory(String sessionId) throws IOException {
        Path sessionDir = Paths.get(tempDirectory, sessionId);
        Files.createDirectories(sessionDir);
        logger.info("Created session directory: {}", sessionDir);
        return sessionDir;
    }

    /**
     * Clona todos los repositorios de las entregas
     */
    private List<File> cloneRepositories(List<SubmissionDto> submissions, Path sessionTempDir) {
        List<File> clonedRepos = new ArrayList<>();

        for (SubmissionDto submission : submissions) {
            try {
                if (!gitService.isValidGitUrl(submission.getRepositoryUrl())) {
                    logger.warn("Invalid Git URL for submission {}: {}",
                            submission.getSubmissionId(), submission.getRepositoryUrl());
                    continue;
                }

                String repoName = "submission_" + submission.getSubmissionId() + "_team_" + submission.getTeamId();
                Path repoPath = sessionTempDir.resolve(repoName);

                if (gitService.cloneRepository(submission.getRepositoryUrl(), repoPath)) {
                    clonedRepos.add(repoPath.toFile());
                    logger.info("Successfully cloned repository for submission: {}", submission.getSubmissionId());

                    // Validar que el repositorio tiene archivos Java
                    long javaFileCount = countJavaFiles(repoPath);
                    if (javaFileCount == 0) {
                        logger.warn("Repository for submission {} contains no Java files",
                                submission.getSubmissionId());
                    } else {
                        logger.info("Repository for submission {} contains {} Java files",
                                submission.getSubmissionId(), javaFileCount);
                    }
                } else {
                    logger.warn("Failed to clone repository for submission: {}", submission.getSubmissionId());
                }

            } catch (Exception e) {
                logger.error("Error cloning repository for submission {}: {}",
                        submission.getSubmissionId(), e.getMessage());
            }
        }

        logger.info("Successfully cloned {} out of {} repositories", clonedRepos.size(), submissions.size());
        return clonedRepos;
    }

    /**
     * Crea un directorio root para el análisis con estructura que JPlag entiende
     */
    private Path createAnalysisRootDirectory(List<File> clonedRepositories) throws IOException {
        // Crear directorio temporal para el análisis
        Path analysisRoot = Paths.get(tempDirectory, "analysis_" + UUID.randomUUID().toString());
        Files.createDirectories(analysisRoot);

        logger.info("Creating analysis root directory: {}", analysisRoot);

        // Copiar cada repositorio como un subdirectorio del root
        for (File repository : clonedRepositories) {
            if (!repository.exists() || !repository.isDirectory()) {
                logger.warn("Skipping invalid repository: {}", repository.getAbsolutePath());
                continue;
            }

            // Crear subdirectorio en el root de análisis
            Path submissionDir = analysisRoot.resolve(repository.getName());

            // Copiar solo archivos .java del repositorio
            copyJavaFilesRecursively(repository.toPath(), submissionDir);

            // Verificar que se copiaron archivos
            long javaFileCount = countJavaFiles(submissionDir);
            logger.info("Submission '{}' - copied {} Java files", repository.getName(), javaFileCount);

            if (javaFileCount == 0) {
                logger.warn("No Java files found in submission: {}", repository.getName());
            }
        }

        // Verificar estructura final
        logAnalysisStructure(analysisRoot);

        return analysisRoot;
    }

    /**
     * Copia recursivamente solo archivos .java manteniendo la estructura de
     * directorios
     */
    private void copyJavaFilesRecursively(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        Files.walk(source)
                .forEach(sourcePath -> {
                    try {
                        Path relativePath = source.relativize(sourcePath);
                        Path targetPath = target.resolve(relativePath);

                        if (Files.isDirectory(sourcePath)) {
                            // Saltar directorios que no necesitamos
                            String dirName = sourcePath.getFileName().toString().toLowerCase();
                            if (!dirName.equals(".git") && !dirName.equals("target") &&
                                    !dirName.equals("build") && !dirName.equals("node_modules") &&
                                    !dirName.equals(".idea") && !dirName.equals("out")) {
                                Files.createDirectories(targetPath);
                            }
                        } else if (sourcePath.toString().toLowerCase().endsWith(".java")) {
                            // Copiar solo archivos .java
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to copy file from {} to {}: {}", sourcePath, target, e.getMessage());
                    }
                });
    }

    /**
     * Cuenta archivos .java en un directorio
     */
    private long countJavaFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }

        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".java"))
                .count();
    }

    /**
     * Log de la estructura del directorio de análisis para debugging
     */
    private void logAnalysisStructure(Path analysisRoot) {
        try {
            logger.info("Analysis directory structure:");
            Files.walk(analysisRoot)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            if (dir.equals(analysisRoot)) {
                                logger.info("  📁 {} (root)", analysisRoot.getFileName());
                            } else {
                                Path relativePath = analysisRoot.relativize(dir);
                                long javaFiles = countJavaFiles(dir);
                                logger.info("    📁 {} ({} Java files)", relativePath, javaFiles);
                            }
                        } catch (IOException e) {
                            logger.warn("Error checking directory {}: {}", dir, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.warn("Error logging analysis structure: {}", e.getMessage());
        }
    }

    /**
     * Método recursivo para buscar archivos .java
     */
    private void findJavaFilesRecursive(File directory, List<File> javaFiles) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Saltar directorios comunes que no contienen código fuente
                String dirName = file.getName().toLowerCase();
                if (!dirName.equals(".git") && !dirName.equals("target") &&
                        !dirName.equals("build") && !dirName.equals("node_modules") &&
                        !dirName.equals(".idea") && !dirName.equals("out")) {
                    findJavaFilesRecursive(file, javaFiles);
                }
            } else if (file.getName().toLowerCase().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }

    /**
     * Ejecuta el análisis de JPlag
     */
    private JPlagResult runJPlagAnalysis(List<File> repositories) throws Exception {
        logger.info("Starting JPlag analysis with {} repositories", repositories.size());

        try {
            // Crear un directorio root temporal que contenga todos los submissions
            Path analysisRootDir = createAnalysisRootDirectory(repositories);

            // Configurar opciones de JPlag para Java
            JavaLanguage javaLanguage = new JavaLanguage();

            // Configurar las opciones del lenguaje Java
            var languageOptions = javaLanguage.getOptions();
            // Puedes configurar opciones específicas de Java aquí si es necesario

            // Usar solo el directorio root - JPlag encontrará automáticamente los
            // subdirectorios
            Set<File> rootDirectories = Set.of(analysisRootDir.toFile());

            logger.info("Analysis root directory: {}", analysisRootDir);
            logger.info("Submissions found: {}",
                    repositories.stream().map(File::getName).toArray());

            // Crear opciones de JPlag
            int minTokenMatch = 12;
            JPlagOptions options = new JPlagOptions(javaLanguage, rootDirectories, Set.of())
                    .withMinimumTokenMatch(minTokenMatch) // Mínimo 1 token
                    .withFileSuffixes(List.of(".java")) // Solo archivos .java
                    .withSimilarityThreshold(minimumSimilarity) // Detectar todas las similitudes
                    .withMaximumNumberOfComparisons(-1); // Sin límite de comparaciones

            // Log de configuración
            logger.info("JPlag configuration:");
            logger.info("  - Minimum tokens: {}", options.minimumTokenMatch());
            logger.info("  - File suffixes: {}", options.fileSuffixes());
            logger.info("  - Similarity threshold: {}", options.similarityThreshold());

            // Ejecutar JPlag
            JPlagResult result = JPlag.run(options);

            logger.info("JPlag analysis completed successfully");
            logger.info("  - Total submissions processed: {}", result.getSubmissions().getSubmissions().size());
            logger.info("  - Total comparisons generated: {}", result.getAllComparisons().size());

            // Log detallado de las comparisons encontradas
            var comparisons = new ArrayList<>(result.getAllComparisons());
            comparisons.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

            logger.info("Top similarities found:");
            int logCount = Math.min(10, comparisons.size());
            for (int i = 0; i < logCount; i++) {
                JPlagComparison comparison = comparisons.get(i);
                logger.info("  {}. {} vs {} - Similarity: {}% - Tokens: {}",
                        (i + 1),
                        comparison.firstSubmission().getName(),
                        comparison.secondSubmission().getName(),
                        comparison.similarity() * 100,
                        comparison.getNumberOfMatchedTokens());
            }

            return result;
        } catch (Exception e) {
            logger.error("Error running JPlag analysis: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Genera el reporte HTML usando la funcionalidad nativa de JPlag
     */
    private String saveReportBundle(JPlagResult jplagResult, String sessionId) throws IOException {
        Path reportsDir = Paths.get(reportsDirectory).toAbsolutePath().normalize();
        Files.createDirectories(reportsDir);

        Path zipPath = reportsDir.resolve("report_" + sessionId + ".zip");
        try {
            ReportObjectFactory reportFactory = new ReportObjectFactory(zipPath.toFile());
            reportFactory.createAndSaveReport(jplagResult);
        } catch (Exception e) {
            logger.error("Error generating JPlag report ZIP: {}", e.getMessage(), e);
            throw new IOException("Failed to generate JPlag HTML report", e);
        }

        Path reportDir = reportsDir.resolve("report_" + sessionId);
        unzip(zipPath, reportDir);

        // Verificación y log útil
        Path index = reportDir.resolve("index.html");
        logger.info("JPlag HTML report unzipped at: {}", reportDir);
        logger.info("Report index exists? {} -> {}", Files.exists(index), index);

        // Generar archivos HTML individuales de comparación
        try {
            comparisonHtmlGenerator.generateComparisonHtmlFiles(reportDir, sessionId);
            logger.info("Generated individual comparison HTML files for session: {}", sessionId);
        } catch (Exception e) {
            logger.warn("Failed to generate individual comparison HTML files for session {}: {}", sessionId, e.getMessage());
        }

        // (Opcional) borrar el zip
        try {
            Files.deleteIfExists(zipPath);
        } catch (IOException ignore) {
        }

        return "/reports/viewer/" + sessionId;
    }

    private void unzip(Path zip, Path dest) throws IOException {
        Path destAbs = dest.toAbsolutePath().normalize(); // <— base absoluta
        Files.createDirectories(destAbs);

        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // Normaliza separadores y quita slashes iniciales (posibles en algunos zips)
                name = name.replace('\\', '/');
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }

                // Bloquea traversal explícito
                if (name.contains("..")) {
                    throw new IOException("Unsafe entry name: " + name);
                }

                Path out = destAbs.resolve(name).normalize(); // <— resuelve contra destAbs
                if (!out.startsWith(destAbs)) {
                    throw new IOException("Zip Slip blocked: " + name);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Convierte los resultados de JPlag al formato de respuesta
     */
    private List<PlagiarismDetectionResponse.ComparisonResult> convertJPlagResultToComparisons(
            JPlagResult jplagResult, List<SubmissionDto> originalSubmissions, String sessionId) {

        List<PlagiarismDetectionResponse.ComparisonResult> comparisons = new ArrayList<>();

        for (JPlagComparison comparison : jplagResult.getAllComparisons()) {
            try {
                // Extraer información de las entregas comparadas usando la API correcta
                String submission1Name = comparison.firstSubmission().getName();
                String submission2Name = comparison.secondSubmission().getName();

                // Buscar información de equipos basada en los nombres de directorios
                SubmissionDto team1Info = findSubmissionByDirectoryName(originalSubmissions, submission1Name);
                SubmissionDto team2Info = findSubmissionByDirectoryName(originalSubmissions, submission2Name);

                // Debug: log team info
                logger.info("Team info for comparison: {} -> {}, {} -> {}", 
                           submission1Name, team1Info != null ? team1Info.getSubmissionId() : "null",
                           submission2Name, team2Info != null ? team2Info.getSubmissionId() : "null");

                // Construir URL del HTML de comparación individual
                String comparisonHtmlUrl = null;
                String submissionId1 = null;
                String submissionId2 = null;
                
                if (team1Info != null && team2Info != null) {
                    submissionId1 = String.valueOf(team1Info.getSubmissionId());
                    submissionId2 = String.valueOf(team2Info.getSubmissionId());
                } else {
                    // Si no encontramos team info, extraer IDs de los nombres de directorio
                    submissionId1 = extractSubmissionIdFromDirectoryName(submission1Name);
                    submissionId2 = extractSubmissionIdFromDirectoryName(submission2Name);
                    logger.warn("Using extracted IDs for comparison: {} -> {}, {} -> {}", 
                               submission1Name, submissionId1, submission2Name, submissionId2);
                }
                
                // Siempre construir la URL si tenemos IDs
                if (submissionId1 != null && submissionId2 != null) {
                    comparisonHtmlUrl = "/reports/comparison/" + sessionId + "/" + submissionId1 + "-" + submissionId2 + ".html";
                    logger.info("Generated comparison HTML URL: {}", comparisonHtmlUrl);
                }

                PlagiarismDetectionResponse.ComparisonResult result = new PlagiarismDetectionResponse.ComparisonResult(
                        submission1Name,
                        submission2Name,
                        team1Info != null ? team1Info.getTeamName() : "Unknown Team",
                        team2Info != null ? team2Info.getTeamName() : "Unknown Team",
                        comparison.similarity(),
                        comparison.getNumberOfMatchedTokens(),
                        "completed",
                        comparisonHtmlUrl,
                        team1Info != null ? team1Info.getSubmissionId() : null,
                        team2Info != null ? team2Info.getSubmissionId() : null);

                comparisons.add(result);

            } catch (Exception e) {
                logger.warn("Error processing comparison: {}", e.getMessage());
            }
        }

        // Ordenar por similaridad descendente
        comparisons.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        logger.info("Converted {} JPlag comparisons to response format", comparisons.size());
        return comparisons;
    }

    /**
     * Busca una entrega por el nombre del directorio
     */
    private SubmissionDto findSubmissionByDirectoryName(List<SubmissionDto> submissions, String directoryName) {
        for (SubmissionDto submission : submissions) {
            String expectedName = "submission_" + submission.getSubmissionId() + "_team_" + submission.getTeamId();
            if (directoryName.contains(expectedName) || expectedName.contains(directoryName)) {
                return submission;
            }
        }
        return null;
    }

    /**
     * Calcula estadísticas del análisis
     */
    private PlagiarismDetectionResponse.Statistics calculateStatistics(
            List<PlagiarismDetectionResponse.ComparisonResult> comparisons, int totalSubmissions) {

        if (comparisons.isEmpty()) {
            return new PlagiarismDetectionResponse.Statistics(totalSubmissions, 0, 0.0, 0.0, 0.0);
        }

        double sum = comparisons.stream().mapToDouble(PlagiarismDetectionResponse.ComparisonResult::getSimilarity)
                .sum();
        double average = sum / comparisons.size();
        double max = comparisons.stream().mapToDouble(PlagiarismDetectionResponse.ComparisonResult::getSimilarity).max()
                .orElse(0.0);
        double min = comparisons.stream().mapToDouble(PlagiarismDetectionResponse.ComparisonResult::getSimilarity).min()
                .orElse(0.0);

        return new PlagiarismDetectionResponse.Statistics(
                totalSubmissions,
                comparisons.size(),
                Math.round(average * 100.0) / 100.0,
                Math.round(max * 100.0) / 100.0,
                Math.round(min * 100.0) / 100.0);
    }

    /**
     * Limpia los directorios temporales después del análisis
     */
    private void cleanupTemporaryDirectories(Path sessionDir) {
        try {
            if (Files.exists(sessionDir)) {
                // Eliminar recursivamente el directorio de sesión
                Files.walk(sessionDir)
                        .sorted((a, b) -> b.compareTo(a)) // Orden inverso para eliminar archivos antes que directorios
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.warn("Could not delete temporary file: {}", path);
                            }
                        });
                logger.info("Cleaned up temporary directory: {}", sessionDir);
            }
        } catch (Exception e) {
            logger.warn("Error during cleanup of temporary directory {}: {}", sessionDir, e.getMessage());
        }

        // También limpiar directorios de análisis temporales
        try {
            Path tempDir = Paths.get(tempDirectory);
            if (Files.exists(tempDir)) {
                Files.list(tempDir)
                        .filter(path -> Files.isDirectory(path) &&
                                path.getFileName().toString().startsWith("analysis_"))
                        .forEach(this::cleanupSingleDirectory);
            }
        } catch (Exception e) {
            logger.warn("Error during cleanup of analysis directories: {}", e.getMessage());
        }
    }

    /**
     * Limpia un directorio individual
     */
    private void cleanupSingleDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.debug("Could not delete temporary file: {}", path);
                        }
                    });
        } catch (Exception e) {
            logger.debug("Error cleaning up directory {}: {}", directory, e.getMessage());
        }
    }

    /**
     * Extrae el ID de submission de un nombre de directorio
     * Ejemplo: "submission_19_team_17" -> "19"
     */
    private String extractSubmissionIdFromDirectoryName(String directoryName) {
        if (directoryName == null) {
            return null;
        }
        
        // Buscar patrón submission_XX_team_YY
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("submission_(\\d+)_team_\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(directoryName);
        
        if (matcher.find()) {
            return matcher.group(1); // Retorna el primer grupo capturado (el ID de submission)
        }
        
        // Si no encuentra el patrón, intentar extraer cualquier número
        pattern = java.util.regex.Pattern.compile("(\\d+)");
        matcher = pattern.matcher(directoryName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        logger.warn("Could not extract submission ID from directory name: {}", directoryName);
        return null;
    }
}