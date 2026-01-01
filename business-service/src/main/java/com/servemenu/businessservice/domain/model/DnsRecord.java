package com.servemenu.businessservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DnsRecord implements Serializable {
    private String type;        // A, CNAME, TXT
    private String name;        // @ or subdomain
    private String value;       // IP or hostname
    private String description; // User guidance
}