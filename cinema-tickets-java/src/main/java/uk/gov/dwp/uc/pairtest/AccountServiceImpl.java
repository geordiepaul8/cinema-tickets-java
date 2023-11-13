package uk.gov.dwp.uc.pairtest;

import java.util.Objects;
import uk.gov.dwp.uc.pairtest.exception.AccountValidationException;

/**
 * Account service.
 */
public class AccountServiceImpl implements AccountService {
  @Override
  public void validateAccount(Long accountId) {
    // validate accountId - all accounts with id > 0 are considered valid
    if (Objects.isNull(accountId) || accountId <= 0L) {
      throw new AccountValidationException("The account id supplied is invalid");
    }
  }
}
