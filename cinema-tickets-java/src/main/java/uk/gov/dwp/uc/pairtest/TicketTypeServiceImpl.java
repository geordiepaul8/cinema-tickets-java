package uk.gov.dwp.uc.pairtest;

import static uk.gov.dwp.uc.pairtest.TicketAllocationServiceImpl.MAX_TICKETS;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.TicketTypeValidationException;

/**
 * Ticket type service.
 */
public class TicketTypeServiceImpl implements TicketTypeService {

  /**
   * This has been set to 40 for a number of reasons. Firstly to avoid a large
   * payload of variadic args to be sent. Secondly, due to the Business rule that states
   * Only a max of 20 tickets that can be purchased at a time, means that I have interpreted this
   * as the maximum valid requests that can be sent are all from single entries, for example:
   * the maximum number of requests that can be sent which does not exceed the Maximum tickets
   * allowed to be purchased within that transaction (20) is: 20 x Adult (single ticket requests)
   * & 20 Infant (single ticket requests). Anymore requests and this exceeds the MAX_TICKETS.
   * See the Readme.md for more examples.
   */
  public static final int MAX_TICKET_TYPE_REQUESTS_ALLOWED = 40;

  @Override
  public void validateTicketTypeRequest(TicketTypeRequest[] ticketTypeRequests) {
    if (Objects.isNull(ticketTypeRequests) || ticketTypeRequests.length == 0) {
      throw new TicketTypeValidationException("The ticketTypeRequests supplied is empty");
    }

    if (ticketTypeRequests.length > MAX_TICKET_TYPE_REQUESTS_ALLOWED) {
      throw new TicketTypeValidationException("The maximum ticketTypeRequests allowed is exceeded");
    }
  }

  @Override
  public Map<Type, Integer> getNumberOfTicketsPerType(TicketTypeRequest[] ticketTypeRequests) {
    Map<Type, Integer> ticketmap = Arrays.stream(ticketTypeRequests)
        .peek(ttr -> {

          if (ttr.type() == null) {
            throw new TicketTypeValidationException("There was an error with a ticket type");
          }

          // we are unsure as to the number of TicketTypeRequests, but if a single one
          // is above the MAX_TICKETS then we know it will fail as the max you can purchase
          // with the requests is MAX_TICKETS
          if (ttr.noOfTickets() <= 0 || ttr.noOfTickets() > MAX_TICKETS) {
            throw new TicketTypeValidationException("There was an error with a requested "
                + "number of tickets");
          }

        })
        .collect(
          Collectors.groupingBy(
            TicketTypeRequest::type,
            Collectors.summingInt(TicketTypeRequest::noOfTickets)));

    // here I will add a total of 0 for any keys that do not exist so the map contains all
    // ticket types and totals for ease of further processing

    Arrays.stream(Type.values()).forEach(type -> {
      if (!ticketmap.containsKey(type)) {
        ticketmap.put(type, 0);
      }
    });

    return ticketmap;
  }
}
