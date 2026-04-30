package org.omnione.did.base.db.domain;

import jakarta.persistence.*;
import lombok.*;
import org.omnione.did.data.model.enums.vc.VcStatus;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "vc_status_history")
public class VcStatusHistory extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vc_id", nullable = false, length = 100)
    private String vcId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "from_status")
    private VcStatus fromStatus;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private VcStatus toStatus;

    @Column(name = "changed_at")
    private Instant changedAt;
}
