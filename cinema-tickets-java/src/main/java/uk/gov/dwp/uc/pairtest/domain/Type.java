package uk.gov.dwp.uc.pairtest.domain;

import java.math.BigDecimal;

/**
 * Ticket type - attached are the ticket cost values.
 */
public enum Type {
  ADULT(BigDecimal.valueOf(20.00)),
  CHILD(BigDecimal.valueOf(10.00)),
  INFANT(BigDecimal.ZERO);

  public final BigDecimal ticketCost;

  Type(BigDecimal ticketCost) {
    this.ticketCost = ticketCost;
  }
}
