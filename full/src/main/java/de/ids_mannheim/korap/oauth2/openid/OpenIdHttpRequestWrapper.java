package de.ids_mannheim.korap.oauth2.openid;

import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;

/** A request wrapper based on HTTPRequest.
 * 
 * @author margaretha
 *
 */
public class OpenIdHttpRequestWrapper extends HTTPRequest {

    private Map<String, String> params;

    public OpenIdHttpRequestWrapper (Method method, URL url) {
        super(method, url);
    }

    @Override
    public Map<String, String> getQueryParameters () {
        return this.params;
    }

    public void toHttpRequest (HttpServletRequest servletRequest,
            Map<String, String> map) throws ParseException {

        this.params = map;
        this.setClientIPAddress(servletRequest.getRemoteAddr());
        this.setContentType(servletRequest.getContentType());

        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement().toString();
            this.setHeader(name, servletRequest.getHeader(name));
        }
    }
}
