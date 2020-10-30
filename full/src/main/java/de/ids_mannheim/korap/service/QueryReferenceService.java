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
};
