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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "annotation_pair", 
    uniqueConstraints=@UniqueConstraint(columnNames={"annotation1","annotation2"}))
public class AnnotationPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "annotation1")
    private int annotationId1;
    @Column(name = "annotation2")
    private int annotationId2;
    @Column(name = "description")
    private String description;

    @Fetch(FetchMode.SELECT)
    @ManyToOne //(fetch=FetchType.LAZY) 
    @JoinColumn(name = "annotation1", insertable = false, updatable = false)
    private Annotation annotation1;
    
    @Fetch(FetchMode.SELECT)
    @ManyToOne //(fetch=FetchType.LAZY) 
    @JoinColumn(name = "annotation2", insertable = false, updatable = false)
    private Annotation annotation2;

    @ManyToMany(fetch=FetchType.LAZY) //(fetch=FetchType.EAGER)
    @JoinTable(
            name="annotation_pair_value",
            joinColumns=@JoinColumn(name="pair_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="value_id", referencedColumnName="id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {
                            "pair_id", "value_id" })
    )
    private Set<Annotation> values;

    @Override
    public String toString () {
        return "id=" + id + ", annotation1=" + annotationId1 + ", annotation2="
                + annotationId1 + ", description=" + description
                + ", annotation1= "+ annotation1
                + ", annotation2= "+ annotation2;
                
    }
}
