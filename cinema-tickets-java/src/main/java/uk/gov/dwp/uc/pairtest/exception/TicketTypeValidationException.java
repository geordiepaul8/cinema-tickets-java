package uk.gov.dwp.uc.pairtest.exception;

/**
 * Ticket type validation exception.
 */
public class TicketTypeValidationException extends RuntimeException {

  public TicketTypeValidationException(String message) {
    super(message);
  }
}
