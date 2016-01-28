package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.utils.BooleanUtils;
import de.ids_mannheim.korap.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: hanl
 * Date: 8/14/13
 * Time: 10:26 AM
 */

@Deprecated
@Getter
@Setter
public class UserSettings {

    // todo: use simple map for settings, not all the parameter
    //todo: --> use sqlbuilder to update settings


    private Map<String, Object> values;
    // those are the only important parameters!!
//    private Integer id;
//    private Integer userID;


    private Integer id;
    private Integer userID;
    private String fileNameForExport;
    //    @Deprecated
    //    private Integer itemForSimpleAnnotation;
    private String leftContextItemForExport;
    private Integer leftContextSizeForExport;
    private String locale;
    private String leftContextItem;
    private Integer leftContextSize;
    private String rightContextItem;
    private String rightContextItemForExport;
    private Integer rightContextSize;
    private Integer rightContextSizeForExport;
    private String selectedCollection;
    private String queryLanguage;
    private Integer pageLength;
    private boolean metadataQueryExpertModus;
    //    @Deprecated
    //    private Integer searchSettingsTab;
    //    @Deprecated
    //    private Integer selectedGraphType;
    //    @Deprecated
    //    private String selectedSortType;
    //    @Deprecated
    //    private String selectedViewForSearchResults;

    /**
     * default values for foundry specification! of structure ff/lay
     */
    private String defaultPOSfoundry;
    private String defaultLemmafoundry;
    //default foundry for constituent information (syntax trees) --> there is no actual layer for this information
    private String defaultConstfoundry;
    private String defaultRelfoundry;

    //todo: refactor to anonymous -- since data is collected either way!
    private boolean collectData;

    /**
     * creates an instance of this object with default values, mapped from a database/configuration file
     */
    public UserSettings() {
        setupDefaultSettings();
    }

    @Deprecated
    public static UserSettings fromObjectMap(Map<String, Object> m) {
        UserSettings s = new UserSettings();
        s.setFileNameForExport((String) m.get(Attributes.FILENAME_FOR_EXPORT));
        //        s.setItemForSimpleAnnotation(
        //                (Integer) m.get(Attributes.ITEM_FOR_SIMPLE_ANNOTATION));
        s.setLeftContextItemForExport(
                (String) m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT));
        s.setLeftContextSizeForExport(
                (Integer) m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT));
        s.setLocale((String) m.get(Attributes.LOCALE));
        s.setLeftContextItem((String) m.get(Attributes.LEFT_CONTEXT_ITEM));
        s.setLeftContextSize((Integer) m.get(Attributes.LEFT_CONTEXT_SIZE));
        s.setRightContextItem((String) m.get(Attributes.RIGHT_CONTEXT_ITEM));
        s.setRightContextItemForExport(
                (String) m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT));
        s.setRightContextSize((Integer) m.get(Attributes.RIGHT_CONTEXT_SIZE));
        s.setRightContextSizeForExport(
                (Integer) m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT));
        s.setSelectedCollection((String) m.get(Attributes.SELECTED_COLLECTION));
        s.setQueryLanguage((String) m.get(Attributes.QUERY_LANGUAGE));
        s.setPageLength((Integer) m.get(Attributes.PAGE_LENGTH));
        s.setMetadataQueryExpertModus((Boolean) BooleanUtils
                .getBoolean(m.get(Attributes.METADATA_QUERY_EXPERT_MODUS)));
        //        s.setSearchSettingsTab((Integer) m.get(Attributes.SEARCH_SETTINGS_TAB));
        //        s.setSelectedGraphType((Integer) m.get(Attributes.SELECTED_GRAPH_TYPE));
        //        s.setSelectedSortType((String) m.get(Attributes.SELECTED_SORT_TYPE));
        //        s.setSelectedViewForSearchResults(
        //                (String) m.get(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS));
        s.setCollectData((Boolean) BooleanUtils
                .getBoolean(m.get(Attributes.COLLECT_AUDITING_DATA)));

        s.setDefaultConstfoundry(
                (String) m.get(Attributes.DEFAULT_CONST_FOUNDRY));
        s.setDefaultRelfoundry((String) m.get(Attributes.DEFAULT_REL_FOUNDRY));
        s.setDefaultPOSfoundry((String) m.get(Attributes.DEFAULT_POS_FOUNDRY));
        s.setDefaultLemmafoundry(
                (String) m.get(Attributes.DEFAULT_LEMMA_FOUNDRY));

        s.setId((Integer) m.get("Id"));
        s.setUserID((Integer) m.get("userID"));
        return s;
    }

    @Deprecated
    public static UserSettings fromMap(Map<String, String> m) {
        UserSettings s = new UserSettings();
        s.setFileNameForExport(m.get(Attributes.FILENAME_FOR_EXPORT));
        //        s.setItemForSimpleAnnotation(
        //                Integer.valueOf(m.get(Attributes.ITEM_FOR_SIMPLE_ANNOTATION)));
        s.setLeftContextItemForExport(
                m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT));
        s.setLeftContextSizeForExport(Integer.valueOf(
                m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT)));
        s.setLocale(m.get(Attributes.LOCALE));
        s.setLeftContextItem(m.get(Attributes.LEFT_CONTEXT_ITEM));
        s.setLeftContextSize(
                Integer.valueOf(m.get(Attributes.LEFT_CONTEXT_SIZE)));
        s.setRightContextItem(m.get(Attributes.RIGHT_CONTEXT_ITEM));
        s.setRightContextItemForExport(
                m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT));
        s.setRightContextSize(
                Integer.valueOf(m.get(Attributes.RIGHT_CONTEXT_SIZE)));
        s.setRightContextSizeForExport(Integer.valueOf(
                m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT)));
        s.setSelectedCollection(m.get(Attributes.SELECTED_COLLECTION));
        s.setQueryLanguage(m.get(Attributes.QUERY_LANGUAGE));
        s.setPageLength(Integer.valueOf(m.get(Attributes.PAGE_LENGTH)));
        s.setMetadataQueryExpertModus(
                Boolean.valueOf(m.get(Attributes.METADATA_QUERY_EXPERT_MODUS)));
        //        s.setSearchSettingsTab(
        //                Integer.valueOf(m.get(Attributes.SEARCH_SETTINGS_TAB)));
        //        s.setSelectedGraphType(
        //                Integer.valueOf(m.get(Attributes.SELECTED_GRAPH_TYPE)));
        //        s.setSelectedSortType(m.get(Attributes.SELECTED_SORT_TYPE));
        //        s.setSelectedViewForSearchResults(
        //                m.get(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS));

        s.setCollectData(
                Boolean.valueOf(m.get(Attributes.COLLECT_AUDITING_DATA)));
        s.setDefaultConstfoundry(m.get(Attributes.DEFAULT_CONST_FOUNDRY));
        s.setDefaultRelfoundry(m.get(Attributes.DEFAULT_REL_FOUNDRY));
        s.setDefaultPOSfoundry(m.get(Attributes.DEFAULT_POS_FOUNDRY));
        s.setDefaultLemmafoundry(m.get(Attributes.DEFAULT_LEMMA_FOUNDRY));
        return s;
    }

    public static UserSettings newSettingsIterator(Map<String, String> m) {
        UserSettings s = new UserSettings();
        if (m.isEmpty())
            return s;

        s.setFileNameForExport(m.get(Attributes.FILENAME_FOR_EXPORT));
        //        s.setItemForSimpleAnnotation(
        //                Integer.valueOf(m.get(Attributes.ITEM_FOR_SIMPLE_ANNOTATION)));
        s.setLeftContextItemForExport(
                m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT));
        s.setLeftContextSizeForExport(Integer.valueOf(
                m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT)));
        s.setLocale(m.get(Attributes.LOCALE));
        s.setLeftContextItem(m.get(Attributes.LEFT_CONTEXT_ITEM));
        s.setLeftContextSize(
                Integer.valueOf(m.get(Attributes.LEFT_CONTEXT_SIZE)));
        s.setRightContextItem(m.get(Attributes.RIGHT_CONTEXT_ITEM));
        s.setRightContextItemForExport(
                m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT));
        s.setRightContextSize(
                Integer.valueOf(m.get(Attributes.RIGHT_CONTEXT_SIZE)));
        s.setRightContextSizeForExport(Integer.valueOf(
                m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT)));
        s.setSelectedCollection(m.get(Attributes.SELECTED_COLLECTION));
        s.setQueryLanguage(m.get(Attributes.QUERY_LANGUAGE));
        s.setPageLength(Integer.valueOf(m.get(Attributes.PAGE_LENGTH)));
        s.setMetadataQueryExpertModus(
                Boolean.valueOf(m.get(Attributes.METADATA_QUERY_EXPERT_MODUS)));
        //        s.setSearchSettingsTab(
        //                Integer.valueOf(m.get(Attributes.SEARCH_SETTINGS_TAB)));
        //        s.setSelectedGraphType(
        //                Integer.valueOf(m.get(Attributes.SELECTED_GRAPH_TYPE)));
        //        s.setSelectedSortType(m.get(Attributes.SELECTED_SORT_TYPE));
        //        s.setSelectedViewForSearchResults(
        //                m.get(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS));

        s.setCollectData(
                Boolean.valueOf(m.get(Attributes.COLLECT_AUDITING_DATA)));
        s.setDefaultConstfoundry(m.get(Attributes.DEFAULT_CONST_FOUNDRY));
        s.setDefaultRelfoundry(m.get(Attributes.DEFAULT_REL_FOUNDRY));
        s.setDefaultPOSfoundry(m.get(Attributes.DEFAULT_POS_FOUNDRY));
        s.setDefaultLemmafoundry(m.get(Attributes.DEFAULT_LEMMA_FOUNDRY));
        return s;
    }

    @Deprecated
    public void updateStringSettings(Map<String, String> m) {
        if (m.get(Attributes.FILENAME_FOR_EXPORT) != null)
            this.setFileNameForExport(m.get(Attributes.FILENAME_FOR_EXPORT));
        //        this.setItemForSimpleAnnotation(
        //                Integer.valueOf(m.get(Attributes.ITEM_FOR_SIMPLE_ANNOTATION)));
        if (m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT) != null)
            this.setLeftContextItemForExport(
                    m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT));
        if (m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT) != null)
            this.setLeftContextSizeForExport(Integer.valueOf(
                    m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT)));
        if (m.get(Attributes.LOCALE) != null)
            this.setLocale(m.get(Attributes.LOCALE));
        if (m.get(Attributes.LEFT_CONTEXT_ITEM) != null)
            this.setLeftContextItem(m.get(Attributes.LEFT_CONTEXT_ITEM));
        if (m.get(Attributes.LEFT_CONTEXT_SIZE) != null)
            this.setLeftContextSize(
                    Integer.valueOf(m.get(Attributes.LEFT_CONTEXT_SIZE)));
        if (m.get(Attributes.RIGHT_CONTEXT_ITEM) != null)
            this.setRightContextItem(m.get(Attributes.RIGHT_CONTEXT_ITEM));
        if (m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT) != null)
            this.setRightContextItemForExport(
                    m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT));
        if (m.get(Attributes.RIGHT_CONTEXT_SIZE) != null)
            this.setRightContextSize(
                    Integer.valueOf(m.get(Attributes.RIGHT_CONTEXT_SIZE)));
        if (m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT) != null)
            this.setRightContextSizeForExport(Integer.valueOf(
                    m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT)));
        if (m.get(Attributes.SELECTED_COLLECTION) != null)
            this.setSelectedCollection(m.get(Attributes.SELECTED_COLLECTION));
        if (m.get(Attributes.QUERY_LANGUAGE) != null)
            this.setQueryLanguage(m.get(Attributes.QUERY_LANGUAGE));
        if (m.get(Attributes.PAGE_LENGTH) != null)
            this.setPageLength(Integer.valueOf(m.get(Attributes.PAGE_LENGTH)));
        if (m.get(Attributes.METADATA_QUERY_EXPERT_MODUS) != null)
            this.setMetadataQueryExpertModus(Boolean.valueOf(
                    m.get(Attributes.METADATA_QUERY_EXPERT_MODUS)));
        //        this.setSearchSettingsTab(
        //                Integer.valueOf(m.get(Attributes.SEARCH_SETTINGS_TAB)));
        //        this.setSelectedGraphType(
        //                Integer.valueOf(m.get(Attributes.SELECTED_GRAPH_TYPE)));
        //        this.setSelectedSortType(m.get(Attributes.SELECTED_SORT_TYPE));
        //        this.setSelectedViewForSearchResults(
        //                m.get(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS));
        if (m.get(Attributes.COLLECT_AUDITING_DATA) != null)
            this.setCollectData(
                    Boolean.valueOf(m.get(Attributes.COLLECT_AUDITING_DATA)));
        if (m.get(Attributes.DEFAULT_POS_FOUNDRY) != null)
            this.setDefaultPOSfoundry(m.get(Attributes.DEFAULT_POS_FOUNDRY));
        if (m.get(Attributes.DEFAULT_LEMMA_FOUNDRY) != null)
            this.setDefaultLemmafoundry(
                    m.get(Attributes.DEFAULT_LEMMA_FOUNDRY));
        if (m.get(Attributes.DEFAULT_CONST_FOUNDRY) != null)
            this.setDefaultConstfoundry(
                    m.get(Attributes.DEFAULT_CONST_FOUNDRY));
        if (m.get(Attributes.DEFAULT_REL_FOUNDRY) != null)
            this.setDefaultRelfoundry(m.get(Attributes.DEFAULT_REL_FOUNDRY));
    }

    @Deprecated
    public void updateObjectSettings(Map<String, Object> m) {
        if (m.get(Attributes.FILENAME_FOR_EXPORT) != null)
            this.setFileNameForExport(
                    (String) m.get(Attributes.FILENAME_FOR_EXPORT));
        //        this.setItemForSimpleAnnotation(
        //                Integer.valueOf(m.get(Attributes.ITEM_FOR_SIMPLE_ANNOTATION)));
        if (m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT) != null)
            this.setLeftContextItemForExport(
                    (String) m.get(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT));
        if (m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT) != null)
            this.setLeftContextSizeForExport(Integer.valueOf(
                    (Integer) m.get(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT)));
        if (m.get(Attributes.LOCALE) != null)
            this.setLocale((String) m.get(Attributes.LOCALE));
        if (m.get(Attributes.LEFT_CONTEXT_ITEM) != null)
            this.setLeftContextItem(
                    (String) m.get(Attributes.LEFT_CONTEXT_ITEM));
        if (m.get(Attributes.LEFT_CONTEXT_SIZE) != null)
            this.setLeftContextSize(Integer.valueOf(
                    (Integer) m.get(Attributes.LEFT_CONTEXT_SIZE)));
        if (m.get(Attributes.RIGHT_CONTEXT_ITEM) != null)
            this.setRightContextItem(
                    (String) m.get(Attributes.RIGHT_CONTEXT_ITEM));
        if (m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT) != null)
            this.setRightContextItemForExport(
                    (String) m.get(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT));
        if (m.get(Attributes.RIGHT_CONTEXT_SIZE) != null)
            this.setRightContextSize(Integer.valueOf(
                    (Integer) m.get(Attributes.RIGHT_CONTEXT_SIZE)));
        if (m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT) != null)
            this.setRightContextSizeForExport(Integer.valueOf(
                    (Integer) m.get(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT)));
        if (m.get(Attributes.SELECTED_COLLECTION) != null)
            this.setSelectedCollection(
                    (String) m.get(Attributes.SELECTED_COLLECTION));
        if (m.get(Attributes.QUERY_LANGUAGE) != null)
            this.setQueryLanguage((String) m.get(Attributes.QUERY_LANGUAGE));
        if (m.get(Attributes.PAGE_LENGTH) != null)
            this.setPageLength((Integer) m.get(Attributes.PAGE_LENGTH));
        if (m.get(Attributes.METADATA_QUERY_EXPERT_MODUS) != null)
            this.setMetadataQueryExpertModus(Boolean.valueOf(
                    (Boolean) m.get(Attributes.METADATA_QUERY_EXPERT_MODUS)));
        //        this.setSearchSettingsTab(
        //                Integer.valueOf(m.get(Attributes.SEARCH_SETTINGS_TAB)));
        //        this.setSelectedGraphType(
        //                Integer.valueOf(m.get(Attributes.SELECTED_GRAPH_TYPE)));
        //        this.setSelectedSortType(m.get(Attributes.SELECTED_SORT_TYPE));
        //        this.setSelectedViewForSearchResults(
        //                m.get(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS));
        if (m.get(Attributes.COLLECT_AUDITING_DATA) != null)
            this.setCollectData(
                    (Boolean) m.get(Attributes.COLLECT_AUDITING_DATA));
        if (m.get(Attributes.DEFAULT_POS_FOUNDRY) != null)
            this.setDefaultPOSfoundry(
                    (String) m.get(Attributes.DEFAULT_POS_FOUNDRY));
        if (m.get(Attributes.DEFAULT_LEMMA_FOUNDRY) != null)
            this.setDefaultLemmafoundry(
                    (String) m.get(Attributes.DEFAULT_LEMMA_FOUNDRY));
        if (m.get(Attributes.DEFAULT_CONST_FOUNDRY) != null)
            this.setDefaultConstfoundry(
                    (String) m.get(Attributes.DEFAULT_CONST_FOUNDRY));
        if (m.get(Attributes.DEFAULT_REL_FOUNDRY) != null)
            this.setDefaultRelfoundry(
                    (String) m.get(Attributes.DEFAULT_REL_FOUNDRY));
    }

    //loadSubTypes from configuration?
    private void setupDefaultSettings() {
        this.setFileNameForExport("export");
        //        this.setItemForSimpleAnnotation(0);
        this.setLocale("de");
        this.setLeftContextItemForExport("char");
        this.setLeftContextSizeForExport(100);
        this.setLeftContextItem("char");
        this.setLeftContextSize(200);
        this.setRightContextItem("char");
        this.setRightContextItemForExport("char");
        this.setRightContextSize(200);
        this.setRightContextSizeForExport(100);
        // persistent id for wikipedia!
        // fixme: deprecation warning!
        this.setSelectedCollection(
                "ZGU0ZTllNTFkYzc3M2VhZmViYzdkYWE2ODI5NDc3NTk4NGQ1YThhOTMwOTNhOWYxNWMwN2M3Y2YyZmE3N2RlNQ==");
        this.setQueryLanguage("COSMAS2");
        this.setPageLength(25);
        this.setMetadataQueryExpertModus(true);
        //        this.setSearchSettingsTab(0);
        //        this.setSelectedGraphType(1);
        //        this.setSelectedSortType("FIFO");
        //        this.setSelectedViewForSearchResults("KWIC");

        this.setCollectData(true);
        this.setDefaultConstfoundry("mate");
        this.setDefaultRelfoundry("mate");
        this.setDefaultPOSfoundry("tt");
        this.setDefaultLemmafoundry("tt");
    }

    public Map toStringMap() {
        Map<String, String> m = new HashMap<>();
        m.put(Attributes.FILENAME_FOR_EXPORT, this.getFileNameForExport());
        //        m.put(Attributes.ITEM_FOR_SIMPLE_ANNOTATION,
        //                String.valueOf(this.getItemForSimpleAnnotation()));
        m.put(Attributes.LEFT_CONTEXT_SIZE,
                String.valueOf(this.getLeftContextSize()));
        m.put(Attributes.LEFT_CONTEXT_ITEM,
                String.valueOf(this.getLeftContextItem()));
        m.put(Attributes.LOCALE, this.getLocale());
        m.put(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT,
                String.valueOf(this.getLeftContextSizeForExport()));
        m.put(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT,
                String.valueOf(this.getLeftContextItemForExport()));
        m.put(Attributes.RIGHT_CONTEXT_ITEM,
                String.valueOf(this.getRightContextItem()));
        m.put(Attributes.RIGHT_CONTEXT_SIZE,
                String.valueOf(this.getRightContextSize()));
        m.put(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT,
                String.valueOf(this.getRightContextItemForExport()));
        m.put(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT,
                String.valueOf(this.getRightContextSizeForExport()));
        m.put(Attributes.SELECTED_COLLECTION, this.getSelectedCollection());
        m.put(Attributes.QUERY_LANGUAGE, this.getQueryLanguage());
        m.put(Attributes.PAGE_LENGTH, String.valueOf(this.getPageLength()));
        m.put(Attributes.METADATA_QUERY_EXPERT_MODUS,
                String.valueOf(this.isMetadataQueryExpertModus()));
        //        m.put(Attributes.SEARCH_SETTINGS_TAB,
        //                String.valueOf(this.getSearchSettingsTab()));
        //        m.put(Attributes.SELECTED_GRAPH_TYPE,
        //                String.valueOf(this.getSelectedGraphType()));
        //        m.put(Attributes.SELECTED_SORT_TYPE, this.getSelectedSortType());
        //        m.put(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS,
        //                this.getSelectedViewForSearchResults());
        m.put(Attributes.DEFAULT_POS_FOUNDRY, this.getDefaultPOSfoundry());
        m.put(Attributes.DEFAULT_LEMMA_FOUNDRY, this.getDefaultLemmafoundry());
        m.put(Attributes.DEFAULT_CONST_FOUNDRY, this.getDefaultConstfoundry());
        m.put(Attributes.DEFAULT_REL_FOUNDRY, this.getDefaultRelfoundry());
        m.put(Attributes.COLLECT_AUDITING_DATA,
                String.valueOf(this.isCollectData()));

        for (Map.Entry pair : m.entrySet()) {
            if (pair.getValue() == null)
                pair.setValue("");
        }
        return m;
    }

    public Map toObjectMap() {
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.FILENAME_FOR_EXPORT, this.getFileNameForExport());
        //        m.put(Attributes.ITEM_FOR_SIMPLE_ANNOTATION,
        //                this.getItemForSimpleAnnotation());
        m.put(Attributes.LEFT_CONTEXT_SIZE, this.getLeftContextSize());
        m.put(Attributes.LEFT_CONTEXT_ITEM, this.getLeftContextItem());
        m.put(Attributes.LOCALE, this.getLocale());
        m.put(Attributes.LEFT_CONTEXT_SIZE_FOR_EXPORT,
                this.getLeftContextSizeForExport());
        m.put(Attributes.LEFT_CONTEXT_ITEM_FOR_EXPORT,
                this.getLeftContextItemForExport());
        m.put(Attributes.RIGHT_CONTEXT_ITEM, this.getRightContextItem());
        m.put(Attributes.RIGHT_CONTEXT_SIZE, this.getRightContextSize());
        m.put(Attributes.RIGHT_CONTEXT_ITEM_FOR_EXPORT,
                this.getRightContextItemForExport());
        m.put(Attributes.RIGHT_CONTEXT_SIZE_FOR_EXPORT,
                this.getRightContextSizeForExport());
        m.put(Attributes.SELECTED_COLLECTION, this.getSelectedCollection());
        m.put(Attributes.QUERY_LANGUAGE, this.getQueryLanguage());
        m.put(Attributes.PAGE_LENGTH, this.getPageLength());
        m.put(Attributes.METADATA_QUERY_EXPERT_MODUS,
                this.isMetadataQueryExpertModus());
        //        m.put(Attributes.SEARCH_SETTINGS_TAB, this.getSearchSettingsTab());
        //        m.put(Attributes.SELECTED_GRAPH_TYPE, this.getSelectedGraphType());
        //        m.put(Attributes.SELECTED_SORT_TYPE, this.getSelectedSortType());
        //        m.put(Attributes.SELECTED_VIEW_FOR_SEARCH_RESULTS,
        //                this.getSelectedViewForSearchResults());
        m.put(Attributes.DEFAULT_POS_FOUNDRY, this.getDefaultPOSfoundry());
        m.put(Attributes.DEFAULT_LEMMA_FOUNDRY, this.getDefaultLemmafoundry());
        m.put(Attributes.DEFAULT_CONST_FOUNDRY, this.getDefaultConstfoundry());
        m.put(Attributes.DEFAULT_REL_FOUNDRY, this.getDefaultRelfoundry());
        m.put(Attributes.COLLECT_AUDITING_DATA, this.isCollectData());
        for (Map.Entry pair : m.entrySet()) {
            if (pair.getValue() == null)
                pair.setValue("");
        }
        return m;
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "id=" + id +
                ", userid=" + userID +
                ", fileNameForExport='" + fileNameForExport + '\'' +
                ", leftContextItemForExport='" + leftContextItemForExport + '\''
                +
                ", leftContextSizeForExport=" + leftContextSizeForExport +
                ", locale='" + locale + '\'' +
                ", leftContextItem='" + leftContextItem + '\'' +
                ", leftContextSize=" + leftContextSize +
                ", rightContextItem='" + rightContextItem + '\'' +
                ", rightContextItemForExport='" + rightContextItemForExport
                + '\'' +
                ", rightContextSize=" + rightContextSize +
                ", rightContextSizeForExport=" + rightContextSizeForExport +
                ", selectedCollection='" + selectedCollection + '\'' +
                ", queryLanguage='" + queryLanguage + '\'' +
                ", pageLength=" + pageLength +
                ", metadataQueryExpertModus=" + metadataQueryExpertModus +
                ", defaultPOSfoundry='" + defaultPOSfoundry + '\'' +
                ", defaultLemmafoundry='" + defaultLemmafoundry + '\'' +
                ", defaultConstfoundry='" + defaultConstfoundry + '\'' +
                ", defaultRelfoundry='" + defaultRelfoundry + '\'' +
                ", collectData='" + collectData + "\'" +
                '}';
    }

    public static UserSettings fromString(String value) {
        Map<String, Object> map;
        try {
            map = JsonUtils.read(value, Map.class);
        }catch (IOException e) {
            return new UserSettings();
        }
        return UserSettings.fromObjectMap(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        UserSettings that = (UserSettings) o;

        if (userID != null ? userID != that.userID : that.userID != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (userID != null ? userID.hashCode() : 0);
        return result;
    }

    public String toJSON() {
        return JsonUtils.toJSON(this);
    }
}
