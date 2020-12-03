package de.ids_mannheim.korap.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.QueryJson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class QueryReferenceServiceTest {

    @Autowired
    private QueryReferenceService qService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void createQuery () throws KustvaktException {
        qService.storeQuery("{\"@type\":\"koral:token\"}", "new-query", "me" );
        JsonNode json = qService.searchQueryByName("me", "new-query", "me");
        assertEquals("koral:token", json.at("/@type").asText());
        qService.deleteQueryByName("me", "new-query", "me");
    };

    @Test
    public void testCreateNonUniqueQuery () throws KustvaktException {
        qService.storeQuery("{\"@type\":\"koral:token\"}", "new-query", "me" );
        thrown.expect(KustvaktException.class);
        qService.storeQuery("{\"@type\":\"koral:token\"}", "new-query", "me" );
        qService.deleteQueryByName("me", "new-query", "me");        
    };
};
