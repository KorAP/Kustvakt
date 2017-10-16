package de.ids_mannheim.korap.entity;

import javax.persistence.Column;
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
@Table(name = "virtual_corpus")
public class VirtualCorpus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String type;
    private String status;
    private String description;
    @Column(name = "required_access")
    private String requiredAccess;
    @Column(name = "collection_query")
    private String collectionQuery;
    private String definition;
    @Column(name = "owner_id")
    private String ownerId;


    @Override
    public String toString () {
        return "id=" + id + ", name= " + name + ", type= " + type + ", status= "
                + status + ", description=" + description + ", requiredAccess="
                + requiredAccess + ", collectionQuery= " + collectionQuery
                + ", definition= " + definition + ", ownerId= " + ownerId;
    }
}
