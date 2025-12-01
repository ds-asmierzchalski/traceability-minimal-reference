package net.catenax.traceability.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.catenax.traceability.validator.ValidationResult;
import net.catenax.traceability.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Quality Notification API endpoints.
 * Handles receiving and updating quality notifications with proper error handling.
 * <br/><br/>
 * <small>
 * Copyright (c) 2025, doubleSlash Net-Business GmbH, http://www.doubleslash.de
 * <br/><br/>
 * </small>
 *
 * @author emanuel.schaeffer@doubleslash.de
 * @version : QualityNotificationController.java eschaeffer $
 */
@RestController
@RequestMapping("/api/traceability/qualitynotifications")
@RequiredArgsConstructor
public class QualityNotificationController {
   private final Logger logger = LoggerFactory.getLogger(QualityNotificationController.class);

   private final Validator validator;

   /**
    * Receives a new quality notification.
    *
    * @return 201 CREATED if successful
    */
   @PostMapping("/receive")
   public ResponseEntity<@NonNull Void> receiveQualityNotification(
         @RequestBody final JsonNode body) {

      final ValidationResult result = validator.validateReceive(body);
      return ResponseEntity.status(
            result.isValid() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST
      ).build();
   }

   /**
    * Updates an existing quality notification.
    *
    * @param requestBody the quality notification update request body (validated)
    * @return 200 OK if successful
    */
   @PostMapping("/update")
   public ResponseEntity<@NonNull Void> updateQualityNotification(
         @RequestBody final JsonNode requestBody) {

      final ValidationResult result = validator.validateUpdate(requestBody);
      return ResponseEntity.status(
            result.isValid() ? HttpStatus.OK : HttpStatus.BAD_REQUEST
      ).build();
   }

}