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
            ensureSingleDefault(address);
        }
        addresses.add(address);
    }

    /**
     * Cập nhật địa chỉ hiện có.
     */
    public void updateAddress(Long addressId, Address updatedData) {
        Address existing = addresses.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ với ID: " + addressId));

        existing.setRecipientName(updatedData.getRecipientName());
        existing.setPhoneNumber(updatedData.getPhoneNumber());
        existing.setStreet(updatedData.getStreet());
        existing.setWard(updatedData.getWard());
        existing.setCity(updatedData.getCity());
        
        if (updatedData.isDefault()) {
            ensureSingleDefault(existing);
        } else {
            existing.setDefault(false);
        }
    }

    /**
     * Xóa địa chỉ.
     */
    public void removeAddress(Long addressId) {
        addresses.removeIf(a -> a.getId().equals(addressId));
    }

    private void ensureSingleDefault(Address defaultAddress) {
        addresses.forEach(a -> {
            if (!a.equals(defaultAddress)) {
                a.setDefault(false);
            }
        });
        defaultAddress.setDefault(true);
    }
}
