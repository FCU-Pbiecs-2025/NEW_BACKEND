package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "child_Info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildInfo {

    @Id
    @Column(name = "ChildID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID childID;

    @Column(name = "NationalID", length = 10)
    private String nationalID;

    @Column(name = "Name", length = 10)
    private String name;

    @Column(name = "Gender")
    private Boolean gender;

    @Column(name = "BirthDate")
    private LocalDate birthDate;

    @Column(name = "FamilyInfoID", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID familyInfoID;

    @Column(name = "HouseholdAddress", length = 200)
    private String householdAddress;
}
