package de.ids_mannheim.korap.interfaces.defaults;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.ConfigLoader;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.ValidatorIface;
import de.ids_mannheim.korap.web.utils.KustvaktMap;
import org.apache.commons.validator.routines.*;
import org.apache.commons.validator.routines.RegexValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Created by hanl on 09.06.16.
 */
public class ApacheValidator implements ValidatorIface {

    private static Logger jlog = LogManager.getLogger(ApacheValidator.class);

    private static final String STRING_PATTERN = "^[\\.;:,&\\|@\\[\\]\\=\\*\\/\\/_()\\-0-9\\p{L}\\p{Space}]{0,1024}$";

    private static final boolean DEBUG = false;

    private Map<String, RegexValidator> validators;


    public ApacheValidator () throws IOException {
        this.validators = load();
    }


    private static Map<String, RegexValidator> load () throws IOException {
        Map<String, RegexValidator> validatorMap = new HashMap<>();
        Properties p = ConfigLoader.loadProperties("validation.properties");

        for (String property : p.stringPropertyNames()) {
            if (property.startsWith("Validator")) {
                String name = property.replace("Validator.", "");
                RegexValidator v = new RegexValidator(
                        p.get(property).toString());
                validatorMap.put(name, v);
            }
        }
        return validatorMap;
    }



    @Override
    public Map<String, Object> validateMap (Map<String, Object> map)
            throws KustvaktException {
        Map<String, Object> safeMap = new HashMap<>();
        KustvaktMap kmap = new KustvaktMap(map);

        if (map != null) {
            loop: for (String key : kmap.keySet()) {
                Object value = kmap.getRaw(key);
                if (value instanceof List) {
                    List list = (List) value;
                    for (int i = 0; i < list.size(); i++) {
                        if (!isValid(String.valueOf(list.get(i)), key)) {
                            //                                list.remove(i);
                            throw new KustvaktException(
                                    StatusCodes.ILLEGAL_ARGUMENT,
                                    "The value for the parameter " + key
                                            + " is not valid or acceptable.");
                        }
                    }

                    if (list.size() == 1)
                        value = list.get(0);
                    else
                        value = list;
                }
                else {
                    if (!isValid(kmap.get(key), key))
                        continue loop;
                }
                safeMap.put(key, value);
            }
        }
        return safeMap;
    }


    @Override
    public String validateEntry (String input, String type)
            throws KustvaktException {
        if (!isValid(input, type))
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "Entry did not validate for type '" + type + "'", input);
        return input;
    }


    @Override
    public boolean isValid (String input, String type) {
        boolean valid = false;
        RegexValidator validator = this.validators.get(type);
        if (validator != null) {
            valid = validator.isValid(input);
        }
        else {
            if (Attributes.EMAIL.equals(type)) {
                valid = EmailValidator.getInstance().isValid(input);
            }
            else if ("date".equals(type)) {
                valid = DateValidator.getInstance().isValid(input);
            }
            else if ("string".equals(type)
                    && !this.validators.containsKey("string")) {
                RegexValidator regex = new RegexValidator(STRING_PATTERN);
                valid = regex.isValid(input);
            }
            else
                return this.isValid(input, "string");
        }
        if (DEBUG){
            jlog.debug("validating entry "+input+" of type "+type+": "+ (
                    valid ? "Is valid!" : "Is not valid!"));
        }
            
        return valid;
    }
}
