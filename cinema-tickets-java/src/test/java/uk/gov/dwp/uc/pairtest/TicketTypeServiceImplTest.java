package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.dwp.uc.pairtest.TicketAllocationServiceImpl.MAX_TICKETS;
import static uk.gov.dwp.uc.pairtest.TicketTypeServiceImpl.MAX_TICKET_TYPE_REQUESTS_ALLOWED;
import static uk.gov.dwp.uc.pairtest.domain.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.Type.INFANT;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.TicketTypeValidationException;

@DisplayName("Ticket Type Service Impl tests")
public class TicketTypeServiceImplTest {

  /** Ticket type service. */
  private TicketTypeService ticketTypeService;

  public static final String TICKET_TYPE_ERROR_MESSAGE = "There was an error with a ticket type";

  public static final String NUM_TICKET_TYPE_ERROR_MESSAGE = "There was an error with a requested number of tickets";

  public static final String MAX_TICKET_TYPE_ERROR_MESSAGE = "The maximum ticketTypeRequests allowed is exceeded";

  public static final String TICKET_REQUEST_EMPTY_ERROR_MESSAGE = "The ticketTypeRequests supplied is empty";

  @BeforeEach
  void setUp() {
    ticketTypeService = new TicketTypeServiceImpl();
  }

  @DisplayName("Validate Ticket Type Requests args.")
  @Nested
  class TicketTypeRequestsArgsValidationTestClass{
    @DisplayName("Throws TicketTypeValidationException when TicketTypeRequests args is null or empty")
    @NullAndEmptySource
    @ParameterizedTest(name = "Invalid TicketTypeRequests = {0}")
    void throwsTicketTypeValidationExceptionWhenNullOrEmpty(TicketTypeRequest[] requests) {

      TicketTypeValidationException ticketTypeValidationException = assertThrows(TicketTypeValidationException.class, () ->
          ticketTypeService.validateTicketTypeRequest(requests));

      assertEquals(TICKET_REQUEST_EMPTY_ERROR_MESSAGE, ticketTypeValidationException.getMessage());
    }

    @DisplayName("Throws TicketTypeValidationException when TicketTypeRequests args > MAX_TICKET_TYPE_REQUESTS_ALLOWED")
    @Test
    void throwsTicketTypeValidationExceptionWhenGreaterThanMaxAllowed() {

      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[MAX_TICKET_TYPE_REQUESTS_ALLOWED + 1];

      TicketTypeValidationException ticketTypeValidationException = assertThrows(TicketTypeValidationException.class, () ->
        ticketTypeService.validateTicketTypeRequest(ticketTypeRequests));

      assertEquals(MAX_TICKET_TYPE_ERROR_MESSAGE, ticketTypeValidationException.getMessage());
    }

    @DisplayName("Does not Throw when TicketTypeRequests args is size 1")
    @Test
    void validateTicketTypeRequestsDoesNotThrowWhenValidSize1() {

      TicketTypeRequest ticketTypeRequestAdult = new TicketTypeRequest(ADULT, 1);

      TicketTypeRequest[] ticketTypeRequests = { ticketTypeRequestAdult };

      assertDoesNotThrow(() -> ticketTypeService.validateTicketTypeRequest(ticketTypeRequests));
    }

    @DisplayName("Does not Throw when TicketTypeRequests args is size > 1")
    @Test
    void validateTicketTypeRequestsDoesNotThrowWhenValidSizeGreaterThan1() {

      TicketTypeRequest ticketTypeRequestAdult = new TicketTypeRequest(ADULT, 1);
      TicketTypeRequest ticketTypeRequestChild = new TicketTypeRequest(CHILD, 1);

      TicketTypeRequest[] ticketTypeRequests = { ticketTypeRequestAdult, ticketTypeRequestChild };

      assertDoesNotThrow(() -> ticketTypeService.validateTicketTypeRequest(ticketTypeRequests));
    }

    @DisplayName("Does not Throw when TicketTypeRequests args == MAX_TICKET_TYPE_REQUESTS_ALLOWED")
    @Test
    void validateTicketTypeRequestsDoesNotThrowWhenValidSizeEqualToMaxAllowed() {

      TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[MAX_TICKET_TYPE_REQUESTS_ALLOWED];

      assertDoesNotThrow(() -> ticketTypeService.validateTicketTypeRequest(ticketTypeRequests));
    }
  }


  @DisplayName("Get number of tickets per type")
  @Nested
  class GetNumberOfTicketsPerTypeTestCase {

    @DisplayName("Throws TicketTypeValidationException when at least 1 ticket type is null in single request")
    @Test
    void throwsTicketTypeValidationExceptionWhenSingleTicketRequestTicketTypeNull() {

      TicketTypeRequest invalidTicketTypeRequest = new TicketTypeRequest(null, 1);

      TicketTypeRequest[] ticketTypeRequests = { invalidTicketTypeRequest };

      TicketTypeValidationException ex = assertThrows (TicketTypeValidationException.class,
        () -> ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests));

      assertEquals(TICKET_TYPE_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Throws TicketTypeValidationException when at least 1 ticket type is null in multiple requests")
    @Test
    void throwsTicketTypeValidationExceptionWhenMultipleTicketRequestTicketTypeNull() {

      TicketTypeRequest invalidTicketTypeRequest = new TicketTypeRequest(null, 1);
      TicketTypeRequest validTicketTypeRequest = new TicketTypeRequest(ADULT, 1);

      TicketTypeRequest[] ticketTypeRequests = { validTicketTypeRequest, invalidTicketTypeRequest };

      TicketTypeValidationException ex = assertThrows (TicketTypeValidationException.class,
        () -> ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests));

      assertEquals(TICKET_TYPE_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Throws TicketTypeValidationException when get number of tickets is <=> 0")
    @ParameterizedTest(name = "Number of tickets requested is {0}")
    @ValueSource(ints = { Integer.MIN_VALUE, -1, 0 })
    void throwsTicketTypeValidationExceptionWhenNumTicketsLessThanEqualTo0(int invalidNumTicketsRequested) {

      TicketTypeRequest invalidTicketTypeRequest = new TicketTypeRequest(ADULT, invalidNumTicketsRequested);

      TicketTypeRequest[] ticketTypeRequests = { invalidTicketTypeRequest };

      TicketTypeValidationException ex = assertThrows (TicketTypeValidationException.class,
        () -> ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests));

      assertEquals(NUM_TICKET_TYPE_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Returns Map containing all 3 Types (ADULT, INFANT, CHILD) with valid totals")
    @Test
    void validTicketsRequestedWithAll3Types() {

      int expectedNumAdultTicketsRequested = 2;
      int expectedNumChildTicketsRequested = 1;
      int expectedNumInfantTicketsRequested = 1;

      TicketTypeRequest validAdultTicketTypeRequest = new TicketTypeRequest(ADULT, expectedNumAdultTicketsRequested);
      TicketTypeRequest validChildTicketTypeRequest = new TicketTypeRequest(CHILD, expectedNumChildTicketsRequested);
      TicketTypeRequest validInfantTicketTypeRequest = new TicketTypeRequest(INFANT, expectedNumInfantTicketsRequested);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest, validChildTicketTypeRequest, validInfantTicketTypeRequest };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());

      assertAll("Number of tickets requested per Type",
        () -> assertEquals(expectedNumAdultTicketsRequested, ticketsMap.get(ADULT)),
        () -> assertEquals(expectedNumChildTicketsRequested, ticketsMap.get(CHILD)),
        () -> assertEquals(expectedNumInfantTicketsRequested, ticketsMap.get(INFANT))
      );
    }

    @DisplayName("Returns Map containing all 3 Types (ADULT, INFANT, CHILD) with valid totals when multiple Types submitted")
    @Test
    void validTicketsRequestedWithAll3TypesWithMultipleTypesSubmitted() {

      int expectedNumAdultTicketsRequested = 5;
      int expectedNumChildTicketsRequested = 3;
      int expectedNumInfantTicketsRequested = 4;

      TicketTypeRequest validAdultTicketTypeRequest1 = new TicketTypeRequest(ADULT, 3);
      TicketTypeRequest validAdultTicketTypeRequest2 = new TicketTypeRequest(ADULT, 2);
      TicketTypeRequest validChildTicketTypeRequest1 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validChildTicketTypeRequest2 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validChildTicketTypeRequest3 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validInfantTicketTypeRequest1 = new TicketTypeRequest(INFANT, 1);
      TicketTypeRequest validInfantTicketTypeRequest2 = new TicketTypeRequest(INFANT, 3);

      TicketTypeRequest[] ticketTypeRequests = {
        validChildTicketTypeRequest3, validAdultTicketTypeRequest1, validInfantTicketTypeRequest1,
        validChildTicketTypeRequest1, validAdultTicketTypeRequest2, validChildTicketTypeRequest2,
        validInfantTicketTypeRequest2
      };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());
      assertAll("Number of tickets requested per Type",
        () -> assertEquals(expectedNumAdultTicketsRequested, ticketsMap.get(ADULT)),
        () -> assertEquals(expectedNumChildTicketsRequested, ticketsMap.get(CHILD)),
        () -> assertEquals(expectedNumInfantTicketsRequested, ticketsMap.get(INFANT))
      );
    }

    @DisplayName("Throws TicketTypeValidationException when multiple Types submitted and a single one is null type")
    @Test
    void throwsTicketTypeValidationExceptionWhenMultipleRequestsSubmittedButOneIsNullType() {

      TicketTypeRequest invalidTicketTypeRequest = new TicketTypeRequest(null, 1); // invalid request

      TicketTypeRequest validAdultTicketTypeRequest1 = new TicketTypeRequest(ADULT, 3);
      TicketTypeRequest validAdultTicketTypeRequest2 = new TicketTypeRequest(ADULT, 2);
      TicketTypeRequest validChildTicketTypeRequest1 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validChildTicketTypeRequest2 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validInfantTicketTypeRequest1 = new TicketTypeRequest(INFANT, 1);
      TicketTypeRequest validInfantTicketTypeRequest2 = new TicketTypeRequest(INFANT, 1);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest1, validInfantTicketTypeRequest1,
        validChildTicketTypeRequest1, validAdultTicketTypeRequest2, invalidTicketTypeRequest, validChildTicketTypeRequest2,
        validInfantTicketTypeRequest2 };

      TicketTypeValidationException ex = assertThrows (TicketTypeValidationException.class,
        () -> ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests));

      assertEquals(TICKET_TYPE_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Throws TicketTypeValidationException when multiple Types submitted and a single one is has invalid number of tickets")
    @ParameterizedTest(name = "Number of tickets requested is {0}")
    @ValueSource(ints = { Integer.MIN_VALUE, -1, 0, (MAX_TICKETS + 1) })
    void throwsTicketTypeValidationExceptionWhenMultipleRequestsSubmittedButOneIsInvalidNumTickets(int invalidNumTickets) {

      TicketTypeRequest invalidTicketTypeRequest = new TicketTypeRequest(ADULT, invalidNumTickets); // invalid request

      TicketTypeRequest validAdultTicketTypeRequest1 = new TicketTypeRequest(ADULT, 3);
      TicketTypeRequest validAdultTicketTypeRequest2 = new TicketTypeRequest(ADULT, 2);
      TicketTypeRequest validChildTicketTypeRequest1 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validChildTicketTypeRequest2 = new TicketTypeRequest(CHILD, 1);
      TicketTypeRequest validInfantTicketTypeRequest1 = new TicketTypeRequest(INFANT, 1);
      TicketTypeRequest validInfantTicketTypeRequest2 = new TicketTypeRequest(INFANT, 1);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest1, validInfantTicketTypeRequest1,
        validChildTicketTypeRequest1, validAdultTicketTypeRequest2, invalidTicketTypeRequest, validChildTicketTypeRequest2,
        validInfantTicketTypeRequest2 };

      TicketTypeValidationException ex = assertThrows (TicketTypeValidationException.class,
        () -> ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests));

      assertEquals(NUM_TICKET_TYPE_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Returns Map containing all valid totals when between 1 = MAX_TICKETS")
    @ParameterizedTest(name = "Number of tickets requested for ADULT is {0}")
    @ValueSource(ints = { 1, MAX_TICKETS })
    void validTicketsRequestedWithAll3TypesWithAdultAbove0(int expectedNumAdultTicketsRequested) {

      int expectedNumChildTicketsRequested = 2; // checks that the validation does not get triggered
      int expectedNumInfantTicketsRequested = 1;

      TicketTypeRequest validAdultTicketTypeRequest = new TicketTypeRequest(ADULT, expectedNumAdultTicketsRequested);
      TicketTypeRequest validChildTicketTypeRequest = new TicketTypeRequest(CHILD, expectedNumChildTicketsRequested);
      TicketTypeRequest validInfantTicketTypeRequest = new TicketTypeRequest(INFANT, expectedNumInfantTicketsRequested);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest, validChildTicketTypeRequest, validInfantTicketTypeRequest };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());
      assertAll("Number of tickets requested per Type",
        () -> assertEquals(expectedNumAdultTicketsRequested, ticketsMap.get(ADULT)),
        () -> assertEquals(expectedNumChildTicketsRequested, ticketsMap.get(CHILD)),
        () -> assertEquals(expectedNumInfantTicketsRequested, ticketsMap.get(INFANT))
      );
    }

    @DisplayName("Returns Map containing all 3 Types (ADULT, INFANT, CHILD) when only 1 Type is submitted: ADULT")
    @Test
    void validTicketsRequestedWithAll3TypesWhenOnly1SubmittedAdult() {

      int numTicketsRequested = 2;

      TicketTypeRequest validAdultTicketTypeRequest = new TicketTypeRequest(ADULT, numTicketsRequested);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());
      assertAll("Number of tickets requested per Type",
        () -> assertEquals(numTicketsRequested, ticketsMap.get(ADULT)),
        () -> assertEquals(0, ticketsMap.get(CHILD)),
        () -> assertEquals(0, ticketsMap.get(INFANT))
      );
    }

    @DisplayName("Returns Map containing all 3 Types (ADULT, INFANT, CHILD) when only 1 Type is submitted: CHILD")
    @Test
    void validTicketsRequestedWithAll3TypesWhenOnly1SubmittedChild() {

      int numTicketsRequested = 2;

      TicketTypeRequest validAdultTicketTypeRequest = new TicketTypeRequest(CHILD, numTicketsRequested);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());
      assertAll("Number of tickets requested per Type",
        () -> assertEquals(numTicketsRequested, ticketsMap.get(CHILD)),
        () -> assertEquals(0, ticketsMap.get(ADULT)),
        () -> assertEquals(0, ticketsMap.get(INFANT))
      );
    }

    @DisplayName("Returns Map containing all 3 Types (ADULT, INFANT, CHILD) when only 1 Type is submitted: INFANT")
    @Test
    void validTicketsRequestedWithAll3TypesWhenOnly1SubmittedInfant() {

      int numTicketsRequested = 2;

      TicketTypeRequest validAdultTicketTypeRequest = new TicketTypeRequest(INFANT, numTicketsRequested);

      TicketTypeRequest[] ticketTypeRequests = { validAdultTicketTypeRequest };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());
      assertAll("Number of tickets requested per Type",
        () -> assertEquals(numTicketsRequested, ticketsMap.get(INFANT)),
        () -> assertEquals(0, ticketsMap.get(CHILD)),
        () -> assertEquals(0, ticketsMap.get(ADULT))
      );
    }

    @DisplayName("Returns Map containing all 3 Types (ADULT, INFANT, CHILD) when No Type is submitted")
    @Test
    void validTicketsRequestedWithAll3TypesWhenOnlyNoTypeSubmitted() {

      TicketTypeRequest[] ticketTypeRequests = { };

      Map<Type, Integer> ticketsMap = ticketTypeService.getNumberOfTicketsPerType(ticketTypeRequests);

      assertEquals(3, ticketsMap.keySet().size());
      assertAll("Number of tickets requested per Type",
        () -> assertEquals(0, ticketsMap.get(INFANT)),
        () -> assertEquals(0, ticketsMap.get(CHILD)),
        () -> assertEquals(0, ticketsMap.get(ADULT))
      );
    }
  }
}