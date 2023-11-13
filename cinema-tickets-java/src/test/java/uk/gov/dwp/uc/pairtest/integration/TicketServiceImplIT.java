package uk.gov.dwp.uc.pairtest.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.dwp.uc.pairtest.AccountServiceImplTest.ACCOUNT_ID_ERROR_MESSAGE;
import static uk.gov.dwp.uc.pairtest.TicketAllocationServiceImpl.MAX_TICKETS;
import static uk.gov.dwp.uc.pairtest.TicketAllocationServiceImplTest.MORE_INFANT_THAN_ADULT_ERROR_MESSAGE;
import static uk.gov.dwp.uc.pairtest.TicketAllocationServiceImplTest.NO_ADULT_TYPES_ERROR_MESSAGE;
import static uk.gov.dwp.uc.pairtest.TicketServiceImplTest.VALID_ACCOUNT_ID;
import static uk.gov.dwp.uc.pairtest.TicketTypeServiceImpl.MAX_TICKET_TYPE_REQUESTS_ALLOWED;
import static uk.gov.dwp.uc.pairtest.TicketTypeServiceImplTest.*;
import static uk.gov.dwp.uc.pairtest.domain.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.Type.INFANT;
import static uk.gov.dwp.uc.pairtest.domain.Type.values;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.AccountServiceImpl;
import uk.gov.dwp.uc.pairtest.TicketAllocationServiceImpl;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.TicketTypeServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.AccountValidationException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.TicketAllocationException;
import uk.gov.dwp.uc.pairtest.exception.TicketTypeValidationException;

@DisplayName("full test - no stubs")
@ExtendWith(MockitoExtension.class)
public class TicketServiceImplIT {

  /** Ticket payment service. */
  @Spy
  TicketPaymentService ticketPaymentService;

  /** Seat reservation service. */
  @Spy
  SeatReservationService seatReservationService;

  /** Ticket service. */
  private TicketServiceImpl ticketService;

  @BeforeEach
  void setUp() {
    ticketService = new TicketServiceImpl(
      ticketPaymentService,
      seatReservationService,
      new AccountServiceImpl(),
      new TicketTypeServiceImpl(),
      new TicketAllocationServiceImpl());
  }


  @DisplayName("Account id validation")
  @Nested
  class AccountIdTestCase {
    @DisplayName("Should throw InvalidPaymentException from AccountValidationException thrown when the accountId parameter is invalid")
    @ParameterizedTest(name = "accountId value is <= 0 : {0}")
    @NullSource
    @ValueSource(longs = { Long.MIN_VALUE, -1, 0})
    void shouldThrowInvalidPaymentExceptionWhenAccountIdInvalidAccountValidationExceptionThrown(Long invalidAccountId) {

      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1) };

      InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
        () -> ticketService.purchaseTickets(invalidAccountId, ticketTypeRequests));

      assertEquals(ACCOUNT_ID_ERROR_MESSAGE, ex.getMessage());
      assertEquals(AccountValidationException.class, ex.getCause().getClass());
      assertEquals(ACCOUNT_ID_ERROR_MESSAGE, ex.getCause().getMessage());
    }
  }

  @DisplayName("Valid ticket request scenarios")
  @Nested
  class ValidTicketRequestScenariosTestCase {
    @DisplayName("Should book the correct # of seats and pay the correct amount")
    @ParameterizedTest(name = "{0}")
    @MethodSource("validTicketTypeRequestScenarios")
    void shouldMakeCorrectPaymentAndReserveCorrectNumberOfSeats(String testDescription, TicketTypeRequest[] ticketTypeRequests,
                                                                int expectedNumSeatsBooked, int expectedTotalCostOfTickets) {

      ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

      verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, expectedTotalCostOfTickets);
      verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, expectedNumSeatsBooked);
    }


    private static Stream<Arguments> validTicketTypeRequestScenarios() {
      return Stream.of(
        // 1 ADULT only
        Arguments.of(
          "A single ADULT should book 1 seat and cost £20",
          buildSingleTicketTypeRequestArgs(1, 0, 0), 1, 20),
        // 1 ADULT & 1 CHILD
        Arguments.of(
          "A 1 x ADULT and 1 x CHILD should book 2 seats and cost £30",
          buildSingleTicketTypeRequestArgs(1, 1, 0), 2, 30),
        // 1 ADULT & 1 CHILD
        Arguments.of(
          "A 1 x ADULT, 1 INFANT and 1 x CHILD should book 2 seats and cost £30",
          buildSingleTicketTypeRequestArgs(1, 1, 1), 2, 30),
        // 1 ADULT & 2 CHILD
        Arguments.of(
          "A 1 x ADULT, 1 INFANT and 2 x CHILD should book 3 seats and cost £40 - you can have more CHILD than ADULTS",
          buildSingleTicketTypeRequestArgs(1, 2, 1), 3, 40),
        // MAX_TICKETS == 20 - adults only
        Arguments.of(
          "A MAX_TICKETS of ADULT requests should book 20 seats and cost £400",
          buildSingleTicketTypeRequestArgs(MAX_TICKETS, 0, 0), 20, 400),
        // MAX_TICKETS == 20 - ADULT x 19, CHILD x 1
        Arguments.of(
          "A MAX_TICKETS of ADULT & CHILD requests should book 20 seats and cost £390 (no INFANT)",
          buildSingleTicketTypeRequestArgs(19, 1, 0), 20, 390),
        // MAX_TICKETS == 20 - ADULT x 19, CHILD x 1
        Arguments.of(
          "A MAX_TICKETS of ADULT & CHILD requests should book 20 seats and cost £390 (with same amount of ADULT requests for INFANT)",
          buildSingleTicketTypeRequestArgs(19, 1, 19), 20, 390),
        // MAX_TICKETS == 1 - adults & infants = 40 requests included
        Arguments.of(
          "40 TicketTypeRequests get submitted (40 x ADULT & 40 x INFANT) should book 20 seats and cost £400" +
            " as INFANT does not count towards the MAX_TICKETS allocation or cost of ticket",
          buildSingleTicketTypeRequestArgs(MAX_TICKETS, 0, MAX_TICKETS), 20, 400)
      );
    }
  }

  @DisplayName("Invalid ticket request scenarios")
  @Nested
  class InvalidTicketRequestScenariosTestCase {
    @DisplayName("Should throw an InvalidPaymentException depending on different causes")
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTicketTypeRequestScenarios")
    void shouldNotMakeBookingAndThrow(String testDescription, TicketTypeRequest[] ticketTypeRequests,
                                      String expectedErrorMessage) {

      InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
        ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests));

      assertEquals(expectedErrorMessage, ex.getMessage());
      assertEquals(expectedErrorMessage, ex.getCause().getMessage());

      verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
      verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
    }

    private static Stream<Arguments> invalidTicketTypeRequestScenarios() {
      return Stream.of(
        // empty args: validateTicketTypeRequest
        Arguments.of(
          "An empty ticket type request should throw from ticketTypeService.validateTicketTypeRequest",
          new TicketTypeRequest[] {}, TICKET_REQUEST_EMPTY_ERROR_MESSAGE),
        // null args: validateTicketTypeRequest
        Arguments.of(
          "A null ticket type request should throw from ticketTypeService.validateTicketTypeRequest",
          null, TICKET_REQUEST_EMPTY_ERROR_MESSAGE),
        Arguments.of(
          "An ticket type request > MAX_TICKET_TYPE_REQUESTS_ALLOWED from ADULT only should throw from ticketTypeService.validateTicketTypeRequest",
          buildSingleTicketTypeRequestArgs(MAX_TICKET_TYPE_REQUESTS_ALLOWED + 1, 0, 0),
          MAX_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "An ticket type request > MAX_TICKET_TYPE_REQUESTS_ALLOWED from ADULT & CHILD only should throw from ticketTypeService.validateTicketTypeRequest",
          buildSingleTicketTypeRequestArgs(MAX_TICKET_TYPE_REQUESTS_ALLOWED, 1, 0),
          MAX_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "a null ticket type was included in the request should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(null, 1) },
          TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested == 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 0) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for ADULT < 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, -1) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for CHILD < 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1), new TicketTypeRequest(CHILD, -1) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for INFANT < 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1), new TicketTypeRequest(CHILD, 0) , new TicketTypeRequest(INFANT, -1) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for ADULT == 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 0) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for CHILD == 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1), new TicketTypeRequest(CHILD, 0) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for INFANT == 0 should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1), new TicketTypeRequest(CHILD, 1) , new TicketTypeRequest(INFANT, 0) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for ADULT > MAX_TICKETS should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, MAX_TICKETS + 1) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for CHILD > MAX_TICKETS should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1), new TicketTypeRequest(CHILD, MAX_TICKETS + 1) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "number of tickets requested for INFANT > MAX_TICKETS should throw from ticketTypeService.getNumberOfTicketsPerType",
          new TicketTypeRequest[] { new TicketTypeRequest(ADULT, 1), new TicketTypeRequest(CHILD, 0) , new TicketTypeRequest(INFANT, MAX_TICKETS + 1) },
          NUM_TICKET_TYPE_ERROR_MESSAGE),
        Arguments.of(
          "no ADULT tickets should throw from ticketAllocationServiceImpl.getAndValidateTotalTicketsRequested",
          buildSingleTicketTypeRequestArgs(0, 1, 1),
          NO_ADULT_TYPES_ERROR_MESSAGE),
        Arguments.of(
          "# ADULT < # INFANT tickets should throw from ticketAllocationServiceImpl.getAndValidateTotalTicketsRequested",
          buildSingleTicketTypeRequestArgs(1, 0, 2),
          MORE_INFANT_THAN_ADULT_ERROR_MESSAGE),
        Arguments.of(
          "total tickets > MAX_TICKETS should throw from ticketAllocationServiceImpl.getAndValidateTotalTicketsRequested",
          buildSingleTicketTypeRequestArgs(MAX_TICKETS + 1, 0, 0),
          String.format("Max tickets allowed is %d, number ADULT & CHILD requested is %d",
            MAX_TICKETS, MAX_TICKETS + 1)),
        Arguments.of(
          "60 Requests with # tickets == 40 so > MAX_TICKETS should throw from ticketAllocationServiceImpl.getAndValidateTotalTicketsRequested ",
          buildSingleTicketTypeRequestArgs(MAX_TICKETS, MAX_TICKETS, MAX_TICKETS),
          MAX_TICKET_TYPE_ERROR_MESSAGE)
      );
    }
  }

  /**
   * Simple helper to build the TicketTypeRequest args: note that each request
   * will represent 1 number of tickets.
   * <br /><br />
   * @param numAdultRequests number of ADULT tickets.
   * @param numChildRequests number of CHILD tickets.
   * @param numInfantRequests number of INFANT tickets.
   * @return array of TicketTypeRequest
   */
  private static TicketTypeRequest[] buildSingleTicketTypeRequestArgs(
    int numAdultRequests,
    int numChildRequests,
    int numInfantRequests
  ) {

    TicketTypeRequest[] arrayAdult = new TicketTypeRequest[numAdultRequests];
    TicketTypeRequest[] arrayChildren = new TicketTypeRequest[numChildRequests];
    TicketTypeRequest[] arrayInfant = new TicketTypeRequest[numInfantRequests];

    if (numAdultRequests > 0) {
      Arrays.fill(arrayAdult, new TicketTypeRequest(ADULT, 1));
    }

    if (numChildRequests > 0) {
      Arrays.fill(arrayChildren, new TicketTypeRequest(CHILD, 1));
    }

    if (numInfantRequests > 0) {
      Arrays.fill(arrayInfant, new TicketTypeRequest(INFANT, 1));
    }

    TicketTypeRequest[] adultAndChildrenArray =  Stream.concat(Arrays.stream(arrayAdult), Arrays.stream(arrayChildren))
      .toArray(TicketTypeRequest[]::new);

    return Stream.concat(Arrays.stream(adultAndChildrenArray), Arrays.stream(arrayInfant))
      .toArray(TicketTypeRequest[]::new);
  }


  @DisplayName("Business Rules / Constraints / assumption tests")
  @Nested
  class BusinessRulesConstraintsAssumptionsTestCase {

    @DisplayName("There are 3 types of tickets i.e. Infant, Child, and Adult & ticket price as per the table.")
    @Test
    void threeTypes() {
      assertEquals(3, values().length);

      assertEquals(BigDecimal.valueOf(20.00), ADULT.ticketCost);
      assertEquals(BigDecimal.valueOf(10.00), CHILD.ticketCost);
      assertEquals(BigDecimal.ZERO, INFANT.ticketCost);
    }

    @DisplayName("Multiple tickets can be purchased at any given time & maximum tickets purchased can only be 20")
    @Test
    void multipleTicketPurchasesMaximumTwenty() {

      // Total: 20 (INFANTs don't count) : { ADULT: 9, CHILD: 11, INFANT: 4 } = £290
      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(ADULT, 1),
        new TicketTypeRequest(ADULT, 3),
        new TicketTypeRequest(CHILD, 1),
        new TicketTypeRequest(INFANT, 2),
        new TicketTypeRequest(INFANT, 2),
        new TicketTypeRequest(ADULT, 1),
        new TicketTypeRequest(CHILD, 1),
        new TicketTypeRequest(CHILD, 9),
        new TicketTypeRequest(ADULT, 3),
        new TicketTypeRequest(ADULT, 1),
      };

      assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, ticketTypeRequests));

      verify(ticketPaymentService, times(1)).makePayment(1L, 290);
      verify(seatReservationService, times(1)).reserveSeat(1L, 20);
    }

    @DisplayName("Maximum number of requests per transaction allowed == 40.")
    @Test
    void maximumTransactionsEquals40Throws() {
      TicketTypeRequest[] tooManyTicketTypeRequests = new TicketTypeRequest[MAX_TICKET_TYPE_REQUESTS_ALLOWED + 1];
      Arrays.fill(tooManyTicketTypeRequests, new TicketTypeRequest(ADULT, 1));


      // should throw as number of tickets requested > MAX_TICKETS (40) but different to the one above
      TicketTypeRequest[] validTicketTypeRequests = new TicketTypeRequest[MAX_TICKET_TYPE_REQUESTS_ALLOWED];
      Arrays.fill(validTicketTypeRequests, new TicketTypeRequest(ADULT, 1));

      // both will throw InvalidPurchaseException but will have different causes / messages

      InvalidPurchaseException tooManyTicketsEx = assertThrows(InvalidPurchaseException.class,
          () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, tooManyTicketTypeRequests));

      assertEquals(MAX_TICKET_TYPE_ERROR_MESSAGE, tooManyTicketsEx.getMessage());
      assertEquals(TicketTypeValidationException.class, tooManyTicketsEx.getCause().getClass());

      InvalidPurchaseException validTicketsEx = assertThrows(InvalidPurchaseException.class,
          () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, validTicketTypeRequests));

      assertEquals("Max tickets allowed is 20, number ADULT & CHILD requested is 40", validTicketsEx.getMessage());
      assertEquals(TicketAllocationException.class, validTicketsEx.getCause().getClass());

      // no calls made
      verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
      verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
    }

    @DisplayName("Maximum number of requests per transaction allowed == 40 (20 ADULT & 20 INFANT is valid and will reserve 20 seats).")
    @Test
    void maximumTransactionsEquals40Valid() {

      TicketTypeRequest[] validTicketTypeRequests = new TicketTypeRequest[MAX_TICKET_TYPE_REQUESTS_ALLOWED];
      Arrays.fill(validTicketTypeRequests, 0, 20, new TicketTypeRequest(ADULT, 1));
      Arrays.fill(validTicketTypeRequests, 20, MAX_TICKET_TYPE_REQUESTS_ALLOWED, new TicketTypeRequest(INFANT, 1));

      assertDoesNotThrow(() -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, validTicketTypeRequests));

      verify(ticketPaymentService, times(1)).makePayment(VALID_ACCOUNT_ID, 400);
      verify(seatReservationService, times(1)).reserveSeat(VALID_ACCOUNT_ID, MAX_TICKETS);
    }

    @DisplayName("CHILD & INFANT tickets cannot be purchased without an ADULT ticket in the request")
    @Test
    void noAdultInRequest() {

      // CHILD only
      TicketTypeRequest[] childOnlyTicketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(CHILD, 1),
      };

      // INFANT only
      TicketTypeRequest[] infantOnlyTicketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(INFANT, 1),
      };

      // CHILD & INFANT only
      TicketTypeRequest[] childAndInfantOnlyTicketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(INFANT, 1),
        new TicketTypeRequest(CHILD, 1),
      };

      InvalidPurchaseException childOnlyEx = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, childOnlyTicketTypeRequests));
      InvalidPurchaseException infantOnlyEx = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, infantOnlyTicketTypeRequests));
      InvalidPurchaseException childAndInfantOnlyEx = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, childAndInfantOnlyTicketTypeRequests));

      assertEquals(NO_ADULT_TYPES_ERROR_MESSAGE, childOnlyEx.getMessage());
      assertEquals(NO_ADULT_TYPES_ERROR_MESSAGE, infantOnlyEx.getMessage());
      assertEquals(NO_ADULT_TYPES_ERROR_MESSAGE, childAndInfantOnlyEx.getMessage());

      // no calls made from all 3 scenarios
      verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
      verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
    }

    @DisplayName("INFANTs do not pay for a ticket and are not allocated a seat, cost of ticket is ADULT price only: £20")
    @Test
    void infantNotAllocatedSeatOrAddToTotal() {

      // ADULT & INFANT
      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(ADULT, 1),
        new TicketTypeRequest(INFANT, 1),
      };

      assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, ticketTypeRequests));

      // 1 Seat reserved at cost of 1 ADULT ticket : £20
      verify(ticketPaymentService, times(1)).makePayment(1L, 20);
      verify(seatReservationService, times(1)).reserveSeat(1L, 1);
    }

    @DisplayName("# INFANTs can not be more than ADULTs")
    @Test
    void infantNotMoreThanAdult() {

      // ADULT (1) & INFANT (2)
      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(ADULT, 1),
        new TicketTypeRequest(INFANT, 2),
      };

      InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,  () -> ticketService.purchaseTickets(1L, ticketTypeRequests));

      assertEquals(MORE_INFANT_THAN_ADULT_ERROR_MESSAGE, ex.getMessage());
      assertEquals(TicketAllocationException.class, ex.getCause().getClass());

      verify(ticketPaymentService, times(0)).makePayment(anyLong(), anyInt());
      verify(seatReservationService, times(0)).reserveSeat(anyLong(), anyInt());
    }

    @DisplayName("All accounts with an id greater than zero are valid.")
    @ParameterizedTest
    @ValueSource(longs = { 1, Long.MAX_VALUE })
    void allAccountsWithIdGreaterThan0Valid(Long accountId) {

      // ADULT & INFANT
      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
        new TicketTypeRequest(ADULT, 1),
        new TicketTypeRequest(INFANT, 1),
      };

      assertDoesNotThrow(() -> ticketService.purchaseTickets(accountId, ticketTypeRequests));

      // 1 Seat reserved at cost of 1 ADULT ticket : £20
      verify(ticketPaymentService, times(1)).makePayment(accountId, 20);
      verify(seatReservationService, times(1)).reserveSeat(accountId, 1);
    }
  }
}
