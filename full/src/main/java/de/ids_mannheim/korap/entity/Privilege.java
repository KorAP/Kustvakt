package de.ids_mannheim.korap.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.ids_mannheim.korap.constant.PrivilegeType;

/**  
 * 
 * @author margaretha
 *
 */
@Entity
@Table
public class Privilege {
    
    @Id
    @Enumerated(EnumType.STRING)
    private PrivilegeType id;
    
    @ManyToOne
    @JoinColumn
    private Role role;
    
    public String toString () {
        return "id=" + id + ", role="+ role;
    }
}
