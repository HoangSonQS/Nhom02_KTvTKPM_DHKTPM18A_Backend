package iuh.fit.se.modules.account.adapter.inbound.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * JSON body for adding/updating an address.
 */
public record AddressRequestBody(
        @NotBlank(message = "Ten nguoi nhan khong duoc de trong") String recipientName,
        @NotBlank(message = "So dien thoai khong duoc de trong") String phoneNumber,
        @NotBlank(message = "Dia chi khong duoc de trong") String street,
        @NotBlank(message = "Phuong/Xa khong duoc de trong") String ward,
        @NotBlank(message = "Tinh/Thanh pho khong duoc de trong") String city,
        @JsonProperty("isDefault") @NotNull(message = "Trang thai mac dinh khong duoc de trong") Boolean isDefault) {

    public AccountUseCase.AddressCommand toCommand() {
        return new AccountUseCase.AddressCommand(
                recipientName, phoneNumber, street, ward, city, Boolean.TRUE.equals(isDefault));
    }
}
