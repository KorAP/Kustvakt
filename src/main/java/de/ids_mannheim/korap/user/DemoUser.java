package de.ids_mannheim.korap.user;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DemoUser extends User implements Serializable {
    private static final long serialVersionUID = -5015206272520970500L;
    public static final String DEMOUSER_NAME = "demo";
    public static final Integer DEMOUSER_ID = 1654234534;
    private static final long ACCOUNT_CREATED = 1377102171202L;
    public static final String PASSPHRASE = "$2a$15$rGPvLWm5JJ1iYj0V61e5guYIGmSo.rjdBkAVIU1vWS/xdybmABxRa";

    // todo: test functionality!
    protected DemoUser() {
        super(DEMOUSER_NAME, 2);
        this.setAccountCreation(ACCOUNT_CREATED);
//        this.setQueries(UserQuery.demoUserQueries());
    }

    protected User clone() {
        return new DemoUser();
    }

}
