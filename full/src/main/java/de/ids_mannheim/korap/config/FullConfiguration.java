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
    // mail configuration
    private boolean isMailEnabled;
    private String testEmail;
    private String noReply;

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

    private boolean isSoftDeleteAutoGroup;
    private boolean isSoftDeleteGroup;
    private boolean isSoftDeleteGroupMember;

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
        setDeleteConfiguration(properties);
        setMailConfiguration(properties);
        ldapConfig = properties.getProperty("ldap.config");

    }

    private void setMailConfiguration (Properties properties) {
        setMailEnabled(Boolean.valueOf(properties.getProperty("mail.enabled", "false")));
        if (isMailEnabled){
            // other properties must be set in the kustvakt.conf
            setTestEmail(properties.getProperty("mail.receiver"));
            setNoReply(properties.getProperty("mail.sender"));
        }
    }

    private void setDeleteConfiguration (Properties properties) {
        setSoftDeleteGroup(
                parseDeleteConfig(properties.getProperty("delete.group", "")));
        setSoftDeleteAutoGroup(parseDeleteConfig(
                properties.getProperty("delete.auto.group", "")));
        setSoftDeleteGroupMember(parseDeleteConfig(
                properties.getProperty("delete.group.member", "")));
    }

    private boolean parseDeleteConfig (String deleteConfig) {
        return deleteConfig.equals("soft") ? true : false;
    }

    private void setLicensePatterns (Properties properties) {
        setFreeLicensePattern(compilePattern(getFreeOnlyRegex()));
        setPublicLicensePattern(compilePattern(
                getFreeOnlyRegex() + "|" + getPublicOnlyRegex()));
        setAllLicensePattern(compilePattern(getFreeOnlyRegex() + "|"
                + getPublicOnlyRegex() + "|" + getAllOnlyRegex()));
    }

    private void setLicenseRegex (Properties properties) {
        setFreeOnlyRegex(properties.getProperty("availability.regex.free", ""));
        freeRegexList = splitAndAddToList(getFreeOnlyRegex());

        setPublicOnlyRegex(
                properties.getProperty("availability.regex.public", ""));
        publicRegexList = splitAndAddToList(getPublicOnlyRegex());

        setAllOnlyRegex(properties.getProperty("availability.regex.all", ""));
        allRegexList = splitAndAddToList(getAllOnlyRegex());
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
        else {
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

    public String getFreeOnlyRegex () {
        return freeOnlyRegex;
    }

    public void setFreeOnlyRegex (String freeOnlyRegex) {
        this.freeOnlyRegex = freeOnlyRegex;
    }

    public String getPublicOnlyRegex () {
        return publicOnlyRegex;
    }

    public void setPublicOnlyRegex (String publicOnlyRegex) {
        this.publicOnlyRegex = publicOnlyRegex;
    }

    public String getAllOnlyRegex () {
        return allOnlyRegex;
    }

    public void setAllOnlyRegex (String allOnlyRegex) {
        this.allOnlyRegex = allOnlyRegex;
    }

    public boolean isSoftDeleteGroup () {
        return isSoftDeleteGroup;
    }

    public void setSoftDeleteGroup (boolean isSoftDeleteGroup) {
        this.isSoftDeleteGroup = isSoftDeleteGroup;
    }

    public boolean isSoftDeleteGroupMember () {
        return isSoftDeleteGroupMember;
    }

    public void setSoftDeleteGroupMember (boolean isSoftDeleteGroupMember) {
        this.isSoftDeleteGroupMember = isSoftDeleteGroupMember;
    }

    public boolean isSoftDeleteAutoGroup () {
        return isSoftDeleteAutoGroup;
    }

    public void setSoftDeleteAutoGroup (boolean isSoftDeleteAutoGroup) {
        this.isSoftDeleteAutoGroup = isSoftDeleteAutoGroup;
    }

    public String getTestEmail () {
        return testEmail;
    }

    public void setTestEmail (String testEmail) {
        this.testEmail = testEmail;
    }

    public boolean isMailEnabled () {
        return isMailEnabled;
    }

    public void setMailEnabled (boolean isMailEnabled) {
        this.isMailEnabled = isMailEnabled;
    }

    public String getNoReply () {
        return noReply;
    }

    public void setNoReply (String noReply) {
        this.noReply = noReply;
    }

}
