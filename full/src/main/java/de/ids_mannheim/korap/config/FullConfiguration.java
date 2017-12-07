package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

import lombok.Getter;

/** Configuration for Kustvakt full version including properties concerning
 *  authentication and licenses. 
 * 
 * @author margaretha
 *
 */
@Getter
public class FullConfiguration extends KustvaktConfiguration {

    private String ldapConfig;

    private String freeOnlyRegex;
    private String publicOnlyRegex;
    private String allOnlyRegex;

    private Pattern publicLicensePattern;
    private Pattern freeLicensePattern;
    private Pattern allLicensePattern;

    private String authenticationScheme;

    public FullConfiguration (Properties properties) throws IOException {
        super(properties);
    }

    @Override
    public void load (Properties properties) throws IOException {

        super.load(properties);
        // EM: regex used for storing vc
        setLicenseRegex(properties);

        // EM: pattern for matching availability in Krill matches
        setLicensePatterns(properties);

        ldapConfig = properties.getProperty("ldap.config");
    }

    private void setLicensePatterns (Properties properties) {
        freeLicensePattern = compilePattern(freeOnlyRegex);
        publicLicensePattern =
                compilePattern(freeOnlyRegex + "|" + publicOnlyRegex);
        allLicensePattern = compilePattern(
                freeOnlyRegex + "|" + publicOnlyRegex + "|" + allOnlyRegex);
    }

    private void setLicenseRegex (Properties properties) {
        freeOnlyRegex = properties.getProperty("availability.regex.free", "");
        publicOnlyRegex =
                properties.getProperty("availability.regex.public", "");
        allOnlyRegex = properties.getProperty("availability.regex.all", "");
    }


    private Pattern compilePattern (String patternStr) {
        if (!patternStr.isEmpty()) {
            return Pattern.compile(patternStr);
        }
        else {
            return null;
        }
    }

}
