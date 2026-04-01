package iuh.fit.se.modules.account.application.port.out;

import iuh.fit.se.modules.account.domain.Account;

import java.util.Optional;

/**
 * AccountPersistencePort — Outbound Port (Infrastructure API).
 */
public interface AccountPersistencePort {

    Optional<Account> findByUserId(Long userId);

    Account save(Account account);
}
