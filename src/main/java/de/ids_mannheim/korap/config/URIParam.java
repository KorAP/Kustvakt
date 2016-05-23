package de.ids_mannheim.korap.config;

import lombok.Getter;

/**
 * @author hanl
 * @date 15/07/15
 */
@Getter
public class URIParam extends ParamFields.Param {

    private final String uriFragment;
    private final Long uriExpiration;


    public URIParam (String uri, Long expire) {
        this.uriFragment = uri;
        this.uriExpiration = expire;
    }


    @Override
    public boolean hasValues () {
        return this.uriFragment != null && !this.uriFragment.isEmpty()
                && this.uriExpiration != null;
    }

}
