package de.ids_mannheim.korap.user;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DemoUser extends User implements Serializable {
    private static final long serialVersionUID = -5015206272520970500L;
    public static final String DEMOUSER_NAME = "guest";
    public static final Integer DEMOUSER_ID = 1654234534;
    private static final long ACCOUNT_CREATED = 1377102171202L;
    public static final String PASSPHRASE = "demo";


    protected DemoUser () {
        super(DEMOUSER_NAME, 2);
        this.setId(-1);
        this.setAccountCreation(ACCOUNT_CREATED);
    }


    protected User clone () {
        return new DemoUser();
    }

}
