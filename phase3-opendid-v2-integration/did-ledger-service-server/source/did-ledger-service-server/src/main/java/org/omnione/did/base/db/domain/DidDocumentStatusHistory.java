package org.omnione.did.base.db.domain;

import jakarta.persistence.*;
import lombok.*;
import org.omnione.did.base.constants.DidDocStatus;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "did_document_status_history")
public class DidDocumentStatusHistory extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", nullable = false, columnDefinition = "SMALLINT")
    private Short version;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private DidDocStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private DidDocStatus toStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at")
    private Instant changedAt;

    @Column(name = "did_id", nullable = false)
    private Long didId;
}
