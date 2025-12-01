package net.catenax.traceability.validator;

import com.github.erosb.kappa.schema.validator.ValidationData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result of an OpenAPI validation operation.
 * Provides methods for handling validation failures in a flexible way.
 * <br/><br/>
 * <small>
 * Copyright (c) 2025, doubleSlash Net-Business GmbH, http://www.doubleslash.de
 * <br/><br/>
 * </small>
 *
 * @author emanuel.schaeffer@doubleslash.de
 */
@Getter
public class ValidationResult {
   private final boolean valid;
   private final List<ValidationError> errors;
   private final String errorMessage;

   private ValidationResult(final boolean valid, final List<ValidationError> errors, final String errorMessage) {
      this.valid = valid;
      this.errors = errors != null ? errors : new ArrayList<>();
      this.errorMessage = errorMessage;
   }

   /**
    * Creates a successful validation result.
    */
   public static ValidationResult success() {
      return new ValidationResult(true, null, null);
   }

   /**
    * Creates a failed validation result from Kappa ValidationData.
    */
   public static ValidationResult failure(final ValidationData<?> validationData) {
      final List<ValidationError> errors = new ArrayList<>();

      if (!validationData.isValid()) {
         validationData.results().forEach(error -> {
            final String location = error.getInstanceLocation() != null ? error.describeInstanceLocation() : "unknown";
            final String message = error.getMessage();
            final String schemaLocation =
                  error.getSchemaLocation() != null ? error.describeSchemaLocation() : "unknown";

            errors.add(new ValidationError(location, message, schemaLocation));
         });
      }

      return new ValidationResult(false, errors, "Validation failed");
   }

   /**
    * Creates a failed validation result with a custom error message.
    */
   public static ValidationResult error(final String errorMessage) {
      return new ValidationResult(false, null, errorMessage);
   }

   /**
    * Checks if validation was successful.
    */
   public boolean isValid() {
      return valid;
   }

   /**
    * Checks if validation failed.
    */
   public boolean hasErrors() {
      return !valid;
   }

   /**
    * Executes an action if validation succeeded.
    *
    * @param action the action to execute
    * @return this ValidationResult for chaining
    */
   public ValidationResult ifValid(final Runnable action) {
      if (valid) {
         action.run();
      }
      return this;
   }

   /**
    * Executes an action if validation failed.
    *
    * @param action the action to execute (receives the list of errors)
    * @return this ValidationResult for chaining
    */
   public ValidationResult ifInvalid(final Consumer<List<ValidationError>> action) {
      if (!valid) {
         action.accept(errors);
      }
      return this;
   }

   /**
    * Throws an exception if validation failed.
    *
    * @param exceptionMapper function that creates an exception from the validation result
    * @param <T>             the exception type
    * @throws T if validation failed
    */
   public <T extends Throwable> ValidationResult orElseThrow(final Function<ValidationResult, T> exceptionMapper)
         throws T {
      if (!valid) {
         throw exceptionMapper.apply(this);
      }
      return this;
   }

   /**
    * Gets the first error message if validation failed.
    */
   public Optional<String> getFirstErrorMessage() {
      if (!errors.isEmpty()) {
         return Optional.of(errors.get(0).message());
      }
      return Optional.ofNullable(errorMessage);
   }

   /**
    * Gets a formatted error summary.
    */
   public String getErrorSummary() {
      if (valid) {
         return "No errors";
      }

      if (errorMessage != null && errors.isEmpty()) {
         return errorMessage;
      }

      final StringBuilder sb = new StringBuilder("Validation failed with ").append(errors.size())
                                                                           .append(" error(s):\n");

      errors.forEach(error -> sb.append("  - ").append(error.path()).append(": ").append(error.message()).append("\n"));

      return sb.toString();
   }

   /**
    * Represents a single validation error.
    */
   public record ValidationError(String path, String message, String schemaLocation) {

      @Override
      public String toString() {
         return String.format("ValidationError[path='%s', message='%s', schemaLocation='%s']", path, message,
               schemaLocation);
      }
   }
}

