package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Applications {

    @Id
    @Column(name = "ApplicationID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID applicationID;

    @Column(name = "ApplicationDate")
    private LocalDate applicationDate;

    @Column(name = "CaseNumber")
    private Long caseNumber;

    @Column(name = "InstitutionID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID institutionID;

    @Column(name = "UserID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID userID;

    @Column(name = "IdentityType")
    private Byte identityType;

    @Column(name = "AttachmentPath", columnDefinition = "NVARCHAR(MAX)")
    private String attachmentPath;

    @Column(name = "AttachmentPath1", columnDefinition = "NVARCHAR(MAX)")
    private String attachmentPath1;

    @Column(name = "AttachmentPath2", columnDefinition = "NVARCHAR(MAX)")
    private String attachmentPath2;

    @Column(name = "AttachmentPath3", columnDefinition = "NVARCHAR(MAX)")
    private String attachmentPath3;

    @OneToMany(mappedBy = "applications")
    private List<ApplicationParticipants> applicationParticipants;
}
