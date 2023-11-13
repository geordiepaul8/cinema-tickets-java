package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.exception.AccountValidationException;

/**
 * Account service interface.
 */
public interface AccountService {

  /**
   * Validates the account id is greater than 0.
   * <br /><br />
   *
   * @param accountId the account id.
   * @throws AccountValidationException when null or when not > 0.
   */
  void validateAccount(Long accountId);

}
