package de.ids_mannheim.korap.entity;

import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes annotation layers as a pair of foundry and layer where
 * foundry denotes where the annotation comes from e.g. Tree tagger
 * parser, and layer denotes the annotation layer e.g. part of speech.
 * 
 * @author margaretha
 * @see Annotation
 */
@Setter
@Getter
@Entity
@Table(name = "annotation_layer", uniqueConstraints = @UniqueConstraint(columnNames = {
        "foundry_id", "layer_id" }))
public class AnnotationLayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "foundry_id")
    private int foundryId;
    @Column(name = "layer_id")
    private int layerId;
    @Column(name = "description")
    private String description;

    @Fetch(FetchMode.SELECT)
    @ManyToOne // (fetch=FetchType.LAZY)
    @JoinColumn(name = "foundry_id", insertable = false, updatable = false)
    private Annotation foundry;

    @Fetch(FetchMode.SELECT)
    @ManyToOne // (fetch=FetchType.LAZY)
    @JoinColumn(name = "layer_id", insertable = false, updatable = false)
    private Annotation layer;

    @OneToMany(mappedBy = "layer", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    private Set<AnnotationKey> keys;

    @Override
    public String toString () {
        return "id=" + id + ", foundry=" + foundry + ", layer=" + layer
                + ", description=" + description + ", keys= " + keys;

    }
}
