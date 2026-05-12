package iuh.fit.se.modules.account.adapter.inbound.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * JSON body cho thêm/sửa địa chỉ — Jackson chỉ tồn tại ở inbound adapter.
 */
public record AddressRequestBody(
        @NotBlank(message = "Địa chỉ không được để trống") String street,
        @NotBlank(message = "Phường/Xã không được để trống") String ward,
        @NotBlank(message = "Quận/Huyện không được để trống") String district,
        @NotBlank(message = "Tỉnh/Thành phố không được để trống") String city,
        @JsonProperty("isDefault") @NotNull(message = "Trạng thái mặc định không được để trống") Boolean isDefault) {

    public AccountUseCase.AddressCommand toCommand() {
        return new AccountUseCase.AddressCommand(
                street, ward, district, city, Boolean.TRUE.equals(isDefault));
    }
}
