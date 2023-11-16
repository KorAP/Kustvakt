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
 * Describes admin users
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "user_id")
    private String userId;

    @Override
    public String toString () {
        return "id=" + id + ", userId=" + userId;
    }
}
