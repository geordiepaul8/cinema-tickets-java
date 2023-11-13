package uk.gov.dwp.uc.pairtest.exception;

/**
 * Account validation exception.
 */
public class AccountValidationException extends RuntimeException {
  public AccountValidationException(String message) {
    super(message);
  }
}
