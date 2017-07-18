package de.ids_mannheim.korap.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "annotation")
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String symbol;
    private String type;
    private String description;

    @Override
    public String toString () {
        return "id=" + id + ", symbol= " + symbol + ", type= " + type
                + ", description=" + description;
    }
}
