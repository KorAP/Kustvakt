package de.ids_mannheim.korap.service;

import java.sql.SQLException;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.utils.JsonUtils;

import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.QueryReferenceDao;

import de.ids_mannheim.korap.constant.VirtualCorpusType;

import de.ids_mannheim.korap.entity.QueryReference;


/**
 * This service is similar to VirtualCorpusService,
 * while not as complete.
 * For the moment, e.g., there is no mechanism supported to
 * share a query reference with other users or groups.
 * Only private queries are supported.
 *
 * @author diewald
 */
@Service
public class QueryReferenceService {

    public static Pattern qNamePattern = Pattern.compile("[-\\w.]+");

    @Autowired
    private AdminDao adminDao;

    @Autowired
    private QueryReferenceDao qDao;


    /**
     * Serch for a query by its name.
     */
    public JsonNode searchQueryByName (String username,
                                       String qName,
                                       String createdBy) throws KustvaktException {

        QueryReference qr = qDao.retrieveQueryByName(qName, createdBy);
        if (qr == null) {
            String refCode = createdBy + "/" + qName;
            throw new KustvaktException(
                StatusCodes.NO_RESOURCE_FOUND,
                "Query reference " + refCode + " is not found.",
                String.valueOf(refCode));
        }

        // TODO:
        //   checkVCAcess(q, username);
        return JsonUtils.readTree(qr.getKoralQuery());
    };


    /**
     * Store a query in the database.
     */
    public void storeQuery (String qJson,
                            String qName,
                            String createdBy)
        throws KustvaktException {

        // TODO:
        //   This doesn't support a whole bunch of applicable
        //   information from VCs, like 'definition', 'description',
        //   'status' etc.
        
        storeQuery(
            qJson,
            qName,
            "",
            createdBy
            );
    }


    /**
     * Store a query in the database.
     */
    public void storeQuery (String qJson,
                            String qName,
                            String desc,
                            String username)
        throws KustvaktException {
        ParameterChecker.checkStringValue(qJson, "q");
        ParameterChecker.checkNameValue(qName, "qName");

        if (!qNamePattern.matcher(qName).matches()) {
            throw new KustvaktException(
                StatusCodes.INVALID_ARGUMENT,
                "Query name must only contain letters, numbers, "
                + "underscores, hypens and spaces",
                qName);
        }

        if (username.equals("system") && !adminDao.isAdmin(username)) {
            throw new KustvaktException(
                StatusCodes.AUTHORIZATION_FAILED,
                "Unauthorized operation for user: " + username, username);            
        };

        int qId = 0;
        try {
            qId = qDao.createQuery(
                qName,
                VirtualCorpusType.PRIVATE,
                qJson,
                "", // TODO: definition,
                desc,
                "", // TODO: status,
                username);

        }
        catch (Exception e) {
            Throwable cause = e;
            Throwable lastCause = null;
            while ((cause = cause.getCause()) != null
                   && !cause.equals(lastCause)) {
                if (cause instanceof SQLException) {
                    break;
                }
                lastCause = cause;
            }
            throw new KustvaktException(
                StatusCodes.DB_INSERT_FAILED,
                cause.getMessage()
                );
        };

        // TODO:
        //   This doesn't publish the query, if it is meant to be published
        //   based on its type.
    };

    /*
    public Status handlePutRequest (String username,
                                    String qCreator,
                                    String qName,
                                    String qJson) throws KustvaktException {
        
        verifyUsername(username, qCreator);
        String q = vcDao.retrieveQueryByName(qName, qCreator);
        // ParameterChecker.checkObjectValue(qJson, "request entity");
        ParameterChecker.checkStringValue(qJson, "q");
        if (q == null) {
            storeQuery(qJson, qName, username);
            return Status.CREATED;
        }
        else {
            editQuery(q, qJson, qName, username);
            return Status.NO_CONTENT;
        };
    };
    */

    /*
    public void editQuery (QueryRefrence existingQ,
                           String newQJson,
                           String qName,
                           String username) throws KustvaktException {

        if (!username.equals(existingQ.getCreatedBy())
            && !adminDao.isAdmin(username)) {
            throw new KustvaktException(
                StatusCodes.AUTHORIZATION_FAILED,
                "Unauthorized operation for user: " + username, username);
        };

        String corpusQuery = newVC.getCorpusQuery();

        if (newQJson != null && !newQJson.isEmpty()) {
            koralQuery = serializeCorpusQuery(corpusQuery);
            requiredAccess = determineRequiredAccess(newVC.isCached(), vcName,
                    koralQuery);
        };

        VirtualCorpusType type = newVC.getType();
        if (type != null) {
            if (existingVC.getType().equals(VirtualCorpusType.PUBLISHED)) {
                // withdraw from publication
                if (!type.equals(VirtualCorpusType.PUBLISHED)) {
                    VirtualCorpusAccess hiddenAccess =
                            accessDao.retrieveHiddenAccess(existingVC.getId());
                    deleteVCAccess(hiddenAccess.getId(), "system");
                    int groupId = hiddenAccess.getUserGroup().getId();
                    userGroupService.deleteAutoHiddenGroup(groupId, "system");
                    // EM: should the users within the hidden group receive 
                    // notifications? 
                }
                // else remains the same
            }
            else if (type.equals(VirtualCorpusType.PUBLISHED)) {
                publishVC(existingVC.getId());
            }
        }

        vcDao.editVirtualCorpus(existingVC, vcName, type, requiredAccess,
                koralQuery, newVC.getDefinition(), newVC.getDescription(),
                newVC.getStatus(), newVC.isCached());
    }
    */

    
    /**
     * Only admin and the owner of the virtual corpus are allowed to
     * delete a virtual corpus.
     */
    public void deleteQueryByName (
        String username,
        String qName,
        String createdBy
        ) throws KustvaktException {

        QueryReference q = qDao.retrieveQueryByName(qName, createdBy);

        if (q == null) {
            String refCode = createdBy + "/" + qName;
            throw new KustvaktException(
                StatusCodes.NO_RESOURCE_FOUND,
                "Query reference " + refCode + " is not found.",
                String.valueOf(refCode));
        }

        // Check if the user created the qr or is admin
        else if (q.getCreatedBy().equals(username)
                 || adminDao.isAdmin(username)) {
            // TODO:
            //   Here checks for publication status is missing
            qDao.deleteQueryReference(q);
        }

        else {
            throw new KustvaktException(
                StatusCodes.AUTHORIZATION_FAILED,
                "Unauthorized operation for user: " + username, username);
        };
    };


    /*
    public void editVC (
        VirtualCorpus existingVC,
        VirtualCorpusJson newVC,
        String vcName,
        String username) throws KustvaktException {

        if (!username.equals(existingVC.getCreatedBy())
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        String koralQuery = null;
        CorpusAccess requiredAccess = null;
        String corpusQuery = newVC.getCorpusQuery();
        if (corpusQuery != null && !corpusQuery.isEmpty()) {
            koralQuery = serializeCorpusQuery(corpusQuery);
            requiredAccess = determineRequiredAccess(newVC.isCached(), vcName,
                    koralQuery);
        }

        VirtualCorpusType type = newVC.getType();
        if (type != null) {
            if (existingVC.getType().equals(VirtualCorpusType.PUBLISHED)) {
                // withdraw from publication
                if (!type.equals(VirtualCorpusType.PUBLISHED)) {
                    VirtualCorpusAccess hiddenAccess =
                            accessDao.retrieveHiddenAccess(existingVC.getId());
                    deleteVCAccess(hiddenAccess.getId(), "system");
                    int groupId = hiddenAccess.getUserGroup().getId();
                    userGroupService.deleteAutoHiddenGroup(groupId, "system");
                    // EM: should the users within the hidden group receive 
                    // notifications? 
                }
                // else remains the same
            }
            else if (type.equals(VirtualCorpusType.PUBLISHED)) {
                publishVC(existingVC.getId());
            }
        }

        vcDao.editVirtualCorpus(existingVC, vcName, type, requiredAccess,
                koralQuery, newVC.getDefinition(), newVC.getDescription(),
                newVC.getStatus(), newVC.isCached());
    }
    */
};
