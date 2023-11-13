package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.uc.pairtest.exception.AccountValidationException;

@DisplayName("Account Service Impl tests")
public class AccountServiceImplTest {
  /** Account service. */
  private AccountService accountService;

  public static final String ACCOUNT_ID_ERROR_MESSAGE = "The account id supplied is invalid";

  @BeforeEach
  void beforeEach() {
    accountService = new AccountServiceImpl();
  }

  @DisplayName("Assumption: All accounts with an id greater than zero are valid.")
  @Nested
  class AccountIdValidationTestClass{

    @DisplayName("Throws AccountValidationException when account id parameter is null, 0 or less")
    @ParameterizedTest(name = "Throws AccountValidationExceptionAccountValidationException when account id parameter is {0}")
    @NullSource
    @ValueSource(longs = { Long.MIN_VALUE, -1L, 0L })
    void validateAccountThrowsAccountValidationExceptionWhenAccountIdIs0OrLess(Long accountId) {

      AccountValidationException accountValidationException = assertThrows(AccountValidationException.class, () ->
        accountService.validateAccount(accountId));

      assertEquals(ACCOUNT_ID_ERROR_MESSAGE, accountValidationException.getMessage());
    }

    @DisplayName("Does not throw AccountValidationException when account id parameter is > 0")
    @ParameterizedTest(name = "Does not throw AccountValidationException when account id parameter is {0}")
    @ValueSource(longs = { 1L, Long.MAX_VALUE })
    void validateAccountDoesNotThrowAccountValidationExceptionWhenAccountIdIsGreaterThan0(Long accountId) {
      assertDoesNotThrow(() -> accountService.validateAccount(accountId));
    }
  }
}