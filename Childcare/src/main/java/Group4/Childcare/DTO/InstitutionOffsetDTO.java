package Group4.Childcare.DTO;

import Group4.Childcare.Model.Institutions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionOffsetDTO {
  private int offset;
  private int size;
  private int totalPages;
  private boolean hasNext;
  private List<Institutions> content;
  private long totalElements;
}
