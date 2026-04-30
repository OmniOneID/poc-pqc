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
@Table(name = "zkp_credential_schema")
public class ZkpCredentialSchema extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_id", nullable = false, length = 100)
    private String schemaId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "attr_names", nullable = false)
    private String attrNames;

    @Column(name = "attr_types", nullable = false)
    private String attrTypes;

    @Column(name = "tag", nullable = false, length = 100)
    private String tag;

    @Column(name = "schema", nullable = false)
    private String schema;
}
