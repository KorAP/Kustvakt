package de.ids_mannheim.korap.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.ids_mannheim.korap.constant.PrivilegeType;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes privilege table listing users and their roles.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
@Entity
@Table(name = "privilege")
public class Privilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    private PrivilegeType name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    private Role role;

    public Privilege () {}

    public Privilege (PrivilegeType name, Role role) {
        this.name = name;
        this.role = role;
    }

    public String toString () {
        return "id=" + id + ", name=" + name + ", role=" + role;
    }
}
