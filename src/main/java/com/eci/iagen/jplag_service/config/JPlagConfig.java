package com.eci.iagen.jplag_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para JPlag
 */
@Configuration
@ConfigurationProperties(prefix = "jplag")
public class JPlagConfig {

    private String tempDirectory = "temp/jplag";
    private double minSimilarityThreshold = 0.10;
    private int maxSubmissions = 100;
    private String language = "java";

    // Getters y Setters
    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public double getMinSimilarityThreshold() {
        return minSimilarityThreshold;
    }

    public void setMinSimilarityThreshold(double minSimilarityThreshold) {
        this.minSimilarityThreshold = minSimilarityThreshold;
    }

    public int getMaxSubmissions() {
        return maxSubmissions;
    }

    public void setMaxSubmissions(int maxSubmissions) {
        this.maxSubmissions = maxSubmissions;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
