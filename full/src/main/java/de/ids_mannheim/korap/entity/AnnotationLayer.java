package de.ids_mannheim.korap.entity;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.Getter;
import lombok.Setter;

/**
 * Describes annotations as a pair, e.g. foundry and layer where
 * foundry denotes where the annotation comes from e.g. Tree tagger
 * parser, and layer denotes the annotation layer e.g. part of speech.
 * 
 * @author margaretha
 * @see Annotation
 */
@Setter
@Getter
@Entity
@Table(name = "annotation_layer", uniqueConstraints = @UniqueConstraint(
        columnNames = { "foundry_id", "layer_id" }))
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

    @OneToMany(mappedBy = "layer", fetch = FetchType.EAGER,
            cascade = CascadeType.REMOVE)
    private Set<AnnotationKey> keys;

    @Override
    public String toString () {
        return "id=" + id + ", foundry=" + foundry + ", layer=" + layer
                + ", description=" + description + ", keys= " + keys;

    }
}
