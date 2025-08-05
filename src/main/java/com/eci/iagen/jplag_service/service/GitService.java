package com.eci.iagen.jplag_service.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

/**
 * Servicio para clonar repositorios Git
 */
@Service
public class GitService {

    private static final Logger logger = LoggerFactory.getLogger(GitService.class);

    /**
     * Clona un repositorio Git en el directorio especificado
     */
    public boolean cloneRepository(String repositoryUrl, Path targetPath) {
        try {
            logger.info("Cloning repository: {} to {}", repositoryUrl, targetPath);

            File targetDir = targetPath.toFile();
            if (targetDir.exists()) {
                logger.warn("Target directory already exists: {}", targetPath);
                return false;
            }

            Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(targetDir)
                    .call();

            logger.info("Successfully cloned repository: {}", repositoryUrl);
            return true;

        } catch (GitAPIException e) {
            logger.error("Failed to clone repository: {} - {}", repositoryUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Valida si una URL es un repositorio Git válido
     */
    public boolean isValidGitUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase().trim();
        return lowerUrl.startsWith("https://github.com/") ||
                lowerUrl.startsWith("https://gitlab.com/") ||
                lowerUrl.startsWith("https://bitbucket.org/") ||
                lowerUrl.endsWith(".git");
    }
}
