package uk.gov.dwp.uc.pairtest.exception;

/**
 * Invalid purchase exception.
 */
public class InvalidPurchaseException extends RuntimeException {
  public InvalidPurchaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
