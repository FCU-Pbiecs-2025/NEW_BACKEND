package Group4.Childcare.DTO;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.Model.Classes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for returning institution data with its associated classes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionWithClassesDTO {
    private Institutions institution;
    private List<Classes> classes;
}
