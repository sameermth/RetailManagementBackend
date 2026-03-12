// FILE 331: common/controller/HealthController.java
package com.retailmanagement.common.controller;

import com.retailmanagement.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", java.time.LocalDateTime.now());
        status.put("service", "Retail Management API");
        status.put("version", "1.0.0");

        return ResponseEntity.ok(ApiResponse.success("Service is running", status));
    }

    @GetMapping("/ping")
    @Operation(summary = "Ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}