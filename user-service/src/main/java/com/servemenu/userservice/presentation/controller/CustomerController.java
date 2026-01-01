package com.servemenu.userservice.presentation.controller;

import com.servemenu.userservice.application.dto.command.CreateCustomerCommand;
import com.servemenu.userservice.application.dto.request.RegisterCustomerRequest;
import com.servemenu.userservice.application.dto.response.CustomerResponse;
import com.servemenu.userservice.application.service.CustomerService;
import com.servemenu.userservice.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        CreateCustomerCommand command = new CreateCustomerCommand(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                request.phoneNumber(),
                request.countryCode(),
                request.businessId()
        );

        CustomerResponse created = customerService.registerCustomer(command);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('customer')")
    public ResponseEntity<CustomerResponse> getCurrentCustomer() {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        CustomerResponse customer = customerService.findByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/business/{businessId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<Page<CustomerResponse>> getCustomersByBusiness(
            @PathVariable UUID businessId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CustomerResponse> customers = customerService.findByBusinessId(businessId, pageable);
        return ResponseEntity.ok(customers);
    }

    @PostMapping("/me/loyalty/add")
    @PreAuthorize("hasRole('customer')")
    public ResponseEntity<Void> addLoyaltyPoints(@RequestParam Integer points) {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        CustomerResponse customer = customerService.findByKeycloakUserId(keycloakUserId);

        customerService.addLoyaltyPoints(customer.user().id(), points);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/loyalty/redeem")
    @PreAuthorize("hasRole('customer')")
    public ResponseEntity<Void> redeemLoyaltyPoints(@RequestParam Integer points) {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        CustomerResponse customer = customerService.findByKeycloakUserId(keycloakUserId);

        customerService.redeemLoyaltyPoints(customer.user().id(), points);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/business/{businessId}/count")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<Long> countCustomersByBusiness(@PathVariable UUID businessId) {
        long count = customerService.countByBusinessId(businessId);
        return ResponseEntity.ok(count);
    }
}