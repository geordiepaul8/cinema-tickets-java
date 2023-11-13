package uk.gov.dwp.uc.pairtest.exception;

/**
 * Ticket allocation exception.
 */
public class TicketAllocationException extends RuntimeException {
  public TicketAllocationException(String message) {
    super(message);
  }
}
