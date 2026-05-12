package iuh.fit.se.modules.account.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Address — Entity phụ thuộc vào Account Aggregate.
 * Không được quản lý độc lập bằng repository riêng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {
    @Setter
    private Long id;
    private String street;
    private String ward;
    private String district;
    private String city;
    
    @Setter
    @JsonProperty("isDefault")
    private boolean isDefault;
}
