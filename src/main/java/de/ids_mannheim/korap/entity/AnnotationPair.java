package de.ids_mannheim.korap.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    // english description
    private String description;
}
