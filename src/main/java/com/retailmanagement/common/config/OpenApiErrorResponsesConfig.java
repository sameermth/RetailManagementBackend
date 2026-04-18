package com.retailmanagement.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiErrorResponsesConfig {

    @Bean
    public OpenApiCustomizer globalErrorResponsesCustomizer() {
        return this::applyGlobalErrorResponses;
    }

    private void applyGlobalErrorResponses(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }

        registerErrorResponseSchema(components);
        registerErrorResponses(components);

        if (openApi.getPaths() == null) {
            return;
        }
        for (PathItem pathItem : openApi.getPaths().values()) {
            for (Operation operation : pathItem.readOperations()) {
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }
                addIfMissing(responses, "400", "#/components/responses/BadRequestError");
                addIfMissing(responses, "401", "#/components/responses/UnauthorizedError");
                addIfMissing(responses, "403", "#/components/responses/ForbiddenError");
                addIfMissing(responses, "404", "#/components/responses/NotFoundError");
                addIfMissing(responses, "500", "#/components/responses/InternalServerError");
            }
        }
    }

    private void registerErrorResponseSchema(Components components) {
        if (components.getSchemas() != null && components.getSchemas().containsKey("ErrorResponse")) {
            return;
        }
        Schema<?> schema = new ObjectSchema()
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("status", new IntegerSchema())
                .addProperty("error", new StringSchema())
                .addProperty("errorCode", new StringSchema())
                .addProperty("message", new StringSchema())
                .addProperty("path", new StringSchema())
                .addProperty("validationErrors", new MapSchema().additionalProperties(new StringSchema()));
        components.addSchemas("ErrorResponse", schema);
    }

    private void registerErrorResponses(Components components) {
        components.addResponses(
                "BadRequestError",
                new ApiResponse()
                        .description("Bad request or validation failure")
                        .content(errorContent())
        );
        components.addResponses(
                "UnauthorizedError",
                new ApiResponse()
                        .description("Authentication required or invalid credentials")
                        .content(errorContent())
        );
        components.addResponses(
                "ForbiddenError",
                new ApiResponse()
                        .description("Authenticated but not allowed to access the resource")
                        .content(errorContent())
        );
        components.addResponses(
                "NotFoundError",
                new ApiResponse()
                        .description("Requested resource not found")
                        .content(errorContent())
        );
        components.addResponses(
                "InternalServerError",
                new ApiResponse()
                        .description("Unexpected server-side failure")
                        .content(errorContent())
        );
    }

    private Content errorContent() {
        return new Content().addMediaType(
                "application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
        );
    }

    private void addIfMissing(ApiResponses responses, String statusCode, String responseRef) {
        if (responses.containsKey(statusCode)) {
            return;
        }
        responses.addApiResponse(statusCode, new ApiResponse().$ref(responseRef));
    }
}
