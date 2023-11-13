package uk.gov.dwp.uc.pairtest.domain;

/**
 * Ticket type request record - immutable object.
 *
 * @param type the type of request.
 * @param noOfTickets the number of tickets requested.
 */
public record TicketTypeRequest(Type type, int noOfTickets) { }
