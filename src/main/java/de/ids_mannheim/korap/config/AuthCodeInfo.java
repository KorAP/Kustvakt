package de.ids_mannheim.korap.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
* @author hanl
* @date 13/05/2015
*/
@Data
public class AuthCodeInfo {
    private String clientId;
    private String scopes;
    private Integer userId;
    private Boolean status;
    private String code;
    private List<String> tokens;

    public AuthCodeInfo() {
        this.setStatus(true);
        this.tokens = new ArrayList<>();
    }

    public AuthCodeInfo(String clientid, String authcode) {
        this();
        this.clientId = clientid;
        this.code = authcode;
    }
}
