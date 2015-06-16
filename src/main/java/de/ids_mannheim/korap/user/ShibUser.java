package de.ids_mannheim.korap.user;

import lombok.Data;

/**
 * User: hanl
 * Date: 10/16/13
 * Time: 2:02 PM
 */
@Data
public class ShibUser extends User {

    private String mail;
    private String affiliation;
    private String cn;

    protected ShibUser() {
        super(1);
    }

    private ShibUser(String eduPersonID, String mail, String cn, String affiliation) {
        this(eduPersonID);
        this.setUsername(eduPersonID);
        this.mail = mail;
        this.affiliation = affiliation;
        this.cn = cn;
    }

    public ShibUser(String username) {
        super(username, 1);

    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ShibUser{");
        sb.append(", mail='").append(mail).append('\'');
        sb.append(", affiliation='").append(affiliation).append('\'');
        sb.append(", cn='").append(cn).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    protected User clone() {
        return new ShibUser(this.getUsername(), this.getMail(), this.getCn(), this.getAffiliation());
    }
}
