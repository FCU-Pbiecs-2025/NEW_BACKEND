package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cancellation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cancellation {

    @Id
    @Column(name = "CancellationID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID cancellationID;

    @Column(name = "ApplicationID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID applicationID;

    @Column(name = "AbandonReason", length = 50)
    private String abandonReason;

    @Column(name = "CancellationDate")
    private LocalDate cancellationDate;

    @Column(name = "ConfirmDate")
    private LocalDate confirmDate;

    @Column(name = "NationalID", length = 20)
    private String nationalID;
}
