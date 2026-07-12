package project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "traceability_matrices")
public class TraceabilityMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "matrix_html", columnDefinition = "TEXT", nullable = false)
    private String matrixHtml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // constructors, getters, setters
}
