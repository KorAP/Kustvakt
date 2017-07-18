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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "annotation_pair")
public class AnnotationPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "annotation1")
    private int annotationId1;
    @Column(name = "annotation2")
    private int annotationId2;
    @Column(name = "de_description")
    private String germanDescription;
    @Column(name = "description")
    private String englishDescription;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "annotation1", insertable = false, updatable = false)
    private Annotation annotation1;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "annotation2", insertable = false, updatable = false)
    private Annotation annotation2;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="annotation_pair_value",
            joinColumns=@JoinColumn(name="pair_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="value_id", referencedColumnName="id")
    )
    private Set<Annotation> values;

    @Override
    public String toString () {
        return "id=" + id + ", annotation1=" + annotationId1 + ", annotation2="
                + annotationId1 + ", description=" + englishDescription
                + ", germanDescription= " + germanDescription 
                + "annotation1= "+ annotation1
                + "annotation2= "+ annotation2;
                
    }

}
