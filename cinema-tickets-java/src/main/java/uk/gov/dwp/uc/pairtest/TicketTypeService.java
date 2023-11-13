package uk.gov.dwp.uc.pairtest;

import java.util.Map;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.TicketTypeValidationException;

/**
 * Ticket type service interface.
 */
public interface TicketTypeService {

  /**
   * Validate the TicketTypeRequest args that are sent. To be valid is must not be null
   * and have size of at least 1 entry.
   * <br /><br />
   *
   * @param ticketTypeRequests the ticket type requests for processing.
   * @throws TicketTypeValidationException when null or empty.
   */
  void validateTicketTypeRequest(TicketTypeRequest[] ticketTypeRequests);


  /**
   * loops through the Ticket requests and validates the ticket type is valid and the
   * number of seats requested is a positive number. Joins these into a map with the
   * Type as the key and the number of tickets requested as the total of all the
   * requests.
   *
   * @param ticketTypeRequests the ticket type requests.
   * @return a map containing the total requests per type.
   * @throws TicketTypeValidationException if the type or number of tickets are deemed
   *                                       to be invalid.
   */
  Map<Type, Integer> getNumberOfTicketsPerType(TicketTypeRequest[] ticketTypeRequests);
}
