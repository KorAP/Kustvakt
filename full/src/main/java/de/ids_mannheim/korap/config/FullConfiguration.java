package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** Configuration for Kustvakt full version including properties concerning
 *  authentication and licenses. 
 * 
 * @author margaretha
 *
 */

public class FullConfiguration extends KustvaktConfiguration {

    private String ldapConfig;

    private String freeOnlyRegex;
    private String publicOnlyRegex;
    private String allOnlyRegex;

    private List<String> freeRegexList;
    private List<String> publicRegexList;
    private List<String> allRegexList;

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
        setFreeLicensePattern(compilePattern(freeOnlyRegex));
        setPublicLicensePattern(
                compilePattern(freeOnlyRegex + "|" + publicOnlyRegex));
        setAllLicensePattern(compilePattern(
                freeOnlyRegex + "|" + publicOnlyRegex + "|" + allOnlyRegex));
    }

    private void setLicenseRegex (Properties properties) {
        freeOnlyRegex = properties.getProperty("availability.regex.free", "");
        freeRegexList = splitAndAddToList(freeOnlyRegex);

        publicOnlyRegex =
                properties.getProperty("availability.regex.public", "");
        publicRegexList = splitAndAddToList(publicOnlyRegex);

        allOnlyRegex = properties.getProperty("availability.regex.all", "");
        allRegexList = splitAndAddToList(allOnlyRegex);
    }

    private List<String> splitAndAddToList (String regex) {
        List<String> list;
        if (regex.contains("|")) {
            String[] regexes = regex.split("\\|");
            list = new ArrayList<>(regexes.length);
            for (String s : regexes) {
                list.add(s.trim());
            }
        }
        else{
            list = new ArrayList<>(1);
            list.add(regex);
        }
        return list;
    }


    private Pattern compilePattern (String patternStr) {
        if (!patternStr.isEmpty()) {
            return Pattern.compile(patternStr);
        }
        else {
            return null;
        }
    }

    public String getLdapConfig () {
        return ldapConfig;
    }

    public Pattern getPublicLicensePattern () {
        return publicLicensePattern;
    }

    public void setPublicLicensePattern (Pattern publicLicensePattern) {
        this.publicLicensePattern = publicLicensePattern;
    }

    public Pattern getFreeLicensePattern () {
        return freeLicensePattern;
    }

    public void setFreeLicensePattern (Pattern freeLicensePattern) {
        this.freeLicensePattern = freeLicensePattern;
    }

    public Pattern getAllLicensePattern () {
        return allLicensePattern;
    }

    public void setAllLicensePattern (Pattern allLicensePattern) {
        this.allLicensePattern = allLicensePattern;
    }

    public String getAuthenticationScheme () {
        return authenticationScheme;
    }

    public void setAuthenticationScheme (String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    public List<String> getFreeRegexList () {
        return freeRegexList;
    }

    public void setFreeRegexList (List<String> freeRegexList) {
        this.freeRegexList = freeRegexList;
    }

    public List<String> getPublicRegexList () {
        return publicRegexList;
    }

    public void setPublicRegexList (List<String> publicRegexList) {
        this.publicRegexList = publicRegexList;
    }

    public List<String> getAllRegexList () {
        return allRegexList;
    }

    public void setAllRegexList (List<String> allRegexList) {
        this.allRegexList = allRegexList;
    }

}
