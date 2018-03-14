package de.ids_mannheim.korap.config;

import java.util.List;

import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.AdminHandlerIface;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;

/**
 * @author hanl
 * @date 20/02/2016
 */
public interface TestBeans {

	public  PolicyHandlerIface getPolicyDao();

//	public  KustvaktConfiguration getConfig();

	public  EntityHandlerIface getUserDao();
	
	public  AdminHandlerIface getAdminDao();

	public  AuditingIface getAuditingDao();

	public  List<ResourceOperationIface> getResourceDaos();

	public  List<UserDataDbIface> getUserdataDaos();

	public  EncryptionIface getCrypto();

	public  AuthenticationManagerIface getAuthManager();

	

}
