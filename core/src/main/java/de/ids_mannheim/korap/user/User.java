package de.ids_mannheim.korap.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.ParamFields;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.utils.KustvaktMap;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public abstract class User implements Serializable {

    //EM: add
    private String email;
    //EM: finish
    
    private Integer id;
    // in local its username, in shib it's edupersonPrincipalName
    private String username;
    private Long accountCreation;
    private boolean isAccountLocked;
    private int type;
    private ParamFields fields;
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private UserSettings settings;
    //todo: remove!
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private UserDetails details;
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private List<UserQuery> queries;

    private List<Userdata> userdata;

//    private boolean isSystemAdmin;

    // Values for corpusAccess:
    public enum CorpusAccess	 {
    	FREE, 	// Access to licence free corpora only, without login   
        PUB,	// Access to public (= öffentliche Korpora) only, externes Login.
        ALL 	// Access to all corpora, internes Login.
    	};
    	
    @Getter
    @Setter
    private CorpusAccess corpusAccess = CorpusAccess.FREE;
        
    // values for location (set using the X-forwarded-for Header):
    public enum Location  {
        INTERN, 	// KorAP accessed by internal Client (inside intranet).
        EXTERN		// KorAP accessed by external Client (outside intranet).
    };
        
    @Getter
    @Setter
    private Location location = Location.EXTERN;

    
    protected User () {
        this.fields = new ParamFields();
        this.accountCreation = TimeUtils.getNow().getMillis();
        this.isAccountLocked = false;
        this.username = "";
        this.id = -1;
        this.userdata = new ArrayList<>();
        this.location 		= Location.EXTERN;
        this.corpusAccess 	= CorpusAccess.FREE;
    }


    protected User (int type) {
        this();
        this.type = type;
    }


    protected User (String username, int type) {
        this(type);
        this.username = username;
    }


    public void addField (ParamFields.Param param) {
        this.fields.add(param);
    }


    public <T extends ParamFields.Param> T getField (Class<T> cl) {
        return this.fields.get(cl);
    }


    public void addUserData (Userdata data) {
        if (data != null) {
            for (Userdata d : this.userdata) {
                // already has an object of that type!
                if (d.getClass().equals(data.getClass()))
                    return;
            }
            userdata.add(data);
        }
    }


    public void setId (Integer id) {
        this.id = id;
        //        if (this.settings != null)
        //            this.settings.setUserID(this.id);
        //        if (this.details != null)
        //            this.details.setUserID(this.id);
    }


    public Map<String, Object> toMap () {
        Map map = new HashMap();
        map.put(Attributes.USERNAME, this.username);
        //TimeUtils.format(new DateTime(this.accountCreation))
        map.put(Attributes.ACCOUNT_CREATION, this.accountCreation);

        //        if (this.getDetails() != null)
        //            map.putAll(this.getDetails().toMap());
        return map;
    }


    public Map toCache () {
        Map map = new HashMap();
        map.put(Attributes.ID, this.id);
        map.put(Attributes.USERNAME, this.username);
        map.put(Attributes.ACCOUNT_CREATION,
                TimeUtils.format(new DateTime(this.accountCreation)));
        map.put(Attributes.ACCOUNTLOCK, this.isAccountLocked);
        map.put(Attributes.TYPE, this.type);
        return map;
    }


    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User user = (User) o;
        if (!username.equals(user.username))
            return false;
        return true;
    }

//    public boolean isAdmin () {
//        return this.getUsername().equals(ADMINISTRATOR_ID);
//    }


    protected abstract User clone ();


    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer();
        sb.append("id='").append(id).append('\'');
        sb.append(", username='").append(username).append('\'');
        return sb.toString();
    }

    public String locationtoString()
    
    {
    	if( this.location == Location.INTERN)
    		return "INTERN";
    	else if( this.location == Location.EXTERN )
    		return "EXTERN";
    	else
    		return "???";
    }
    
    public String accesstoString()
    
    {
    	if( this.corpusAccess == CorpusAccess.ALL )
    		return "ALL";
    	else if( this.corpusAccess == CorpusAccess.PUB )
    		return "PUB";
    	else if( this.corpusAccess == CorpusAccess.FREE )
    		return "FREE";
    	else
    		return "???";
    }
    
    public static class UserFactory {

        public static KorAPUser getUser (String username) {
            return new KorAPUser(username);
        }


        public static KorAPUser getUser (String username, String password) {
            KorAPUser user = new KorAPUser(username);
            user.setPassword(password);
            return user;
        }

//        public static KorAPUser getAdmin () {
//            return new KorAPUser(ADMINISTRATOR_ID, ADMINISTRATOR_NAME);
//        }


        public static DemoUser getDemoUser () {
            return new DemoUser();
        }


        public static DemoUser getDemoUser (Integer id) {
            DemoUser demo = new DemoUser();
            demo.setId(id);
            return demo;
        }


        public static boolean isDemo (String username) {
            return new DemoUser().getUsername().equalsIgnoreCase(username);
        }


//        public static ShibUser getShibInstance (String eduPersonID,
//                String mail, String cn) {
//            ShibUser u = new ShibUser(eduPersonID);
//            u.setAffiliation("");
//            u.setMail(mail);
//            u.setUsername(eduPersonID);
//            u.setCn(cn);
//            return u;
//        }


        public static KorAPUser toKorAPUser (Map<String, Object> map) {
            KorAPUser user = UserFactory.getUser((String) map
                    .get(Attributes.USERNAME));
            user.setPassword((String) map.get(Attributes.PASSWORD));
            int id = map.get(Attributes.ID) == null ? -1 : (int) map
                    .get(Attributes.ID);
            if (id != -1)
                user.setId(id);
            long cr = map.get(Attributes.ACCOUNT_CREATION) == null ? -1
                    : (long) map.get(Attributes.ACCOUNT_CREATION);
            if (cr != -1)
                user.setAccountCreation((Long) map
                        .get(Attributes.ACCOUNT_CREATION));
            return user;
        }


        public static User toUser (Map<String, Object> map) {
            KustvaktMap kmap = new KustvaktMap(map);
            int type = map.get(Attributes.TYPE) == null ? 0 : (Integer) kmap
                    .get(Attributes.TYPE, Integer.class);
            User user;
            long created = -1;
            int id = kmap.get(Attributes.ID, Integer.class) == null ? -1
                    : (Integer) kmap.get(Attributes.ID, Integer.class);

            if (map.get(Attributes.ACCOUNT_CREATION) != null)
                created = TimeUtils.getTime(kmap.get(Attributes.ACCOUNT_CREATION))
                        .getMillis();
            switch (type) {
                case 0:
                    user = UserFactory.getUser(kmap.get(Attributes.USERNAME));
                    if (id != -1)
                        user.setId((Integer) kmap.get(Attributes.ID,
                                Integer.class));
                    user.setAccountLocked(map.get(Attributes.ACCOUNTLOCK) == null ? false
                            : (Boolean) kmap.get(Attributes.ACCOUNTLOCK,
                                    Boolean.class));
                    user.setAccountCreation(created);
                    break;
                default:
                    user = UserFactory.getDemoUser();
                    user.setAccountCreation(created);
            }
            return user;
        }


        public static KorAPUser toUser (String value) throws KustvaktException {
            JsonNode node = JsonUtils.readTree(value);
            KorAPUser user = UserFactory.getUser(node.path(Attributes.USERNAME)
                    .asText());
            user.setAccountLocked(node.path(Attributes.ACCOUNTLOCK).asBoolean());
            user.setAccountLink(node.path(Attributes.ACCOUNTLINK).asText());
            user.setAccountCreation(node.path(Attributes.ACCOUNT_CREATION)
                    .asLong());
            user.setPassword(node.path(Attributes.PASSWORD).asText());
            return user;
        }
    }

}
