package de.ids_mannheim.korap.web.input;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserGroupJson {

    private int id;
    private String name;
    private String[] members;
}
