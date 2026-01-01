package com.servemenu.businessservice.domain.model;

import com.servemenu.businessservice.domain.enums.QRType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(
        name = "qr_customizations",
        indexes = {
                @Index(name = "idx_qr_customization_store_id", columnList = "store_id"),
                @Index(name = "idx_qr_customization_business_id", columnList = "business_id"),
                @Index(name = "idx_qr_customizations_qr_type", columnList = "qr_type")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Business-level QR (BUSINESS type only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", foreignKey = @ForeignKey(name = "fk_qr_customization_business"))
    private Business business;
    
    // Store-level QR (TABLE, WIFI types)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", foreignKey = @ForeignKey(name = "fk_qr_customization_store"))
    private Store store;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "qr_type", nullable = false, length = 20)
    @Builder.Default
    private QRType qrType = QRType.TABLE;

    // QR Dimensions
    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 800;

    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 800;

    @Column(name = "margin", nullable = false)
    @Builder.Default
    private Integer margin = 10;

    // Colors
    @Column(name = "background_color", nullable = false, length = 7)
    @Builder.Default
    private String backgroundColor = "#FFFFFF";

    @Column(name = "pattern_color_mode", nullable = false, length = 20)
    @Builder.Default
    private String patternColorMode = "single"; // single, gradient

    @Column(name = "pattern_color_single", length = 7)
    @Builder.Default
    private String patternColorSingle = "#000000";

    @Column(name = "pattern_gradient_type", length = 20)
    private String patternGradientType; // linear, radial

    @Column(name = "pattern_gradient_start", length = 7)
    private String patternGradientStart;

    @Column(name = "pattern_gradient_end", length = 7)
    private String patternGradientEnd;

    @Column(name = "pattern_gradient_rotation")
    private Integer patternGradientRotation;

    // Pattern & Eye Types
    @Column(name = "pattern_type", nullable = false, length = 20)
    @Builder.Default
    private String patternType = "rounded"; // square, rounded, dots, classy, classy-rounded, extra-rounded

    @Column(name = "eye_type", nullable = false, length = 20)
    @Builder.Default
    private String eyeType = "extra-rounded"; // square, dot, extra-rounded

    @Column(name = "eye_color_enabled", nullable = false)
    @Builder.Default
    private Boolean eyeColorEnabled = true;

    @Column(name = "eye_color_outer", length = 7)
    @Builder.Default
    private String eyeColorOuter = "#132440";

    @Column(name = "eye_color_inner", length = 7)
    @Builder.Default
    private String eyeColorInner = "#bf092f";

    // Logo
    @Column(name = "logo_media_id")
    private UUID logoMediaId;

    @Column(name = "logo_size")
    @Builder.Default
    private Integer logoSize = 30; // percentage
    
    // Transient field for logo URL (enriched from Media Service)
    @Transient
    private String logoUrl;

    // Frame
    @Column(name = "frame_type", nullable = false, length = 20)
    @Builder.Default
    private String frameType = "none"; // none, circle-frame, bottom-text, top-text, box-frame

    @Column(name = "frame_text", length = 50)
    @Builder.Default
    private String frameText = "SCAN FOR MENU";

    @Column(name = "frame_font", length = 50)
    @Builder.Default
    private String frameFont = "Comic Sans MS";

    @Column(name = "frame_text_color", length = 7)
    @Builder.Default
    private String frameTextColor = "#FFFFFF"; // Text color for frame

    @Column(name = "frame_color_mode", nullable = false, length = 20)
    @Builder.Default
    private String frameColorMode = "single"; // single, gradient

    @Column(name = "frame_color_single", length = 7)
    @Builder.Default
    private String frameColorSingle = "#000000";

    @Column(name = "frame_gradient_type", length = 20)
    private String frameGradientType;

    @Column(name = "frame_gradient_start", length = 7)
    private String frameGradientStart;

    @Column(name = "frame_gradient_end", length = 7)
    private String frameGradientEnd;

    @Column(name = "frame_gradient_rotation")
    private Integer frameGradientRotation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Business Logic Methods
    public void updateDimensions(Integer width, Integer height, Integer margin) {
        this.width = width;
        this.height = height;
        this.margin = margin;
    }

    public void updateColors(String backgroundColor, String patternColorMode, String patternColorSingle) {
        this.backgroundColor = backgroundColor;
        this.patternColorMode = patternColorMode;
        this.patternColorSingle = patternColorSingle;
    }

    public void updatePatternGradient(String type, String start, String end, Integer rotation) {
        this.patternGradientType = type;
        this.patternGradientStart = start;
        this.patternGradientEnd = end;
        this.patternGradientRotation = rotation;
    }

    public void updatePatternType(String patternType) {
        this.patternType = patternType;
    }

    public void updateEyeSettings(String eyeType, Boolean eyeColorEnabled, String outer, String inner) {
        this.eyeType = eyeType;
        this.eyeColorEnabled = eyeColorEnabled;
        this.eyeColorOuter = outer;
        this.eyeColorInner = inner;
    }

    public void updateLogo(UUID logoMediaId, Integer logoSize) {
        this.logoMediaId = logoMediaId;
        this.logoSize = logoSize;
    }

    public void updateFrame(String frameType, String frameText, String frameFont, String frameTextColor) {
        this.frameType = frameType;
        this.frameText = frameText;
        this.frameFont = frameFont;
        this.frameTextColor = frameTextColor;
    }

    public void updateFrameColors(String frameColorMode, String frameColorSingle) {
        this.frameColorMode = frameColorMode;
        this.frameColorSingle = frameColorSingle;
    }

    public void updateFrameGradient(String type, String start, String end, Integer rotation) {
        this.frameGradientType = type;
        this.frameGradientStart = start;
        this.frameGradientEnd = end;
        this.frameGradientRotation = rotation;
    }

    public boolean hasLogo() {
        return logoMediaId != null;
    }

    public boolean hasFrame() {
        return !"none".equals(frameType);
    }

    public boolean isPatternGradient() {
        return "gradient".equals(patternColorMode);
    }

    public boolean isFrameGradient() {
        return "gradient".equals(frameColorMode);
    }
}
