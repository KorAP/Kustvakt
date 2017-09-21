package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.handlers.AdminDao;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.*;
import org.springframework.context.annotation.Bean;

import java.util.List;

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
