package iuh.fit.se.modules.account.adapter.inbound.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mixin cho {@link iuh.fit.se.modules.account.domain.Address} — giữ key JSON {@code isDefault}
 * mà không đưa annotation vào domain.
 */
public abstract class AddressDomainJsonMixin {

    @JsonProperty("isDefault")
    abstract boolean isDefault();
}
