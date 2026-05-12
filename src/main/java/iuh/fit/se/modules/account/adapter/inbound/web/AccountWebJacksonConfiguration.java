package iuh.fit.se.modules.account.adapter.inbound.web;

import iuh.fit.se.modules.account.domain.Address;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountWebJacksonConfiguration {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer accountAddressJsonMixinCustomizer() {
        return builder -> builder.postConfigurer(
                om -> om.addMixIn(Address.class, AddressDomainJsonMixin.class));
    }
}
