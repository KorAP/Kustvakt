package de.ids_mannheim.korap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes annotation tags available in the system / used in
 * annotating corpus data.
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
@Entity
@Table(name = "annotation")
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String code;
    private String type;
    private String text;
    private String description;
    @Column(name = "de_description")
    private String germanDescription;

    public Annotation () {}

    public Annotation (String code, String type, String text,
                       String description) {
        this.code = code;
        this.type = type;
        this.text = text;
        this.description = description;
    }

    @Override
    public String toString () {
        return "id=" + id + ", code= " + code + ", type= " + type
                + ", description=" + description + ", germanDescription="
                + germanDescription;
    }
}
