package iuh.fit.se.modules.account.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Account — Aggregate Root của module Account.
 * Quản lý Profile và danh sách địa chỉ.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Account {
    @Setter
    private Long id;
    private Long userId; // Soft reference tới User trong module Auth
    private String phoneNumber;
    private String avatarUrl;
    private String avatarPublicId;
    private boolean isDeleted;

    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    /**
     * Factory method để tạo profile mặc định.
     */
    public static Account createDefault(Long userId) {
        return Account.builder()
                .userId(userId)
                .isDeleted(false)
                .addresses(new ArrayList<>())
                .build();
    }

    /**
     * Cập nhật thông tin profile cơ bản.
     */
    public void updateProfile(String phoneNumber, String avatarUrl, String avatarPublicId) {
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.avatarPublicId = avatarPublicId;
    }

    /**
     * Thêm địa chỉ mới vào Account.
     * Logic: Nếu là địa chỉ mặc định đầu tiên, tự động set isDefault = true.
     */
    public void addAddress(Address address) {
        if (addresses.isEmpty()) {
            address.setDefault(true);
        } else if (address.isDefault()) {
            // Đảm bảo chỉ có một địa chỉ mặc định
            addresses.forEach(a -> a.setDefault(false));
            address.setDefault(true);
        }
        addresses.add(address);
    }
}
