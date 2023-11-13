package uk.gov.dwp.uc.pairtest;

import java.util.Map;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.TicketAllocationException;

/** Ticket allocation service interface. */
public interface TicketAllocationService {

  /**
   * Validates the number of tickets (seats) requested and returns the total if all
   * the rules are satisfied. Extra assumption here is there should be no more INFANT tickets
   * than ADULT tickets.
   * <br /><br />
   *
   * @param ticketsMap the ticket map containing number of tickets per Type.
   * @return the total number of tickets requested (excluding INFANT).
   * @throws TicketAllocationException when no ADULT ticket is present, or INFANT tickets exceed
   *            ADULT tickets, or the total ADULT / CHILD tickets exceeds MAX_TICKETS allowed.
   */
  int getAndValidateTotalTicketsRequested(Map<Type, Integer> ticketsMap);


  /**
   * Loop through the ticket set map and calculate the cost by multiplying
   * the total seats requested per type by the cost of the ticket. Whilst INFANT cost is 0,
   * it is included in the calculation as nothing will be added to the cost of the ticket,
   * however should the price of an INFANT ticket increase, then nothing needs to change to
   * the calculation logic.
   * <br /><br />
   *
   * @param ticketsMap the tickets map set by Type.
   * @return the total cost of the tickets.
   */
  int calculateTotalAmountFromTicketsRequested(Map<Type, Integer> ticketsMap);
}
