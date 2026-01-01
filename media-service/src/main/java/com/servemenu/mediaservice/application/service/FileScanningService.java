package com.servemenu.mediaservice.application.service;

import com.servemenu.mediaservice.infrastructure.exception.InvalidMediaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for scanning uploaded files for malware using ClamAV
 * 
 * Features:
 * - Real-time malware detection
 * - Virus signature scanning
 * - Prevents malicious file uploads
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@Service
public class FileScanningService {
    
    private final ClamavClient clamavClient;
    
    @Value("${clamav.enabled:true}")
    private boolean scanningEnabled;
    
    public FileScanningService(
            @Value("${clamav.host:localhost}") String clamavHost,
            @Value("${clamav.port:3310}") int clamavPort) {
        
        this.clamavClient = new ClamavClient(clamavHost, clamavPort);
        log.info("ClamAV client initialized: {}:{}", clamavHost, clamavPort);
    }
    
    /**
     * Scan file for malware
     * 
     * @param file File to scan
     * @throws InvalidMediaException if malware detected
     * @throws IOException if scanning fails
     */
    public void scanFile(MultipartFile file) throws IOException {
        if (!scanningEnabled) {
            log.debug("File scanning disabled, skipping scan for: {}", file.getOriginalFilename());
            return;
        }
        
        log.debug("Scanning file for malware: {}", file.getOriginalFilename());
        
        try {
            // Ping ClamAV to check if it's available
            try {
                clamavClient.ping();
                log.debug("ClamAV is responding");
            } catch (Exception e) {
                log.warn("ClamAV is not responding, skipping scan: {}", e.getMessage());
                return;
            }
            
            // Scan file using InputStream
            try (InputStream inputStream = file.getInputStream()) {
                ScanResult result = clamavClient.scan(inputStream);
                
                // Check result
                if (result instanceof ScanResult.OK) {
                    log.debug("File scan passed: {}", file.getOriginalFilename());
                } else if (result instanceof ScanResult.VirusFound virusFound) {
                    log.error("Malware detected in file: {} - Virus: {}",
                        file.getOriginalFilename(), virusFound.getFoundViruses());
                    throw new InvalidMediaException(
                        "File contains malware: " + virusFound.getFoundViruses());
                } else {
                    log.warn("Unknown scan result for file: {}", file.getOriginalFilename());
                }
            }
            
        } catch (InvalidMediaException e) {
            throw e; // Re-throw malware exception
        } catch (Exception e) {
            log.error("File scanning failed for: {} - Error: {}", 
                file.getOriginalFilename(), e.getMessage(), e);
            // Don't block upload if scanning fails (graceful degradation)
            // In production, you might want to reject the file instead
        }
    }
    
    /**
     * Check if ClamAV is available
     * 
     * @return true if ClamAV is responding
     */
    public boolean isAvailable() {
        try {
            clamavClient.ping();
            return true;
        } catch (Exception e) {
            log.error("Failed to ping ClamAV: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get ClamAV version
     * 
     * @return ClamAV version string
     */
    public String getVersion() {
        try {
            return clamavClient.version();
        } catch (Exception e) {
            log.error("Failed to get ClamAV version: {}", e.getMessage());
            return "Unknown";
        }
    }
}
