package uk.gov.dwp.uc.pairtest;

import static uk.gov.dwp.uc.pairtest.domain.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.Type.INFANT;

import java.util.Map;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.TicketAllocationException;

/**
 * Ticket Allocation Service.
 */
public class TicketAllocationServiceImpl implements TicketAllocationService {

  /**
   * The maximum # of tickets allowed to purchase from
   * a single ticket type request.
   */
  public static final int MAX_TICKETS = 20;

  @Override
  public int getAndValidateTotalTicketsRequested(Map<Type, Integer> ticketsMap) {

    // extra guard - should not be needed but being extra safe!
    if (ticketsMap == null || ticketsMap.isEmpty()) {
      throw new TicketAllocationException("No ticket types are supplied");
    }

    // must have at least 1 ADULT - this allows for a CHILD & INFANT to attend
    int numAdultTickets = getTotalTicketsPerType(ticketsMap, ADULT);

    if (numAdultTickets == 0) {
      throw new TicketAllocationException("There was 0 ADULT tickets included in the request");
    }

    int numChildTickets = getTotalTicketsPerType(ticketsMap, CHILD);
    int numInfantTickets = getTotalTicketsPerType(ticketsMap, INFANT);

    // INFANT total must be <= ADULT
    if (numInfantTickets > numAdultTickets) {
      throw new TicketAllocationException("There was more INFANT tickets requested than ADULT");
    }

    // only ADULT & CHILD tickets count towards seat allocation
    int totalTicketsRequested = numAdultTickets + numChildTickets;

    // total tickets of ADULT & CHILD must not exceed MAX_TICKETS
    if (totalTicketsRequested > MAX_TICKETS) {
      throw new TicketAllocationException(
        String.format("Max tickets allowed is %d, number ADULT & CHILD requested is %d",
          MAX_TICKETS, totalTicketsRequested));
    }

    return totalTicketsRequested;
  }

  @Override
  public int calculateTotalAmountFromTicketsRequested(Map<Type, Integer> ticketsMap) {
    return ticketsMap.entrySet().stream()
      .mapToInt(type -> type.getValue() * type.getKey().ticketCost.intValue())
      .sum();
  }

  /**
   * Get the total tickets from the map of Types. If the type is not listed, then an
   * entry will be added for that type with a value of 0 to allow for follow-on processing.
   *
   * @param ticketMap the ticket map containing the keys of the Types grouped from the
   *                  ticket type requests.
   * @param type the type to  get the number of tickets from.
   * @return the total number of tickets calculated for the type.
   */
  private int getTotalTicketsPerType(Map<Type, Integer> ticketMap, Type type) {
    return ticketMap.get(type) != null ? ticketMap.get(type) : 0;
  }
}
