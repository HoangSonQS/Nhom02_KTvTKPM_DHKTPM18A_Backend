package iuh.fit.se.modules.account.adapter.outbound.persistence;

import iuh.fit.se.modules.account.application.port.out.AccountPersistencePort;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.modules.account.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AccountPersistenceAdapter — Mapping giữa Domain và JPA Entities.
 * Thực thi Persistence Port.
 */
@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountPersistencePort {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Optional<Account> findByUserId(Long userId) {
        return accountJpaRepository.findByUserId(userId).map(this::mapToDomain);
    }

    @Override
    public Account save(Account account) {
        // Cập nhật/Tạo mới JPA Entity từ Domain Entity
        AccountJpaEntity entity = mapToJpa(account);
        AccountJpaEntity saved = accountJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    // Mapping: Jpa -> Domain
    private Account mapToDomain(AccountJpaEntity entity) {
        return Account.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .phoneNumber(entity.getPhoneNumber())
                .avatarUrl(entity.getAvatarUrl())
                .avatarPublicId(entity.getAvatarPublicId())
                .isDeleted(entity.isDeleted())
                .addresses(entity.getAddresses().stream().map(this::mapToDomainAddress).collect(Collectors.toList()))
                .build();
    }

    private Address mapToDomainAddress(AddressJpaEntity entity) {
        return Address.builder()
                .id(entity.getId())
                .recipientName(entity.getRecipientName())
                .phoneNumber(entity.getPhoneNumber())
                .street(entity.getStreet())
                .ward(entity.getWard())
                .city(entity.getCity())
                .isDefault(entity.isDefault())
                .build();
    }

    // Mapping: Domain -> Jpa
    private AccountJpaEntity mapToJpa(Account domain) {
        AccountJpaEntity entity = accountJpaRepository.findByUserId(domain.getUserId())
                .orElse(new AccountJpaEntity());

        entity.setUserId(domain.getUserId());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setAvatarPublicId(domain.getAvatarPublicId());
        entity.setDeleted(domain.isDeleted());

        // Xử lý list addresses
        entity.getAddresses().clear();
        entity.getAddresses().addAll(domain.getAddresses().stream()
                .map(a -> mapToJpaAddress(a, entity))
                .collect(Collectors.toList()));

        return entity;
    }

    private AddressJpaEntity mapToJpaAddress(Address domainAddress, AccountJpaEntity accountEntity) {
        AddressJpaEntity entity = AddressJpaEntity.builder()
                .account(accountEntity)
                .recipientName(domainAddress.getRecipientName())
                .phoneNumber(domainAddress.getPhoneNumber())
                .street(domainAddress.getStreet())
                .ward(domainAddress.getWard())
                .city(domainAddress.getCity())
                .isDefault(domainAddress.isDefault())
                .build();
        
        if (domainAddress.getId() != null) {
            entity.setId(domainAddress.getId());
        }
        
        return entity;
    }
}
