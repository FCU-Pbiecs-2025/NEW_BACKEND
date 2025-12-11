package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Classes {

    @Id
    @Column(name = "ClassID", columnDefinition = "UNIQUEIDENTIFIER")
    @JsonProperty("classID")
    @JsonAlias({"classId"})
    private UUID classID;

    @Column(name = "ClassName", length = 50)
    private String className;

    @Column(name = "Capacity")
    private Integer capacity;

    @Column(name = "CurrentStudents")
    private Integer currentStudents;

    @Column(name = "MinAgeDescription")
    private Integer minAgeDescription;

    @Column(name = "MaxAgeDescription")
    private Integer maxAgeDescription;

    @Column(name = "AdditionalInfo", length = 100)
    private String additionalInfo;

    @Column(name = "InstitutionID", columnDefinition = "UNIQUEIDENTIFIER")
    @JsonProperty("institutionID")
    @JsonAlias({"institutionId"})
    private UUID institutionID;
}
