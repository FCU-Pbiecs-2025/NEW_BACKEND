package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @Column(name = "UserID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID userID;

    @Column(name = "Account", length = 50)
    private String account;

    @Column(name = "Password", length = 512)
    private String password;

    @Column(name = "AccountStatus")
    private Byte accountStatus;

    @Column(name = "PermissionType")
    private Byte permissionType;

    @Column(name = "Name", length = 10)
    private String name;

    @Column(name = "Gender")
    private Boolean gender;

    @Column(name = "PhoneNumber", length = 15)
    private String phoneNumber;

    @Column(name = "MailingAddress", length = 200)
    private String mailingAddress;


    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "BirthDate")
    private LocalDate birthDate;

    @Column(name = "FamilyInfoID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID familyInfoID;

    @Column(name = "InstitutionID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID institutionID;

    @Column(name = "NationalID", length = 20)
    private String nationalID;

    // Explicit getters/setters for static analysis and JSON mapping
    public String getNationalID() {
        return this.nationalID;
    }

    public void setNationalID(String nationalID) {
        this.nationalID = nationalID;
    }


}
