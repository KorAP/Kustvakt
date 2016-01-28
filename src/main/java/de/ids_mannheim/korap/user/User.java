package de.ids_mannheim.korap.user;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.ParamFields;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.utils.KustvaktMap;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public abstract class User implements Serializable {

    public static final int ADMINISTRATOR_ID = 34349733;
    public static final String ADMINISTRATOR_NAME = "admin";

    private Integer id;
    // in local its username, in shib it's edupersonPrincipalName
    private String username;
    private Long accountCreation;
    private boolean isAccountLocked;
    private int type;
    private ParamFields fields;
    private UserSettings settings;
    //todo: remove!
    private UserDetails details;
    private List<UserQuery> queries;

    private List<Userdata> userdata;

    protected User() {
        this.fields = new ParamFields();
        this.accountCreation = TimeUtils.getNow().getMillis();
        this.isAccountLocked = false;
        this.username = "";
        this.id = -1;
    }

    protected User(int type) {
        this();
        this.type = type;
    }

    protected User(String username, int type) {
        this(type);
        this.username = username;
    }

    public void addField(ParamFields.Param param) {
        this.fields.add(param);
    }

    public <T extends ParamFields.Param> T getField(Class<T> cl) {
        return this.fields.get(cl);
    }

    //todo: repair transfer
    public void transfer(User user) {
        //        this.setSettings(user.getSettings());
        //        this.setDetails(user.getDetails());
        //        this.setQueries(user.getQueries());
        if (this instanceof KorAPUser) {

            //            this.getSettings().setUserID(this.id);
            //            this.getDetails().setUserID(this.id);
            //            for (UserQuery q : this.getQueries())
            //                q.setOwner(this.accountID);
        }
    }

    public void addUserData(Userdata data) {
        if (data != null) {
            for (Userdata d : this.userdata) {
                // already has an object of that type!
                if (d.getClass().equals(data.getClass()))
                    return;
            }
            userdata.add(data);
        }
    }

    @Deprecated
    public void setDetails(UserDetails details) {
        if (details != null)
            details.setUserID(this.id);
        //        this.details = details;
    }

    @Deprecated
    public void setSettings(UserSettings settings) {
        if (settings != null)
            settings.setUserID(this.id);
        //        this.settings = settings;
    }

    public void setId(Integer id) {
        this.id = id;
        //        if (this.settings != null)
        //            this.settings.setUserID(this.id);
        //        if (this.details != null)
        //            this.details.setUserID(this.id);
    }

    public Map<String, Object> toMap() {
        Map map = new HashMap();
        map.put(Attributes.USERNAME, this.username);
        //TimeUtils.format(new DateTime(this.accountCreation))
        map.put(Attributes.ACCOUNT_CREATION, this.accountCreation);

        //        if (this.getDetails() != null)
        //            map.putAll(this.getDetails().toMap());
        return map;
    }

    public Map toCache() {
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User user = (User) o;
        if (!username.equals(user.username))
            return false;
        return true;
    }

    public boolean isDemo() {
        return this.getUsername().equalsIgnoreCase(DemoUser.DEMOUSER_NAME);
    }

    public boolean isAdmin() {
        return this.getUsername().equals(ADMINISTRATOR_ID);
    }

    protected abstract User clone();

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("id='").append(id).append('\'');
        sb.append(", username='").append(username).append('\'');
        return sb.toString();
    }

    public static class UserFactory {

        public static KorAPUser getUser(String username) {
            return new KorAPUser(username);
        }

        public static KorAPUser getUser(String username, String password) {
            KorAPUser user = new KorAPUser(username);
            user.setPassword(password);
            return user;
        }

        public static KorAPUser getAdmin() {
            return new KorAPUser(ADMINISTRATOR_ID, ADMINISTRATOR_NAME);
        }

        public static DemoUser getDemoUser() {
            return new DemoUser();
        }

        public static DemoUser getDemoUser(Integer id) {
            DemoUser demo = new DemoUser();
            demo.setId(id);
            return demo;
        }

        public static ShibUser getShibInstance(String eduPersonID, String mail,
                String cn) {
            ShibUser u = new ShibUser(eduPersonID);
            u.setAffiliation("");
            u.setMail(mail);
            u.setUsername(eduPersonID);
            u.setCn(cn);
            return u;
        }

        public static User toUser(Map<String, Object> map) {
            KustvaktMap kmap = new KustvaktMap(map);
            int type = map.get(Attributes.TYPE) == null ?
                    0 :
                    (Integer) kmap.get(Attributes.TYPE, Integer.class);
            User user;
            long created = -1;
            if (map.get(Attributes.ACCOUNT_CREATION) != null)
                created = DateTime.parse(kmap.get(Attributes.ACCOUNT_CREATION))
                        .getMillis();
            switch (type) {
                case 0:
                    user = UserFactory.getUser(kmap.get(Attributes.USERNAME));
                    user.setId(
                            (Integer) kmap.get(Attributes.ID, Integer.class));
                    user.setAccountLocked(
                            map.get(Attributes.ACCOUNTLOCK) == null ?
                                    false :
                                    (Boolean) kmap.get(Attributes.ACCOUNTLOCK,
                                            Boolean.class));
                    user.setAccountCreation(created);
                    break;
                default:
                    user = UserFactory.getDemoUser(
                            (Integer) kmap.get(Attributes.ID, Integer.class));
                    user.setAccountCreation(created);
            }
            return user;
        }

        public static KorAPUser toUser(String value) {
            JsonNode node = JsonUtils.readTree(value);
            KorAPUser user = UserFactory
                    .getUser(node.path(Attributes.USERNAME).asText());
            user.setAccountLocked(
                    node.path(Attributes.ACCOUNTLOCK).asBoolean());
            user.setAccountLink(node.path(Attributes.ACCOUNTLINK).asText());
            user.setAccountCreation(
                    node.path(Attributes.ACCOUNT_CREATION).asLong());
            user.setPassword(node.path(Attributes.PASSWORD).asText());
            return user;
        }
    }

}
