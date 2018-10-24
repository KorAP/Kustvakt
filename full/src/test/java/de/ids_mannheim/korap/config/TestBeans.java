package de.ids_mannheim.korap.config;

import java.util.List;

import de.ids_mannheim.korap.authentication.AuthenticationManager;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;

/**
 * @author hanl
 * @date 20/02/2016
 */
public interface TestBeans {

//	public  KustvaktConfiguration getConfig();

	public  EntityHandlerIface getUserDao();
	
	public  AuditingIface getAuditingDao();

	public  List<ResourceOperationIface> getResourceDaos();

	public  List<UserDataDbIface> getUserdataDaos();

	public  EncryptionIface getCrypto();

	public  AuthenticationManager getAuthManager();

}
