package de.ids_mannheim.korap.authentication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.springframework.beans.factory.annotation.Autowired;

// import com.novell.ldap.*; search() funktioniert nicht korrekt, ausgewechselt gegen unboundID's Bibliothek 20.04.17/FB
//Using JAR from unboundID:
import com.unboundid.ldap.sdk.LDAPException;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.WrappedException;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.EntityHandlerIface;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.DemoUser;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.ShibbolethUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.user.User.Location;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.validator.Validator;

/**
 * contains the logic to authentication and registration processes. Uses
 * interface implementations (AuthenticationIface) for different databases and
 * handlers
 * 
 * @author hanl
 */
public class KustvaktAuthenticationManager extends AuthenticationManager {

	private static Logger jlog = LogManager.getLogger(KustvaktAuthenticationManager.class);
	public static boolean DEBUG = false;
	
	private EncryptionIface crypto;
	private EntityHandlerIface entHandler;
	@Autowired
	private FullConfiguration config;
	@Autowired
	private Validator validator;
	
	public KustvaktAuthenticationManager(EncryptionIface crypto,
			FullConfiguration config) {
	    super("id_tokens");
		this.config = config;
		this.crypto = crypto;
		// todo: load via beancontext
//		try {
//			this.validator = new ApacheValidator();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * get session object if token was a session token
	 * 
	 * @param token
	 * @param host
	 * @param useragent
	 * @return
	 * @throws KustvaktException
	 */
	@Override
	public TokenContext getTokenContext(TokenType type, String token, 
	        String host, String userAgent) throws KustvaktException {

		AuthenticationIface provider = getProvider(type , null);

		if (provider == null){
			throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT, 
			        "Authentication provider for token type "+type
			        +" is not found.", type.displayName());
		}
		
		TokenContext context = provider.getTokenContext(token);
		context.setHostAddress(host);
		context.setUserAgent(userAgent);
		// if (!matchStatus(host, useragent, context))
		// provider.removeUserSession(token);
		return context;
	}

	@Override
	public User getUser(String username) throws KustvaktException {
		// User user;
		// Object value = this.getCacheValue(username);

		if (User.UserFactory.isDemo(username))
			return User.UserFactory.getDemoUser();

		// if (value != null) {
		// Map map = (Map) value;
		// user = User.UserFactory.toUser(map);
		// }
		// else {
		// user = entHandler.getAccount(username);
		// this.storeInCache(username, user.toCache());
		// todo: not valid. for the duration of the session, the host should not
		// change!
		// }
		// todo:
		// user.addField(Attributes.HOST, context.getHostAddress());
		// user.addField(Attributes.USER_AGENT, context.getUserAgent());
		
		//EM:copied from EntityDao
		KorAPUser user = new KorAPUser(); // oder eigentlich new DemoUser oder new DefaultUser.
        user.setUsername(username);
        return user;
//		return entHandler.getAccount(username);
	}
	
	@Override
    public User getUser (String username, String method)
            throws KustvaktException {
	    KorAPUser user = new KorAPUser();
        user.setUsername(username);
        String email = null;
        switch (method.toLowerCase()) {
            case "ldap":
                email = config.getTestEmail();
                break;
            default:
                email = config.getTestEmail();
                break;
        }
        user.setEmail(email);
        return user;
    }

	public TokenContext refresh(TokenContext context) throws KustvaktException {
		AuthenticationIface provider = getProvider(context.getTokenType(), null);
		if (provider == null) {
			// todo:
		}

		try {
//			provider.removeUserSession(context.getToken());
			User user = getUser(context.getUsername());
			return provider.createTokenContext(user, context.params());
		} catch (KustvaktException e) {
			throw new WrappedException(e, StatusCodes.LOGIN_FAILED);
		}
	}

	/** EM: fix type is not flexible
	 * @param type
	 * @param attributes
	 *            contains username and password to authenticate the user.
	 *            Depending of the authentication schema, may contain other
	 *            values as well
	 * @return User
	 * @throws KustvaktException
	 */
	@Override
	public User authenticate(AuthenticationMethod method, String username, String password, Map<String, Object> attributes)
			throws KustvaktException {
		User user;
		switch (method) {
		case SHIBBOLETH:
			// todo:
			user = authenticateShib(attributes);
			break;
		case LDAP:
			// IdM/LDAP: (09.02.17/FB)
			user = authenticateIdM(username, password, attributes);
			break;
		// EM: added a dummy authentication for testing
		case TEST:
		    user = getUser(username);
		    break;
		default:
			user = authenticate(username, password, attributes);
			break;
		}
		return user;
	}

	// a. set location depending on X-Forwarded-For.
	// X-Forwarded-For: clientIP, ProxyID, ProxyID...
	// the following private address spaces may be used to define intranet
	// spaces:
	// 10.0.0.0 - 10.255.255.255 (10/8 prefix)
	// 172.16.0.0 - 172.31.255.255 (172.16/12 prefix)
	// 192.168.0.0 - 192.168.255.255 (192.168/16 prefix)
	// b. set corpusAccess depending on location:
	// c. DemoUser only gets corpusAccess=FREE.
	// 16.05.17/FB

	@Override
	public void setAccessAndLocation(User user, HttpHeaders headers) {
		MultivaluedMap<String, String> headerMap = headers.getRequestHeaders();
		Location location = Location.EXTERN;
		CorpusAccess corpusAccess = CorpusAccess.FREE;
		
	    if( user instanceof DemoUser )
	    {
	    	// to be absolutely sure:
	    	user.setCorpusAccess(User.CorpusAccess.FREE);
            if (DEBUG) {
                jlog.debug("setAccessAndLocation: DemoUser: location="
                        + user.locationtoString() + " access="
                        + user.accesstoString());
            }
	     	return;
	    }
		
		if (headerMap != null && headerMap.containsKey(HttpHeader.X_FORWARDED_FOR.toString())) {

			String[] vals = headerMap.getFirst(HttpHeader.X_FORWARDED_FOR.toString()).split(",");
			String clientAddress = vals[0];

			try {
				InetAddress ip = InetAddress.getByName(clientAddress);
				if (ip.isSiteLocalAddress()){
					location = Location.INTERN;
					corpusAccess = CorpusAccess.ALL;
				}
				else{
					corpusAccess = CorpusAccess.PUB;
				}
				
				if (DEBUG){
                    jlog.debug(String.format(
                            "X-Forwarded-For : '%s' (%d values) -> %s\n",
                            Arrays.toString(vals), vals.length, vals[0]));
                    jlog.debug(String.format(
                            "X-Forwarded-For : location = %s corpusAccess = %s\n",
                            location == Location.INTERN ? "INTERN" : "EXTERN",
                            corpusAccess == CorpusAccess.ALL ? "ALL"
                                    : corpusAccess == CorpusAccess.PUB ? "PUB"
                                            : "FREE"));
                }

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			user.setLocation(location);
			user.setCorpusAccess(corpusAccess);
	    	
            if (DEBUG) {
                jlog.debug("setAccessAndLocation: KorAPUser: location="
                        + user.locationtoString() + ", access="
                        + user.accesstoString());
            }

		}
	} // getAccess

	@Override
	public TokenContext createTokenContext(User user, Map<String, Object> attr, TokenType type)
			throws KustvaktException {
	    //  use api token
		AuthenticationIface provider = getProvider(type, TokenType.API);

		// EM: not in the new DB
//		if (attr.get(Attributes.SCOPES) != null)
//			this.getUserData(user, UserDetails.class);

		TokenContext context = provider.createTokenContext(user, attr);
		if (context == null)
			throw new KustvaktException(StatusCodes.NOT_SUPPORTED);
		context.setUserAgent((String) attr.get(Attributes.USER_AGENT));
		context.setHostAddress(Attributes.HOST);
		return context;
	}

	@Deprecated
	private User authenticateShib(Map<String, Object> attributes) throws KustvaktException {
		// todo use persistent id, since eppn is not unique
		String eppn = (String) attributes.get(Attributes.EPPN);

		if (eppn == null || eppn.isEmpty())
			throw new KustvaktException(StatusCodes.REQUEST_INVALID);

		if (!attributes.containsKey(Attributes.EMAIL) && validator.isValid(eppn, Attributes.EMAIL))
			attributes.put(Attributes.EMAIL, eppn);

		User user = null;
		return user;
	}

	// todo: what if attributes null?
	private User authenticate(String username, String password, Map<String, Object> attr) throws KustvaktException {

		Map<String, Object> attributes = validator.validateMap(attr);
		User unknown;
		// just to make sure that the plain password does not appear anywhere in
		// the logs!

		try {
			validator.validateEntry(username, Attributes.USERNAME);
		} catch (KustvaktException e) {
			throw new WrappedException(e, StatusCodes.LOGIN_FAILED, username);
		}

		if (username == null || username.isEmpty())
			throw new WrappedException(new KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
					StatusCodes.LOGIN_FAILED);
		else {
			try {
				unknown = entHandler.getAccount(username);
			} catch (EmptyResultException e) {
				// mask exception to disable user guessing in possible attacks
				throw new WrappedException(new KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
						StatusCodes.LOGIN_FAILED, username);
			} catch (KustvaktException e) {
				jlog.error("Error: {}", e);
				throw new WrappedException(e, StatusCodes.LOGIN_FAILED, attributes.toString());
			}
		}

        if (DEBUG) {
            jlog.debug(
                    "Authentication: found username " + unknown.getUsername());
        }
		if (unknown instanceof KorAPUser) {
			if (password == null || password.isEmpty())
				throw new WrappedException(new KustvaktException(unknown.getId(), StatusCodes.BAD_CREDENTIALS),
						StatusCodes.LOGIN_FAILED, username);

			KorAPUser user = (KorAPUser) unknown;
			boolean check = crypto.checkHash(password, user.getPassword());

			if (!check) {
				// the fail counter only applies for wrong password
				jlog.warn("Wrong Password!");
				throw new WrappedException(new KustvaktException(user.getId(), StatusCodes.BAD_CREDENTIALS),
						StatusCodes.LOGIN_FAILED, username);
			}

			// bad credentials error has precedence over account locked or
			// unconfirmed codes
			// since latter can lead to account guessing of third parties
			if (user.isAccountLocked()) {
				URIParam param = (URIParam) user.getField(URIParam.class);

				if (param.hasValues()) {
                    if (DEBUG) {
                        jlog.debug("Account is not yet activated for user '"
                                + user.getUsername() + "'");
                    }
					if (TimeUtils.getNow().isAfter(param.getUriExpiration())) {
						jlog.error("URI token is expired. Deleting account for user "+ user.getUsername());
						throw new WrappedException(
								new KustvaktException(unknown.getId(), StatusCodes.EXPIRED,
										"account confirmation uri has expired!", param.getUriFragment()),
								StatusCodes.LOGIN_FAILED, username);
					}
					throw new WrappedException(
							new KustvaktException(unknown.getId(), StatusCodes.ACCOUNT_NOT_CONFIRMED),
							StatusCodes.LOGIN_FAILED, username);
				}
				jlog.error("ACCESS DENIED: account not active for '"+unknown.getUsername()+"'");
				throw new WrappedException(new KustvaktException(unknown.getId(), StatusCodes.ACCOUNT_DEACTIVATED),
						StatusCodes.LOGIN_FAILED, username);
			}

		} else if (unknown instanceof ShibbolethUser) {
			// todo
		}
        if (DEBUG) {
            jlog.debug("Authentication done: " + unknown);
        }
		return unknown;
	}

	/**
	 * authenticate using IdM (Identitätsmanagement) accessed by LDAP.
	 * 
	 * @param username
	 * @param password
	 * @param attr
	 * @return
	 * @throws KustvaktException
	 * @date 09.02.17/FB
	 */
	// todo: what if attributes null?

	private User authenticateIdM(String username, String password, Map<String, Object> attr) throws KustvaktException {

		User unknown = null;
		// just to make sure that the plain password does not appear anywhere in
		// the logs!
		if (DEBUG){
            jlog.debug("Debug: authenticateIdM: entering for '%s'...\n", username);
		}
		/**
		 * wozu Apache Validatoren für User/Passwort für IdM/LDAP? siehe
		 * validation.properties. Abgeschaltet 21.04.17/FB try {
		 * validator.validateEntry(username, Attributes.USERNAME); } catch
		 * (KustvaktException e) { throw new WrappedException(e,
		 * StatusCodes.LOGIN_FAILED, username); }
		 */
		if (username == null || username.isEmpty() || password == null || password.isEmpty())
			throw new WrappedException(new KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
					StatusCodes.LOGIN_FAILED);

		// LDAP Access:
		try {
			// todo: unknown = ...
			int ret = LdapAuth3.login(username, password, config.getLdapConfig());
			if (DEBUG){
			    jlog.debug("Debug: autenticationIdM: Ldap.login(%s) returns: %d.\n", username, ret);
			}
			if (ret != LdapAuth3.LDAP_AUTH_ROK) {
				jlog.error("LdapAuth3.login(username='"+username+"') returns '"+ret+"'='"+LdapAuth3.getErrMessage(ret)+"'!");

				// mask exception to disable user guessing in possible attacks
				/*
				 * by Hanl throw new WrappedException(new
				 * KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
				 * StatusCodes.LOGIN_FAILED, username);
				 */
				throw new WrappedException(new KustvaktException(username, StatusCodes.LDAP_BASE_ERRCODE + ret,
						LdapAuth3.getErrMessage(ret), null), StatusCodes.LOGIN_FAILED, username);
			}
		} catch (LDAPException e) {

			jlog.error("Error: username='"+username+"' -> '"+e+"'!");
			// mask exception to disable user guessing in possible attacks
			/*
			 * by Hanl: throw new WrappedException(new
			 * KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
			 * StatusCodes.LOGIN_FAILED, username);
			 */
			throw new WrappedException(
					new KustvaktException(username, StatusCodes.LDAP_BASE_ERRCODE + LdapAuth3.LDAP_AUTH_RINTERR,
							LdapAuth3.getErrMessage(LdapAuth3.LDAP_AUTH_RINTERR), null),
					StatusCodes.LOGIN_FAILED, username);
		}

		// Create a User
		// TODO: KorAPUser für solche mit einem bestehenden Account
		// DefaultUser sonst.
		User user = new KorAPUser();
		user.setUsername(username);
		/*
		 * folgender Code funktioniert hier noch nicht, da die Headers noch
		 * nicht ausgewertet worden sind - 23.05.17/FB Object o =
		 * attr.get(Attributes.LOCATION); String loc = (String)o.toString(); int
		 * location = Integer.parseInt(loc); user.setLocation(location);
		 * user.setCorpusAccess(Integer.parseInt(attr.get(Attributes.
		 * CORPUS_ACCESS).toString()));
		 */
		unknown = user;

        if (DEBUG) {
            jlog.trace(
                    "Authentication: found username " + unknown.getUsername());
        }
		if (unknown instanceof KorAPUser) {
			/*
			 * password already checked using LDAP: if (password == null ||
			 * password.isEmpty()) throw new WrappedException(new
			 * KustvaktException( unknown.getId(), StatusCodes.BAD_CREDENTIALS),
			 * StatusCodes.LOGIN_FAILED, username);
			 * 
			 * KorAPUser user = (KorAPUser) unknown; boolean check =
			 * crypto.checkHash(password, user.getPassword());
			 * 
			 * if (!check) { // the fail counter only applies for wrong password
			 * jlog.warn("Wrong Password!"); processLoginFail(unknown); throw
			 * new WrappedException(new KustvaktException(user.getId(),
			 * StatusCodes.BAD_CREDENTIALS), StatusCodes.LOGIN_FAILED,
			 * username); }
			 */
			// bad credentials error has precedence over account locked or
			// unconfirmed codes
			// since latter can lead to account guessing of third parties
			/*
			 * if (user.isAccountLocked()) {
			 * 
			 * URIParam param = (URIParam) user.getField(URIParam.class);
			 * 
			 * if (param.hasValues()) {
			 * jlog.debug("Account is not yet activated for user '{}'",
			 * user.getUsername()); if
			 * (TimeUtils.getNow().isAfter(param.getUriExpiration())) {
			 * jlog.error( "URI token is expired. Deleting account for user {}",
			 * user.getUsername()); deleteAccount(user); throw new
			 * WrappedException(new KustvaktException( unknown.getId(),
			 * StatusCodes.EXPIRED, "account confirmation uri has expired!",
			 * param.getUriFragment()), StatusCodes.LOGIN_FAILED, username); }
			 * throw new WrappedException(new KustvaktException(
			 * unknown.getId(), StatusCodes.ACCOUNT_NOT_CONFIRMED),
			 * StatusCodes.LOGIN_FAILED, username); }
			 * jlog.error("ACCESS DENIED: account not active for '{}'",
			 * unknown.getUsername()); throw new WrappedException(new
			 * KustvaktException( unknown.getId(),
			 * StatusCodes.ACCOUNT_DEACTIVATED), StatusCodes.LOGIN_FAILED,
			 * username); }
			 */

		} 
//		else if (unknown instanceof ShibbolethUser) {
//			// todo
//		}

        if (DEBUG) {
            jlog.debug("Authentication done: " + username);
        }
		return unknown;

	} // authenticateIdM

}
