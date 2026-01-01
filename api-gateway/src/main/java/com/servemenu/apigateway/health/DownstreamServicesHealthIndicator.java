package com.servemenu.apigateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator for downstream services
 * 
 * Checks health of all registered services in Eureka
 */
@Component
public class DownstreamServicesHealthIndicator implements ReactiveHealthIndicator {

    private final DiscoveryClient discoveryClient;

    public DownstreamServicesHealthIndicator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(() -> {
            Map<String, Object> details = new HashMap<>();
            List<String> services = discoveryClient.getServices();
            
            details.put("totalServices", services.size());
            details.put("services", services);
            
            // Check each service
            Map<String, Integer> serviceInstances = new HashMap<>();
            for (String service : services) {
                int instanceCount = discoveryClient.getInstances(service).size();
                serviceInstances.put(service, instanceCount);
            }
            details.put("instances", serviceInstances);
            
            // Determine overall health
            boolean allHealthy = serviceInstances.values().stream()
                .allMatch(count -> count > 0);
            
            if (allHealthy && !services.isEmpty()) {
                return Health.up().withDetails(details).build();
            } else if (services.isEmpty()) {
                return Health.down()
                    .withDetail("reason", "No services registered")
                    .withDetails(details)
                    .build();
            } else {
                // Some services have no instances - still UP but with warning
                details.put("warning", "Some services have no instances");
                return Health.up().withDetails(details).build();
            }
        });
    }
}
