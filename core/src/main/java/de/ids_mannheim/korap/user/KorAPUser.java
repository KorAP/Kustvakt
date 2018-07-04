package de.ids_mannheim.korap.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KorAPUser extends User {
    private static Logger jlog = LogManager.getLogger(KorAPUser.class);
    private static final long serialVersionUID = -7108308497625884584L;

    //fixme: accountlink to shibboleth account
    private String accountLink;

    private String password;
    private String URIFragment;
    private Long URIExpiration;


    protected KorAPUser (String username) {
        super(username, 0);
        this.URIFragment = "";
        this.URIExpiration = 0L;
    }


    public KorAPUser (Integer id, String username) {
        this(username);
        this.setId(id);
    }


    public KorAPUser () {
        super();
    }


    @Override
    protected User clone () {
        KorAPUser user = new KorAPUser(this.getUsername());
        user.setUsername(this.getUsername());
        user.setAccountCreation(this.getAccountCreation());
        return user;
    }


    @Override
    public int hashCode () {
        int result = super.hashCode();
        result = 31 * result + (jlog != null ? jlog.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result
                + (URIFragment != null ? URIFragment.hashCode() : 0);
        result = 31 * result
                + (URIExpiration != null ? URIExpiration.hashCode() : 0);
        return result;
    }


    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KorAPUser))
            return false;
        if (!super.equals(o))
            return false;

        KorAPUser korAPUser = (KorAPUser) o;
        if (URIExpiration != korAPUser.URIExpiration)
            return false;
        if (URIFragment != null ? !URIFragment.equals(korAPUser.URIFragment)
                : korAPUser.URIFragment != null)
            return false;
        return true;
    }
}
