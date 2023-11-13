package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.uc.pairtest.domain.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.Type.INFANT;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.AccountValidationException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.TicketAllocationException;
import uk.gov.dwp.uc.pairtest.exception.TicketTypeValidationException;

@DisplayName("Ticket Service Impl tests")
public class TicketServiceImplTest{

  /** Ticket service. */
  private TicketService ticketService;

  /** Seat reservation service. */
  private SeatReservationService seatReservationService;

  /** Ticket payment service. */
  private TicketPaymentService ticketPaymentService;

  /** Account service. */
  private AccountService accountService;

  /** Ticket type service. */
  private TicketTypeService ticketTypeService;

  /** Ticket allocation service. */
  private TicketAllocationService ticketAllocationService;

  /** Valid account id. */
  public static final Long VALID_ACCOUNT_ID = 1L;

  /** Single valid Adult ticket type request. */
  private final TicketTypeRequest[] ticketTypeRequests = { new TicketTypeRequest(ADULT, 1) };

  @BeforeEach
  void setUp() {
    ticketPaymentService = mock(TicketPaymentService.class);
    seatReservationService = mock(SeatReservationService.class);
    accountService = mock(AccountService.class);
    ticketTypeService = mock(TicketTypeService.class);
    ticketAllocationService = mock(TicketAllocationService.class);

    ticketService = new TicketServiceImpl(ticketPaymentService,
        seatReservationService, accountService, ticketTypeService,
        ticketAllocationService);

    // set happy path mocks

    // Assumptions:
    // - The `TicketPaymentService` implementation is an external provider with no defects. You do not need to worry about how the actual payment happens.
    // - The payment will always go through once a payment request has been made to the `TicketPaymentService`.
    doNothing().when(ticketPaymentService).makePayment(anyLong(), anyInt());

    // - The `SeatReservationService` implementation is an external provider with no defects. You do not need to worry about how the seat reservation algorithm works.
    // - The seat will always be reserved once a reservation request has been made to the `SeatReservationService`.
    doNothing().when(seatReservationService).reserveSeat(anyLong(), anyInt());

    doNothing().when(accountService).validateAccount(anyLong());

    doNothing().when(ticketTypeService).validateTicketTypeRequest(any(TicketTypeRequest[].class));
  }

  @DisplayName("Should throw InvalidPaymentException when account service throws AccountValidationException")
  @Test
  void throwsInvalidPaymentExceptionWhenAccountServiceThrowsAccountValidationException() {
    final String errorMessage = "an error";

    doThrow(new AccountValidationException(errorMessage))
        .when(accountService).validateAccount(anyLong());

    InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
        () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID));

    assertEquals(errorMessage, ex.getMessage());
    assertEquals(AccountValidationException.class, ex.getCause().getClass());
    assertEquals(errorMessage, ex.getCause().getMessage());

    verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
    verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
  }

  @DisplayName("Should throw InvalidPaymentException when ticket type service validate TicketTypeRequest throws TicketTypeValidationException")
  @Test
  void throwsInvalidPaymentExceptionWhenValidateTicketTypeRequestThrowsTicketTypeValidationException() {
    final String errorMessage = "an error";

    doThrow(new TicketTypeValidationException(errorMessage))
        .when(ticketTypeService).validateTicketTypeRequest(any(TicketTypeRequest[].class));

    InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
        () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests));

    assertEquals(errorMessage, ex.getMessage());
    assertEquals(TicketTypeValidationException.class, ex.getCause().getClass());
    assertEquals(errorMessage, ex.getCause().getMessage());
    verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
    verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
  }

  @DisplayName("Should throw InvalidPaymentException when ticket type service get num tickets per type throws TicketTypeValidationException")
  @Test
  void throwsInvalidPaymentExceptionWhenTicketTypeServiceNumTicketsThrowsTicketTypeValidationException() {
    final String errorMessage = "an error";

    doThrow(new TicketTypeValidationException(errorMessage))
        .when(ticketTypeService).getNumberOfTicketsPerType(any(TicketTypeRequest[].class));

    InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
        () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests));

    assertEquals(errorMessage, ex.getMessage());
    assertEquals(TicketTypeValidationException.class, ex.getCause().getClass());
    assertEquals(errorMessage, ex.getCause().getMessage());
    verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
    verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
  }

  @DisplayName("Should throw InvalidPaymentException when ticket allocation service get and validate total tickets throws TicketAllocationException")
  @Test
  void throwsInvalidPaymentExceptionWhenGetValidateThrowsTicketAllocationException() {
    final String errorMessage = "an error";

    when(ticketTypeService.getNumberOfTicketsPerType(any(TicketTypeRequest[].class)))
        .thenReturn(Map.ofEntries(Map.entry(ADULT, 1)));

    doThrow(new TicketAllocationException(errorMessage))
        .when(ticketAllocationService).getAndValidateTotalTicketsRequested(anyMap());

    InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
      () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests));

    assertEquals(errorMessage, ex.getMessage());
    assertEquals(TicketAllocationException.class, ex.getCause().getClass());
    assertEquals(errorMessage, ex.getCause().getMessage());

    verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
    verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
  }

  @DisplayName("Should throw InvalidPaymentException when ticket allocation service calculate total amount throws TicketAllocationException")
  @Test
  void throwsInvalidPaymentExceptionWhenTicketAllocationServiceCalculateTotalThrowsTicketAllocationException() {
    final String errorMessage = "an error";

    when(ticketTypeService.getNumberOfTicketsPerType(any(TicketTypeRequest[].class)))
        .thenReturn(Map.ofEntries(Map.entry(ADULT, 1)));

    when(ticketAllocationService.getAndValidateTotalTicketsRequested(anyMap()))
        .thenReturn(1);

    doThrow(new TicketAllocationException(errorMessage))
        .when(ticketAllocationService).calculateTotalAmountFromTicketsRequested(anyMap());

    InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
        () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests));

    assertEquals(errorMessage, ex.getMessage());
    assertEquals(TicketAllocationException.class, ex.getCause().getClass());
    assertEquals(errorMessage, ex.getCause().getMessage());

    verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
    verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
  }


  @DisplayName("Should make correct payment and reserve correct amount of seats according to #ticket types")
  @Test
  void makeCorrectPaymentAndReserveCorrectAmountOfSeatsPerRequest() {
    final int expectedAmountToPay = 20;
    final int expectedNumSeats = 1;

    when(ticketTypeService.getNumberOfTicketsPerType(any(TicketTypeRequest[].class)))
        .thenReturn(Map.ofEntries(Map.entry(ADULT, 1)));

    when(ticketAllocationService.getAndValidateTotalTicketsRequested(anyMap()))
        .thenReturn(expectedNumSeats);

    when(ticketAllocationService.calculateTotalAmountFromTicketsRequested(anyMap()))
        .thenReturn(expectedAmountToPay);

    ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

    verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, expectedAmountToPay);
    verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, expectedNumSeats);
  }

  @DisplayName("Should make correct payment and reserve correct amount of seats with ADULT = 19 && CHILD = 1 && INFANT = 19")
  @Test
  void makeCorrectPaymentAndReserveCorrectAmountOfSeatsPerRequestMaxLimit() {
    final int expectedAmountToPay = 390;
    final int expectedNumSeats = 20;

    when(ticketTypeService.getNumberOfTicketsPerType(any(TicketTypeRequest[].class)))
        .thenReturn(Map.ofEntries(
          Map.entry(ADULT, 19),
          Map.entry(INFANT, 19),
          Map.entry(CHILD, 1)
        ));

    when(ticketAllocationService.getAndValidateTotalTicketsRequested(anyMap()))
        .thenReturn(expectedNumSeats);

    when(ticketAllocationService.calculateTotalAmountFromTicketsRequested(anyMap()))
        .thenReturn(expectedAmountToPay);

    ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

    verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, expectedAmountToPay);
    verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, expectedNumSeats);
  }
}