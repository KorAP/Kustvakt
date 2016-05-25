package de.ids_mannheim.korap.config;

import lombok.Data;

/**
 * @author hanl
 * @date 22/01/2014
 */
@Data
public class UserClientInfo {


    private String OSName;
    private String userAgent;
    private String clientIP;


    public UserClientInfo () {
        this.clientIP = "";
        this.userAgent = "";
        this.OSName = "";
    }


    public UserClientInfo (String os, String ua, String ip) {
        this.OSName = os;
        this.userAgent = ua;
        this.clientIP = ip;
    }
}
