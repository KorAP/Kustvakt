package de.ids_mannheim.korap.service;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class MailAuthenticator extends Authenticator {

    private PasswordAuthentication passwordAuthentication;

    public MailAuthenticator (String username, String password) {
        passwordAuthentication = new PasswordAuthentication(username, password);

    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication () {
        return passwordAuthentication;
    }

}
