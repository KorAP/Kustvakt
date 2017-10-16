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
@Table(name = "vc_access_group")
public class VirtualCorpusAccessGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "vc_id")
    private int virtualCorpusId;
    @Column(name = "group_id")
    private int groupId;


    @Override
    public String toString () {
        return "id=" + id + ", virtualCorpusId= " + virtualCorpusId + ", groupId= "
                + groupId;
    }
}
