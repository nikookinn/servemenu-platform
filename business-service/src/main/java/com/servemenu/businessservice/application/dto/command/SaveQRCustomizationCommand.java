package com.servemenu.businessservice.application.dto.command;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SaveQRCustomizationCommand(
        Integer width,
        Integer height,
        Integer margin,
        String backgroundColor,
        String patternColorMode,
        String patternColorSingle,
        String patternGradientType,
        String patternGradientStart,
        String patternGradientEnd,
        Integer patternGradientRotation,
        String patternType,
        String eyeType,
        Boolean eyeColorEnabled,
        String eyeColorOuter,
        String eyeColorInner,
        UUID logoMediaId,
        Integer logoSize,
        String frameType,
        String frameText,
        String frameFont,
        String frameTextColor,
        String frameColorMode,
        String frameColorSingle,
        String frameGradientType,
        String frameGradientStart,
        String frameGradientEnd,
        Integer frameGradientRotation
) {
}
