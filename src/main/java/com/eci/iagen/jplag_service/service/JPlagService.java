package com.eci.iagen.jplag_service.service;

import com.eci.iagen.jplag_service.dto.request.PlagiarismRequest;
import com.eci.iagen.jplag_service.dto.request.SubmissionInfo;
import com.eci.iagen.jplag_service.dto.response.ComparisonResult;
import com.eci.iagen.jplag_service.dto.response.PlagiarismResponse;
import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.Language;
import de.jplag.java.JavaLanguage;
import de.jplag.options.JPlagOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Servicio principal para análisis de plagio usando JPlag
 */
@Service
public class JPlagService {

    private static final Logger logger = LoggerFactory.getLogger(JPlagService.class);

    @Autowired
    private GitService gitService;

    /**
     * Extrae el submission ID de nombres de directorio como "submission_16\src" o
     * "submission_16"
     */
    private Long extractSubmissionId(String directoryName) {
        try {
            // Casos posibles:
            // "submission_16" -> 16
            // "submission_16\src" -> 16
            // "submission_16/src" -> 16

            String cleanName = directoryName;

            // Si contiene separadores de ruta, tomar solo la primera parte
            if (cleanName.contains("\\")) {
                cleanName = cleanName.split("\\\\")[0];
            } else if (cleanName.contains("/")) {
                cleanName = cleanName.split("/")[0];
            }

            // Extraer el número del nombre como "submission_16"
            if (cleanName.startsWith("submission_")) {
                String idStr = cleanName.replace("submission_", "");
                return Long.parseLong(idStr);
            }

            return null;
        } catch (NumberFormatException e) {
            logger.warn("Could not parse submission ID from directory name: {}", directoryName);
            return null;
        }
    }

    /**
     * Analiza todas las entregas y detecta similitudes
     */
    public PlagiarismResponse analyzePlagiarism(PlagiarismRequest request) throws IOException {
        logger.info("Starting plagiarism analysis for assignment: {}", request.getAssignmentId());

        // 1. Crear directorio temporal único
        Path tempDir = Files.createTempDirectory("jplag-analysis-");

        try {
            // 2. Clonar todos los repositorios
            Map<Long, Path> clonedRepos = cloneRepositories(request.getSubmissions(), tempDir);

            if (clonedRepos.size() < 2) {
                logger.warn("Only {} repositories cloned successfully, need at least 2 for comparison",
                        clonedRepos.size());
                return new PlagiarismResponse(request.getAssignmentId(), request.getAssignmentTitle(),
                        new ArrayList<>());
            }

            // 3. Ejecutar análisis JPlag
            List<ComparisonResult> comparisons = performJPlagAnalysis(clonedRepos);

            // 4. Construir respuesta
            return buildResponse(request, comparisons);

        } finally {
            // 5. Limpiar archivos temporales
            deleteDirectoryRecursively(tempDir.toFile());
        }
    }

    /**
     * Clona todos los repositorios de las entregas
     */
    private Map<Long, Path> cloneRepositories(List<SubmissionInfo> submissions, Path tempDir) {
        Map<Long, Path> clonedRepos = new HashMap<>();

        for (SubmissionInfo submission : submissions) {
            try {
                // Validar URL
                if (!gitService.isValidGitUrl(submission.getRepositoryUrl())) {
                    logger.warn("Invalid Git URL for submission {}: {}",
                            submission.getSubmissionId(), submission.getRepositoryUrl());
                    continue;
                }

                // Crear directorio para esta entrega
                Path repoPath = tempDir.resolve("submission_" + submission.getSubmissionId());

                // Clonar repositorio
                if (gitService.cloneRepository(submission.getRepositoryUrl(), repoPath)) {
                    clonedRepos.put(submission.getSubmissionId(), repoPath);
                    logger.info("Successfully cloned repository for submission {}", submission.getSubmissionId());
                } else {
                    logger.warn("Failed to clone repository for submission {}", submission.getSubmissionId());
                }

            } catch (Exception e) {
                logger.error("Error cloning repository for submission {}: {}",
                        submission.getSubmissionId(), e.getMessage());
            }
        }

        logger.info("Successfully cloned {} out of {} repositories",
                clonedRepos.size(), submissions.size());
        return clonedRepos;
    }

    /**
     * Ejecuta el análisis JPlag sobre los repositorios clonados
     */
    private List<ComparisonResult> performJPlagAnalysis(Map<Long, Path> clonedRepos) {
        List<ComparisonResult> comparisons = new ArrayList<>();

        try {
            // Configurar JPlag para Java
            Language language = new JavaLanguage();

            // Crear lista explícita de directorios válidos
            Set<File> submissionDirectories = new HashSet<>();

            // Solo agregar los directorios que sabemos que existen y son válidos
            for (Map.Entry<Long, Path> entry : clonedRepos.entrySet()) {
                Path repoPath = entry.getValue();
                File repoDir = repoPath.toFile();

                // Verificar que es un directorio válido y no comienza con punto
                if (repoDir.exists() && repoDir.isDirectory() && !repoDir.getName().startsWith(".")) {
                    submissionDirectories.add(repoDir);
                    logger.info("Adding valid submission directory: {}", repoDir.getAbsolutePath());
                } else {
                    logger.warn("Skipping invalid directory: {} (exists: {}, isDir: {}, startsWithDot: {})",
                            repoDir.getAbsolutePath(), repoDir.exists(), repoDir.isDirectory(),
                            repoDir.getName().startsWith("."));
                }
            }

            // Log de los directorios que se van a analizar
            logger.info("=== JPlag Analysis Setup ===");
            logger.info("Total cloned repositories: {}", clonedRepos.size());
            logger.info("Valid submission directories after filtering: {}", submissionDirectories.size());

            // Log detallado de cada directorio
            submissionDirectories.forEach(dir -> {
                logger.info("Final submission directory: {} (exists: {}, isDirectory: {})",
                        dir.getAbsolutePath(), dir.exists(), dir.isDirectory());

                // Verificar que no hay subdirectorios .git
                File[] contents = dir.listFiles();
                if (contents != null) {
                    for (File file : contents) {
                        if (file.getName().startsWith(".")) {
                            logger.info("  Hidden item found (will be ignored by JPlag): {} ({})",
                                    file.getName(), file.isDirectory() ? "DIR" : "FILE");
                        } else {
                            logger.info("  Visible item: {} ({})", file.getName(),
                                    file.isDirectory() ? "DIR" : "FILE");
                        }
                    }
                }
            });

            if (submissionDirectories.size() < 2) {
                logger.error("Only {} valid submission directories after filtering, need at least 2",
                        submissionDirectories.size());
                return comparisons; // Retorna lista vacía
            }

            // Detectar la estructura de directorios más común
            String subdirectory = detectBestSubdirectory(clonedRepos);
            logger.info("Best subdirectory detected: '{}', but will analyze from root to avoid .git issues",
                    subdirectory);

            // NO usar subdirectorio para evitar que JPlag analice carpetas .git
            // JPlag automáticamente encontrará los archivos .java recursivamente
            JPlagOptions options = new JPlagOptions(language, submissionDirectories, Set.of())
                    .withMinimumTokenMatch(1) // Reducir a 1 para detectar incluso pequeñas similitudes
                    .withSimilarityThreshold(0.0); // 0% threshold - shows ALL similarities

            logger.info("JPlag configured to analyze from repository root with 0% threshold and minimum 1 token match");

            // Ejecutar JPlag
            JPlagResult result = JPlag.run(options);
            logger.info("JPlag execution completed. Total comparisons found: {}", result.getAllComparisons().size());

            // CORRECCIÓN: Crear mapeo más robusto de directorio a submission ID
            Map<String, Long> dirToSubmissionId = new HashMap<>();
            for (Map.Entry<Long, Path> entry : clonedRepos.entrySet()) {
                Long submissionId = entry.getKey();
                Path path = entry.getValue();
                String dirName = path.getFileName().toString(); // "submission_123"
                dirToSubmissionId.put(dirName, submissionId);
                logger.info("Mapping: directory '{}' -> submission ID {}", dirName, submissionId);
            }

            logger.info("=== Processing JPlag Results ===");
            logger.info("Directory to submission mapping: {}", dirToSubmissionId);

            // Procesar resultados
            for (JPlagComparison comparison : result.getAllComparisons()) {
                String dir1 = comparison.firstSubmission().getName();
                String dir2 = comparison.secondSubmission().getName();
                double similarity = comparison.similarity();

                logger.info("JPlag comparison found: '{}' vs '{}' = {:.4f} ({:.2f}%)",
                        dir1, dir2, similarity, similarity * 100);

                // CORRECCIÓN: Extraer el ID del directorio padre desde nombres como
                // "submission_16\src"
                Long submissionId1 = extractSubmissionId(dir1);
                Long submissionId2 = extractSubmissionId(dir2);

                logger.info("Mapped to submission IDs: {} vs {} ", submissionId1, submissionId2);

                if (submissionId1 != null && submissionId2 != null) {
                    double similarityPercentage = similarity * 100;

                    ComparisonResult compResult = new ComparisonResult(
                            submissionId1, submissionId2, similarityPercentage);

                    comparisons.add(compResult);

                    logger.info("Added comparison: Submission {} vs {} = {:.2f}%",
                            submissionId1, submissionId2, similarityPercentage);
                } else {
                    logger.warn("Could not extract submission IDs from directories: '{}' -> {}, '{}' -> {}",
                            dir1, submissionId1, dir2, submissionId2);
                }
            }

            logger.info("JPlag analysis completed. Final comparisons count: {}", comparisons.size());

        } catch (Exception e) {
            logger.error("Error during JPlag analysis: {}", e.getMessage(), e);
            throw new RuntimeException("JPlag analysis failed: " + e.getMessage(), e);
        }

        return comparisons;
    }

    /**
     * Construye la respuesta final
     */
    private PlagiarismResponse buildResponse(PlagiarismRequest request,
            List<ComparisonResult> comparisons) {

        return new PlagiarismResponse(
                request.getAssignmentId(),
                request.getAssignmentTitle(),
                comparisons);
    }

    /**
     * Detecta el mejor subdirectorio para análisis basado en los repositorios
     * clonados
     */
    private String detectBestSubdirectory(Map<Long, Path> clonedRepos) {
        Map<String, Integer> subdirectoryCount = new HashMap<>();

        // Analizar cada repositorio clonado
        for (Map.Entry<Long, Path> entry : clonedRepos.entrySet()) {
            Long submissionId = entry.getKey();
            Path repoPath = entry.getValue();

            logger.info("=== Analyzing directory structure for submission {} ===", submissionId);
            logger.info("Repository path: {}", repoPath.toAbsolutePath());
            logger.info("Directory exists: {}", Files.exists(repoPath));
            logger.info("Is directory: {}", Files.isDirectory(repoPath));

            // Listar contenido del directorio raíz (excluyendo archivos/carpetas ocultos)
            try {
                logger.info("Contents of repository root (excluding hidden files):");
                Files.list(repoPath)
                        .filter(path -> !path.getFileName().toString().startsWith("."))
                        .forEach(path -> {
                            logger.info("  - {} ({})", path.getFileName(),
                                    Files.isDirectory(path) ? "DIR" : "FILE");
                        });
            } catch (Exception e) {
                logger.warn("Error listing repository contents: {}", e.getMessage());
            }

            try {
                // Buscar directorios comunes que podrían contener código fuente
                String[] candidateSubdirs = { "src", "src/main/java", "source", "java", "" };

                for (String subdir : candidateSubdirs) {
                    Path fullPath = subdir.isEmpty() ? repoPath : repoPath.resolve(subdir);

                    logger.debug("Checking subdirectory: '{}' -> {}", subdir, fullPath.toAbsolutePath());

                    if (Files.exists(fullPath) && Files.isDirectory(fullPath)) {
                        // Contar archivos .java en este directorio (recursivamente)
                        long javaFileCount = countJavaFiles(fullPath);
                        logger.info("Subdirectory '{}' in submission {} contains {} Java files",
                                subdir, submissionId, javaFileCount);

                        if (javaFileCount > 0) {
                            subdirectoryCount.merge(subdir, (int) javaFileCount, Integer::sum);
                            break; // Usar el primer subdirectorio que contenga archivos Java
                        }
                    } else {
                        logger.debug("Subdirectory '{}' does not exist or is not a directory", subdir);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error analyzing directory structure for submission {}: {}", submissionId, e.getMessage());
            }
        }

        // Seleccionar el subdirectorio con más archivos Java en total
        String bestSubdirectory = subdirectoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");

        logger.info("Directory analysis results: {}", subdirectoryCount);
        logger.info("Selected best subdirectory: '{}'", bestSubdirectory);

        return bestSubdirectory;
    }

    /**
     * Cuenta archivos .java en un directorio recursivamente
     */
    private long countJavaFiles(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".java"))
                    .count();
        } catch (Exception e) {
            logger.warn("Error counting Java files in {}: {}", directory, e.getMessage());
            return 0;
        }
    }

    /**
     * Elimina directorio recursivamente
     */
    private void deleteDirectoryRecursively(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryRecursively(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}