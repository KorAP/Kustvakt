package de.ids_mannheim.korap.user;

/**
 * User: hanl
 * Date: 10/16/13
 * Time: 2:02 PM
 * 
 * @author margaretha
 * @last-update 18/04/2018
 */
public class ShibbolethUser extends User {

    /**
     * Auto generated serial Id
     */
    private static final long serialVersionUID = -4008236368010397075L;
    private String mail;
    private String affiliation;
    // EM: common name
    private String commonName;


    protected ShibbolethUser () {
        super(1);
    }


    private ShibbolethUser (String eduPersonID, String mail, String cn,
                      String affiliation) {
        this(eduPersonID);
        this.setUsername(eduPersonID);
        this.setMail(mail);
        this.setAffiliation(affiliation);
        this.setCommonName(cn);
    }


    public ShibbolethUser (String username) {
        super(username, 1);

    }


    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer("ShibbolethUser{");
        sb.append(", mail='").append(getMail()).append('\'');
        sb.append(", affiliation='").append(getAffiliation()).append('\'');
        sb.append(", common-name='").append(getCommonName()).append('\'');
        sb.append('}');
        return sb.toString();
    }


    @Override
    protected User clone () {
        return new ShibbolethUser(this.getUsername(), this.getMail(), this.getCommonName(),
                this.getAffiliation());
    }


    public String getMail () {
        return mail;
    }


    public void setMail (String mail) {
        this.mail = mail;
    }


    public String getAffiliation () {
        return affiliation;
    }


    public void setAffiliation (String affiliation) {
        this.affiliation = affiliation;
    }


    public String getCommonName () {
        return commonName;
    }


    public void setCommonName (String commonName) {
        this.commonName = commonName;
    }
}
