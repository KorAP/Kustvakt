package de.ids_mannheim.korap.entity;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "annotation_key", uniqueConstraints = @UniqueConstraint(
        columnNames = { "layer_id", "key_id" }))
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
    @JoinTable(name = "annotation_value",
            joinColumns = @JoinColumn(name = "key_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "value_id",
                    referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "key_id", "value_id" }))
    private Set<Annotation> values;

    public AnnotationKey () {}

    public AnnotationKey (int layerId, int keyId) {
        this.layerId = layerId;
        this.keyId = keyId;
    }

}
