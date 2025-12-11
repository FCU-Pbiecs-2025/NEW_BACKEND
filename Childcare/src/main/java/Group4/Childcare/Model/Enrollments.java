package Group4.Childcare.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollments {

    @Id
    @Column(name = "EnrollmentId", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID enrollmentId;

    @Column(name = "ChildId", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID childId;

    @Column(name = "ClassId", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID classId;
}
