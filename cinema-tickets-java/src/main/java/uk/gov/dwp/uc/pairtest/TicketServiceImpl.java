package uk.gov.dwp.uc.pairtest;

import java.util.Map;
import java.util.logging.Logger;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.AccountValidationException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.TicketAllocationException;
import uk.gov.dwp.uc.pairtest.exception.TicketTypeValidationException;

/**
 * Ticket service implementation class.
 */
public class TicketServiceImpl implements TicketService {

  /** Ticket payment service. */
  private final TicketPaymentService ticketPaymentService;

  /** Seat reservation service. */
  private final SeatReservationService seatReservationService;
  
  /** Account service. */
  private final AccountService accountService;

  /** Ticket type service. */
  private final TicketTypeService ticketTypeService;

  /** Ticket allocation service. */
  private final TicketAllocationService ticketAllocationService;

  /** logger. */
  private static final Logger logger = Logger.getLogger(TicketServiceImpl.class.getName());


  /** Constructor. */
  public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                           SeatReservationService seatReservationService,
                           AccountService accountService,
                           TicketTypeService ticketTypeService,
                           TicketAllocationService ticketAllocationService) {
    this.ticketPaymentService = ticketPaymentService;
    this.seatReservationService = seatReservationService;
    this.accountService = accountService;
    this.ticketTypeService = ticketTypeService;
    this.ticketAllocationService = ticketAllocationService;
  }


  /**
   * Should only have private methods other than the one below.
   */

  @Override
  public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
      throws InvalidPurchaseException {

    try {
      // validate the account id
      accountService.validateAccount(accountId);

      // validate the Ticket type requests
      ticketTypeService.validateTicketTypeRequest(ticketTypeRequests);

      // Convert the ticket type requests into a Map with Types as  the key and the total number of
      // tickets requested as the value
      Map<Type, Integer> ticketsMap =
          ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      int totalSeatsToAllocate =
          ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap);

      int totalAmountToPay =
          ticketAllocationService.calculateTotalAmountFromTicketsRequested(ticketsMap);

      // make payment
      ticketPaymentService.makePayment(accountId, totalAmountToPay);

      // book seats
      seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);

      logger.info(String.format("Total cost: £%d | Number of seats reserved: %d | Enjoy your movie",
          totalAmountToPay, totalSeatsToAllocate));

    } catch (AccountValidationException | TicketTypeValidationException
             | TicketAllocationException ex) {
      throw new InvalidPurchaseException(ex.getMessage(), ex);
    }
  }
}
