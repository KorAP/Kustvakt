package de.ids_mannheim.korap.oauth2.constant;

public enum OAuth2Scope {

    OPENID, SEARCH, SERIALIZE_QUERY, MATCH_INFO, CREATE_VC, LIST_VC, EDIT_VC, VC_INFO, CLIENT_INFO;

    @Override
    public String toString () {
        return super.toString().toLowerCase();
    }
}
