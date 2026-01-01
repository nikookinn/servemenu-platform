package com.servemenu.mediaservice.application.dto.request;

import com.servemenu.mediaservice.domain.enums.MediaType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Request DTO for media upload
 */
@Data
public class MediaUploadRequest {
    
    @NotNull(message = "File is required")
    private MultipartFile file;
    
    @NotNull(message = "Media type is required")
    private MediaType mediaType;
    
    @NotNull(message = "Entity ID is required")
    private UUID entityId;
    
    private UUID uploadedBy;
}
