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
@Table(name = "resource")
public class Resource {

    @Id
    private String id;

    // german title
    private String title;

    @Column(name = "en_title")
    private String englishTitle;
    private String description;


    @Override
    public String toString () {
        return "id=" + id + ", title=" + title + ", english title="
                + englishTitle + ", description="+description;
    }

}
