package net.catenax.traceability.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.erosb.kappa.core.exception.ResolutionException;
import com.github.erosb.kappa.core.validation.ValidationException;
import com.github.erosb.kappa.operation.validator.model.Request;
import com.github.erosb.kappa.operation.validator.model.impl.Body;
import com.github.erosb.kappa.operation.validator.model.impl.DefaultRequest;
import com.github.erosb.kappa.operation.validator.validation.OperationValidator;
import com.github.erosb.kappa.parser.OpenApi3Parser;
import com.github.erosb.kappa.parser.model.v3.OpenApi3;
import com.github.erosb.kappa.parser.model.v3.Operation;
import com.github.erosb.kappa.parser.model.v3.Path;
import com.github.erosb.kappa.schema.validator.ValidationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 * <br/><br/>
 * <small>
 * Copyright (c) 2025, doubleSlash Net-Business GmbH, http://www.doubleSlash.de
 * <br/><br/>
 * </small>
 *
 * @author emanuel.schaeffer@doubleslash.de
 * @version : Validator.java eschaeffer $
 */
@Component
public class Validator {
   private final OpenApi3 spec;
   private final Map<String, OperationValidator> validatorCache;
   private static final Logger log = LoggerFactory.getLogger(Validator.class);

   public Validator(@Value("${app.traceability.openapi-spec-url}") final String openApiUrl)
         throws ResolutionException, ValidationException, MalformedURLException {
      @SuppressWarnings("deprecation") final URL url = new URL(openApiUrl);
      this.spec = new OpenApi3Parser().parse(url, false);
      this.validatorCache = new HashMap<>();

      log.info("Initialized validator with OpenAPI spec from: {}", openApiUrl);
   }

   /**
    * Validates the request body against the OpenAPI specification for a given endpoint.
    *
    * @param pathPattern the OpenAPI path (e.g., "/qualitynotifications/receive")
    * @param method      the HTTP method (e.g., POST)
    * @param body        the request body object
    * @return ValidationResult containing validation status and any errors
    */
   public ValidationResult validate(final String pathPattern, final Request.Method method, final JsonNode body) {
      try {
         final OperationValidator validator = getOrCreateValidator(pathPattern, method);

         final Request request = new DefaultRequest.Builder(pathPattern, method).body(Body.from(body.toString()))
                                                                                .header("Content-Type",
                                                                                      "application/json")
                                                                                .build();

         final ValidationData<?> validationData = new ValidationData<>();
         validator.validateBody(request, validationData);

         if (!validationData.isValid()) {
            log.warn("Validation failed for {} {}: {}", method, pathPattern, validationData.results());
            return ValidationResult.failure(validationData);
         }

         log.debug("Validation successful for {} {}", method, pathPattern);
         return ValidationResult.success();

      } catch (final Exception e) {
         log.error("Unexpected validation error", e);
         return ValidationResult.error("Validation error: " + e.getMessage());
      }
   }

   /**
    * Convenience method for validating POST requests to /qualitynotifications/receive
    */
   public ValidationResult validateReceive(final JsonNode body) {
      return validate("/qualitynotifications/receive", Request.Method.POST, body);
   }

   /**
    * Convenience method for validating POST requests to /qualitynotifications/update
    */
   public ValidationResult validateUpdate(final JsonNode body) {
      return validate("/qualitynotifications/update", Request.Method.POST, body);
   }

   /**
    * Gets or creates an OperationValidator for the given path and method.
    * Validators are cached for performance.
    */
   private OperationValidator getOrCreateValidator(final String pathPattern, final Request.Method method) {
      final String cacheKey = method + ":" + pathPattern;

      return validatorCache.computeIfAbsent(cacheKey, k -> {
         final Path path = spec.getPath(pathPattern);
         if (path == null) {
            throw new IllegalArgumentException("Path not found in OpenAPI spec: " + pathPattern);
         }

         final Operation operation = switch (method) {
            case GET -> path.getGet();
            case POST -> path.getPost();
            case PUT -> path.getPut();
            case DELETE -> path.getDelete();
            case PATCH -> path.getPatch();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
         };

         if (operation == null) {
            throw new IllegalArgumentException("Operation not found for " + method + " " + pathPattern);
         }

         return new OperationValidator(spec, path, operation);
      });
   }
}