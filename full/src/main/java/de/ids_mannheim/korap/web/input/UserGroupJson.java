package de.ids_mannheim.korap.web.input;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserGroupJson {

    private String name;
    private List<String> members;
}
