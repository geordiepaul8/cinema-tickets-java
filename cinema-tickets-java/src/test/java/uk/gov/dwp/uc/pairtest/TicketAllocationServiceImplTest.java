package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.dwp.uc.pairtest.TicketAllocationServiceImpl.MAX_TICKETS;
import static uk.gov.dwp.uc.pairtest.domain.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.Type.INFANT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.gov.dwp.uc.pairtest.domain.Type;
import uk.gov.dwp.uc.pairtest.exception.TicketAllocationException;

@DisplayName("Ticket Allocation Service Impl tests")
public class TicketAllocationServiceImplTest {

  /** Ticket allocation service. */
  private TicketAllocationService ticketAllocationService;

  public static final String MORE_INFANT_THAN_ADULT_ERROR_MESSAGE = "There was more INFANT tickets requested than ADULT";

  public static final String NO_TICKET_TYPES_ERROR_MESSAGE = "No ticket types are supplied";

  public static final String NO_ADULT_TYPES_ERROR_MESSAGE = "There was 0 ADULT tickets included in the request";

  @BeforeEach
  void beforeEach() {
    ticketAllocationService = new TicketAllocationServiceImpl();
  }

  @DisplayName("Get and validate total tickets requested")
  @Nested
  class GetAndValidateTotalTicketsRequestedTestClass{

    @DisplayName("Should throw TicketAllocationException when tickets map is empty or null")
    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowTicketAllocationExceptionWhenTicketsMapIsEmptyOrNull(Map<Type, Integer> ticketsMap) {

      TicketAllocationException ex = assertThrows(TicketAllocationException.class,
        () -> ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap));

      assertEquals(NO_TICKET_TYPES_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Should throw TicketAllocationException when 0 ADULT types are in request")
    @ParameterizedTest
    @MethodSource("provideTicketRequestsForNoAdultType")
    void shouldThrowTicketAllocationExceptionWhenNoAdultTypesInRequest(Map<Type, Integer> ticketsMap) {

      TicketAllocationException ex = assertThrows(TicketAllocationException.class,
        () -> ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap));

        assertEquals(NO_ADULT_TYPES_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Should not throw TicketAllocationException when at least 1 ADULT types are in request")
    @Test
    void shouldNotThrowTicketAllocationExceptionWhenOneAdultTypesInRequest() {

      Map<Type, Integer> ticketsMap = Map.ofEntries(Map.entry(ADULT, 1));

      assertDoesNotThrow(() -> ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap));
    }

    @DisplayName("Should throw TicketAllocationException when # INFANT tickets exceed # ADULT tickets")
    @Test
    void shouldThrowTicketAllocationExceptionWhenNumInfantExceedsAdult() {

      Map<Type, Integer> ticketsMap = Map.ofEntries(Map.entry(ADULT, 1), Map.entry(INFANT, 3));

      TicketAllocationException ex = assertThrows(TicketAllocationException.class,
        () -> ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap));

      assertEquals(MORE_INFANT_THAN_ADULT_ERROR_MESSAGE, ex.getMessage());
    }

    @DisplayName("Should not throw TicketAllocationException when # INFANT tickets does not exceed # ADULT tickets")
    @ParameterizedTest
    @MethodSource("provideTicketRequestsForInfantNotExceedingAdult")
    void shouldNotThrowTicketAllocationExceptionWhenNumInfantDoesNotExceedsAdult(Map<Type, Integer> ticketsMap) {

      assertDoesNotThrow(() -> ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap));
    }

    @DisplayName("Should throw TicketAllocationException when ADULT + CHILD types exceed MAX_TICKETS allowed")
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTicketRequestsForAdultAndChildExceedMaxTickets")
    void shouldThrowTicketAllocationExceptionAdultAndChildExceedMaxRequests(String testDescription,
      Map<Type, Integer> ticketsMap, int expectedTotalRequested) {

     String expectedMessage = String.format("Max tickets allowed is %d, number ADULT & CHILD requested is %d",
        MAX_TICKETS, expectedTotalRequested);

      TicketAllocationException ex = assertThrows(TicketAllocationException.class,
        () -> ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap));

      assertEquals(expectedMessage, ex.getMessage());
    }

    @DisplayName("Should NOT throw TicketAllocationException when ADULT + CHILD types do not exceed MAX_TICKETS allowed & returns valid total")
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTicketRequestsForAdultAndChildDoesNotExceedMaxTickets")
    void shouldNotThrowTicketAllocationExceptionAdultAndChildDoNotExceedMaxRequests(String testDescription,
                                                                           Map<Type, Integer> ticketsMap, int expectedTotalRequested) {

      int totalTicketsRequested = ticketAllocationService.getAndValidateTotalTicketsRequested(ticketsMap);

      assertEquals(expectedTotalRequested, totalTicketsRequested);
    }


    private static Stream<Arguments> provideTicketRequestsForAdultAndChildDoesNotExceedMaxTickets() {
      return Stream.of(
        // ADULT == MAX_TICKETS
        Arguments.of(
          "ADULT only in request == MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS)),
          MAX_TICKETS
        ),
        // ADULT < MAX_TICKETS
        Arguments.of(
          "ADULT only in request < MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, 1)),
          1
        ),
        // ADULT & CHILD == MAX_TICKETS
        Arguments.of(
          "ADULT & CHILD == MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS - 10), Map.entry(CHILD, MAX_TICKETS - 10)),
          MAX_TICKETS
        ),
        // ADULT & CHILD < MAX_TICKETS
        Arguments.of(
          "ADULT & CHILD < MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, 1), Map.entry(CHILD, MAX_TICKETS - 2)),
          MAX_TICKETS - 1
        ),

        // ADULT == MAX_TICKETS (with INFANT)
        Arguments.of(
          "ADULT only in request == MAX_TICKETS, INFANT total does not count",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS), Map.entry(INFANT, 5)),
          MAX_TICKETS
        ),
        // ADULT < MAX_TICKETS (with infant)
        Arguments.of(
          "ADULT only in request < MAX_TICKETS, INFANT total does not count",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS - 1), Map.entry(INFANT, MAX_TICKETS - 1)),
          MAX_TICKETS - 1
        ),
        // ADULT & CHILD == MAX_TICKETS (with INFANT)
        Arguments.of(
          "ADULT & CHILD == MAX_TICKETS, INFANT total does not count",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS - 10), Map.entry(CHILD, MAX_TICKETS - 10), Map.entry(INFANT, MAX_TICKETS - 10)),
          MAX_TICKETS
        ),
        // ADULT & CHILD < MAX_TICKETS, INFANT total does not count
        Arguments.of(
          "ADULT & CHILD < MAX_TICKETS, INFANT total does not count",
          Map.ofEntries(Map.entry(ADULT, 1), Map.entry(CHILD, MAX_TICKETS - 2), Map.entry(INFANT, 1)),
          MAX_TICKETS - 1
        )
      );
    }

    private static Stream<Arguments> provideTicketRequestsForAdultAndChildExceedMaxTickets() {
      return Stream.of(
        // ADULT > MAX_TICKETS
        Arguments.of(
          "ADULT only in request exceeds MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS + 1)),
          MAX_TICKETS + 1
        ),
        // ADULT & CHILD (greater than 0) > MAX_TICKETS
        Arguments.of(
          "ADULT & CHILD (greater than 0) > MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS), Map.entry(CHILD, 1)),
          MAX_TICKETS + 1
        ),
        // ADULT & CHILD (equal to 0) > MAX_TICKETS
        Arguments.of(
          "ADULT & CHILD (equal to 0) > MAX_TICKETS",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS + 1), Map.entry(CHILD, 0)),
          MAX_TICKETS + 1
        ),
        // ADULT only exceeds with INFANT also
        Arguments.of(
          "ADULT only in request > MAX_TICKETS, INFANT totals do not count",
          Map.ofEntries(Map.entry(ADULT, MAX_TICKETS + 1), Map.entry(INFANT, 5)),
          MAX_TICKETS + 1
        )
      );
    }

    private static Stream<Arguments> provideTicketRequestsForInfantNotExceedingAdult() {
      return Stream.of(
        // INFANT == ADULT (no CHILD)
        Arguments.of(Map.ofEntries(Map.entry(INFANT, 1), Map.entry(ADULT, 1))),
        // INFANT == ADULT (with CHILD)
        Arguments.of(Map.ofEntries(Map.entry(INFANT, 1), Map.entry(ADULT, 1), Map.entry(CHILD, 6))),
        // INFANT < ADULT (NO CHILD)
        Arguments.of(Map.ofEntries(Map.entry(ADULT, 2), Map.entry(INFANT, 1))),
        // INFANT < ADULT (with CHILD)
        Arguments.of(Map.ofEntries(Map.entry(ADULT, 2), Map.entry(INFANT, 1), Map.entry(CHILD, 6)))
      );
    }

    private static Stream<Arguments> provideTicketRequestsForNoAdultType() {
      return Stream.of(
        // only CHILD type
        Arguments.of(Map.of(CHILD, 1)),
        // only INFANT type
        Arguments.of(Map.of(INFANT, 1)),
        // only INFANT and CHILD type
        Arguments.of(Map.ofEntries(Map.entry(CHILD, 1), Map.entry(INFANT, 3)))
      );
    }
  }


  @DisplayName("Get total tickets per type requested")
  @Nested
  class GetTotalTicketsPerTypeTestClass{

    // Using reflection to get access to the private method for testing purposes
    private Method getTotalTicketsPerType;

    @BeforeEach
    void beforeEachGetTotalTicketsPerTypeTestClass() throws NoSuchMethodException {
      getTotalTicketsPerType = ticketAllocationService.getClass().getDeclaredMethod("getTotalTicketsPerType", Map.class, Type.class);
      getTotalTicketsPerType.setAccessible(true);
    }

    @DisplayName("if Type is not contained as a key in the map, return 0")
    @Test
    void return0WhenTypeNotContainedInMap() throws InvocationTargetException, IllegalAccessException {

      int total = (int) getTotalTicketsPerType.invoke(ticketAllocationService, Map.of(ADULT, 1), CHILD);

      assertEquals(0, total);
    }

    @DisplayName("if Type is contained as a key in the map, return the corresponding value")
    @Test
    void returnValueOfTypeWhenTypeContainedInMap() {

      Map<Type, Integer> ticketsMap = Map.ofEntries(
        Map.entry(ADULT, 1),
        Map.entry(CHILD, 2),
        Map.entry(INFANT, 3));

      assertAll("Number of tickets requested per Type",
        () -> assertEquals(1, (int) getTotalTicketsPerType.invoke(ticketAllocationService, ticketsMap, ADULT)),
        () -> assertEquals(2, (int) getTotalTicketsPerType.invoke(ticketAllocationService, ticketsMap, CHILD)),
        () -> assertEquals(3, (int) getTotalTicketsPerType.invoke(ticketAllocationService, ticketsMap, INFANT))
      );
    }
  }

  @DisplayName("Calculate total amount from the number of tickets requested")
  @Nested
  class CalculateTotalAmountFromTicketsRequestedTestClass{

    @DisplayName("Returns valid cost of tickets per type")
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTicketCostsFotValidCostOfTickets")
    void returnsValidCostOfTicketsPerType(
      String description, Map<Type, Integer> ticketsMap ,int expectedTotalCostOfTickets) {

      int totalTicketCost = ticketAllocationService.calculateTotalAmountFromTicketsRequested(ticketsMap);

      assertEquals(expectedTotalCostOfTickets, totalTicketCost);
    }

    private static Stream<Arguments> provideTicketCostsFotValidCostOfTickets() {
      return Stream.of(
        // 0 ADULT
        Arguments.of("Should return £0 for 0 x ADULT", Map.ofEntries(Map.entry(ADULT, 0)), 0),
        // 1 ADULT
        Arguments.of("Should return £20.00 for 1 x ADULT", Map.ofEntries(Map.entry(ADULT, 1)), 20),
        // 2 ADULT
        Arguments.of("Should return £40.00 for 2 x ADULT", Map.ofEntries(Map.entry(ADULT, 2)), 40),
        // 3 ADULT
        Arguments.of("Should return £60.00 for 3 x ADULT", Map.ofEntries(Map.entry(ADULT, 3)), 60),
        // MAX_TICKETS ADULT
        Arguments.of("Should return £400.00 for 20 x ADULT", Map.ofEntries(Map.entry(ADULT, MAX_TICKETS)), 400),

        // 0 CHILD
        Arguments.of("Should return £0 for 0 x CHILD", Map.ofEntries(Map.entry(CHILD, 0)), 0),
        // 1 CHILD
        Arguments.of("Should return £10.00 for 1 x CHILD", Map.ofEntries(Map.entry(CHILD, 1)), 10),
        // 2 CHILD
        Arguments.of("Should return £20.00 for 2 x CHILD", Map.ofEntries(Map.entry(CHILD, 2)), 20),
        // 3 CHILD
        Arguments.of("Should return £30.00 for 3 x CHILD", Map.ofEntries(Map.entry(CHILD, 3)), 30),
        // MAX_TICKETS CHILD
        Arguments.of("Should return £200.00 for 20 x CHILD", Map.ofEntries(Map.entry(CHILD, MAX_TICKETS)), 200),

        // 0 INFANT
        Arguments.of("Should return £0 for 0 x INFANT", Map.ofEntries(Map.entry(INFANT, 0)), 0),
        // 1 INFANT
        Arguments.of("Should return £0 for 1 x INFANT", Map.ofEntries(Map.entry(INFANT, 1)), 0),
        // 2 INFANT
        Arguments.of("Should return £0 for 2 x INFANT", Map.ofEntries(Map.entry(INFANT, 2)), 0),
        // 3 INFANT
        Arguments.of("Should return £0 for 3 x INFANT", Map.ofEntries(Map.entry(INFANT, 3)), 0),
        // MAX_TICKETS INFANT
        Arguments.of("Should return £0 for 20 x INFANT", Map.ofEntries(Map.entry(INFANT, MAX_TICKETS)), 0),

        // ADULT / INFANT / CHILD
        Arguments.of("Should return £50 for 2 x ADULT, 1 x CHILD & 2 x INFANT",
            Map.ofEntries(Map.entry(INFANT, 2), Map.entry(ADULT, 2), Map.entry(CHILD, 1)), 50),
        Arguments.of("Should return £40 for 2 x ADULT, 0 x CHILD & 2 x INFANT",
          Map.ofEntries(Map.entry(INFANT, 2), Map.entry(ADULT, 2), Map.entry(CHILD, 0)), 40),
        Arguments.of("Should return £40 for 2 x ADULT, 0 x CHILD & 2 x INFANT (Child not included)",
          Map.ofEntries(Map.entry(INFANT, 2), Map.entry(ADULT, 2)), 40)
      );
    }
  }
}