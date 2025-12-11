package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Table(name = "rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // JPA 實體必須有主鍵，資料庫端可以不理會這個欄位

    @Column(name = "AdmissionEligibility", columnDefinition = "NVARCHAR(MAX)")
    private String admissionEligibility;

    @Column(name = "ServiceContentAndTime", columnDefinition = "NVARCHAR(MAX)")
    private String serviceContentAndTime;

    @Column(name = "FeeAndRefundPolicy", columnDefinition = "NVARCHAR(MAX)")
    private String feeAndRefundPolicy;
}
