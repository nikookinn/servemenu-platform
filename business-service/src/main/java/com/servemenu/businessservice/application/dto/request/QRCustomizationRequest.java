package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QRCustomizationRequest(
        @NotNull
        @Min(100)
        @Max(2000)
        Integer width,

        @NotNull
        @Min(100)
        @Max(2000)
        Integer height,

        @NotNull
        @Min(0)
        @Max(50)
        Integer margin,

        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Background color must be a valid hex color")
        String backgroundColor,

        @NotBlank
        @Pattern(regexp = "^(single|gradient)$", message = "Pattern color mode must be 'single' or 'gradient'")
        String patternColorMode,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Pattern color must be a valid hex color")
        String patternColorSingle,

        @Pattern(regexp = "^(linear|radial)$", message = "Pattern gradient type must be 'linear' or 'radial'")
        String patternGradientType,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Pattern gradient start must be a valid hex color")
        String patternGradientStart,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Pattern gradient end must be a valid hex color")
        String patternGradientEnd,

        @Min(0)
        @Max(360)
        Integer patternGradientRotation,

        @NotBlank
        @Pattern(regexp = "^(square|rounded|dots|classy|classy-rounded|extra-rounded)$")
        String patternType,

        @NotBlank
        @Pattern(regexp = "^(square|dot|extra-rounded)$")
        String eyeType,

        @NotNull
        Boolean eyeColorEnabled,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Eye color outer must be a valid hex color")
        String eyeColorOuter,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Eye color inner must be a valid hex color")
        String eyeColorInner,

        UUID logoMediaId,

        @Min(10)
        @Max(50)
        Integer logoSize,

        @NotBlank
        @Pattern(regexp = "^(none|circle-frame|bottom-text|top-text|box-frame)$")
        String frameType,

        @Size(max = 50, message = "Frame text must not exceed 50 characters")
        String frameText,

        @Size(max = 50)
        String frameFont,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Frame text color must be a valid hex color")
        String frameTextColor,

        @NotBlank
        @Pattern(regexp = "^(single|gradient)$", message = "Frame color mode must be 'single' or 'gradient'")
        String frameColorMode,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Frame color must be a valid hex color")
        String frameColorSingle,

        @Pattern(regexp = "^(linear|radial)$", message = "Frame gradient type must be 'linear' or 'radial'")
        String frameGradientType,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Frame gradient start must be a valid hex color")
        String frameGradientStart,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Frame gradient end must be a valid hex color")
        String frameGradientEnd,

        @Min(0)
        @Max(360)
        Integer frameGradientRotation
) {
    public static QRCustomizationRequest getDefault() {
        return QRCustomizationRequest.builder()
                .width(800)
                .height(800)
                .margin(10)
                .backgroundColor("#FFFFFF")
                .patternColorMode("single")
                .patternColorSingle("#000000")
                .patternType("rounded")
                .eyeType("extra-rounded")
                .eyeColorEnabled(true)
                .eyeColorInner("#bf092f")
                .eyeColorOuter("#132440")
                .logoSize(30)
                .frameType("none")
                .frameFont("Arial")
                .frameTextColor("#FFFFFF")
                .frameColorMode("single")
                .frameColorSingle("#000000")
                .build();
    }
}
