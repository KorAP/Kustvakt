package de.ids_mannheim.korap.resources;

import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 05/11/2014
 */
@Getter
@Setter
public class Document extends KustvaktResource {

    private String corpus;
    private boolean disabled;


    public Document (String persistentID) {
        this.setId(-1);
        this.setPersistentID(persistentID);
        this.corpus = getCorpusID();
        this.setDisabled(true);
    }


    public Document (String persistentID, boolean disabled) {
        this(persistentID);
        this.setDisabled(disabled);
    }


    private String getCorpusID () {
        //WPD_SSS.07367
        if (this.getPersistentID() != null)
            return this.getPersistentID().split("_")[0];
        return null;
    }


    @Override
    public String toString () {
        return "Document{" + "id='" + this.getId() + "'" + "persistentid='"
                + this.getPersistentID() + "'" + "corpus='" + corpus + '\''
                + ", disabled=" + disabled + '}';
    }


    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Document document = (Document) o;

        if (disabled != document.disabled)
            return false;
        if (!getPersistentID().equals(document.getPersistentID()))
            return false;

        return true;
    }


    @Override
    public int hashCode () {
        int result = getPersistentID().hashCode();
        result = 31 * result + (disabled ? 1 : 0);
        return result;
    }
}
