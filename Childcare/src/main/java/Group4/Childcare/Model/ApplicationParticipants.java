package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationParticipants {

    @Id
    @Column(name = "ParticipantID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID participantID;

    @Column(name = "ApplicationID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID applicationID;

    @Column(name = "ParticipantType")
    private Boolean participantType;

    @Column(name = "NationalID", length = 20)
    private String nationalID;

    @Column(name = "Name", length = 50)
    private String name;

    @Column(name = "Gender")
    private Boolean gender;

    @Column(name = "RelationShip", length = 15)
    private String relationShip;

    @Column(name = "Occupation", length = 15)
    private String occupation;

    @Column(name = "PhoneNumber", length = 15)
    private String phoneNumber;

    @Column(name = "HouseholdAddress", length = 200)
    private String householdAddress;

    @Column(name = "MailingAddress", length = 200)
    private String mailingAddress;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "BirthDate")
    private LocalDate birthDate;

    @Column(name = "IsSuspended")
    private Boolean isSuspended;

    @Column(name = "SuspendEnd")
    private LocalDate suspendEnd;

    @Column(name = "CurrentOrder")
    private Integer currentOrder;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "Reason", length = 100)
    private String reason;

    @Column(name = "ClassID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID classID;


    @Column(name = "ReviewDate")
    private LocalDateTime reviewDate;

    @ManyToOne
    @JoinColumn(name = "ApplicationID", insertable = false, updatable = false)
    private Applications applications;
}
