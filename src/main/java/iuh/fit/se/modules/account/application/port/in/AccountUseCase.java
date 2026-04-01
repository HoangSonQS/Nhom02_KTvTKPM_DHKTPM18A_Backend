package iuh.fit.se.modules.account.application.port.in;

import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.modules.account.domain.Address;

/**
 * AccountUseCase — Inbound Port (Public API).
 */
public interface AccountUseCase {

    Account getProfile(Long userId);

    Account updateProfile(Long userId, UpdateProfileCommand command);

    Account addAddress(Long userId, AddressCommand command);

    record UpdateProfileCommand(String phoneNumber, byte[] avatarFile) {
    }

    record AddressCommand(String street, String ward, String district, String city, boolean isDefault) {
    }
}
