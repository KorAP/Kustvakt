package de.ids_mannheim.korap.core.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes the annotation key mapping to annotation_key table in the
 * database and annotation key relations to {@link AnnotationLayer}
 * and {@link Annotation}.
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
@Entity
@Table(name = "annotation_key", uniqueConstraints = @UniqueConstraint(columnNames = {
        "layer_id", "key_id" }))
public class AnnotationKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "key_id")
    private int keyId;
    @Column(name = "layer_id")
    private int layerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "layer_id", insertable = false, updatable = false)
    private AnnotationLayer layer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "key_id", insertable = false, updatable = false)
    private Annotation key;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "annotation_value", joinColumns = @JoinColumn(name = "key_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "value_id", referencedColumnName = "id"), uniqueConstraints = @UniqueConstraint(columnNames = {
            "key_id", "value_id" }))
    private Set<Annotation> values;

    public AnnotationKey () {}

    public AnnotationKey (int layerId, int keyId) {
        this.layerId = layerId;
        this.keyId = keyId;
    }

}
