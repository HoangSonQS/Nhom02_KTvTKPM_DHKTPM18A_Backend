package iuh.fit.se.modules.logistics.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Supplier create(String name, String contactPerson, String phoneNumber, String email, String address, String taxCode) {
        return Supplier.builder()
                .name(name)
                .contactPerson(contactPerson)
                .phoneNumber(phoneNumber)
                .email(email)
                .address(address)
                .taxCode(taxCode)
                .deleted(false)
                .build();
    }

    public void softDelete() {
        this.deleted = true;
    }
}
