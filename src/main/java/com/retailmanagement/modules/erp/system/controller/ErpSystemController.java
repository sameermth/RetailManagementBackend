package com.retailmanagement.modules.erp.system.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "ERP System", description = "ERP system status endpoints")
public class ErpSystemController {

    @GetMapping("/api/erp/system/ping")
    @Operation(summary = "Ping ERP module")
    public ErpApiResponse<PingResponse> ping() {
        return ErpApiResponse.ok(new PingResponse("ok", "erp-aligned"));
    }

    public record PingResponse(String status, String module) {}
}
