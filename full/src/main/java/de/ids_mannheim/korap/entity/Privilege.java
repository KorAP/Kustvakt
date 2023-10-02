package de.ids_mannheim.korap.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
