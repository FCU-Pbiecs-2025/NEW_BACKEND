package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSummaryDTO {
    private UUID classID;
    private String className;
    private Integer capacity;
    private String minAgeDescription;
    private String maxAgeDescription;
    private String institutionName;
    private UUID institutionID;
    private Integer currentStudents; // added to hold current students count
}
