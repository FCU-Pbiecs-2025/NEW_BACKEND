package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(name = "parent_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentInfo {
    @Id
    @Column(name = "ParentID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID parentID;

    @Column(name = "NationalID", length = 20)
    private String nationalID;

    @Column(name = "Name", length = 50)
    private String name;

    @Column(name = "Gender")
    private Boolean gender;

    @Column(name = "Relationship", length = 15)
    private String relationship;

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

    @Column(name = "FamilyInfoID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID familyInfoID;
}
