package de.ids_mannheim.korap.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

    final static ObjectMapper mapper = new ObjectMapper();
    
    public JsonNode retrieveUserInfo (String username) {
        return mapper.createObjectNode().put("username", username);
    }
}
