package com.servemenu.businessservice.application.dto.command;

import java.util.List;

public record UpdateBusinessCommand(
        String businessName,
        List<String> supportedLanguages
) {}
