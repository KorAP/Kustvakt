package de.ids_mannheim.korap.resources;


import de.ids_mannheim.korap.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 05/11/2014
 */
// todo: distinguish between document and text!
@Getter
@Setter
public class Document extends KustvaktResource {

    private String corpus;
    private boolean disabled;
    private String docSigle;


    public Document (String textSigle) {
        this.setPersistentID(textSigle);
        this.docSigle = StringUtils.getDocSigle(textSigle);
        this.corpus = StringUtils.getCorpusSigle(textSigle);
    }


    public Document (String docSigle, String textSigle) {
        this.setId(-1);
        this.setPersistentID(textSigle);
        this.docSigle = docSigle;
        this.corpus = StringUtils.getCorpusSigle(textSigle);
        this.setDisabled(true);
    }


    public Document (String docsigle, String textSigle, boolean disabled) {
        this(docsigle, textSigle);
        this.setDisabled(disabled);
    }


    public boolean isText () {
        return this.getPersistentID().contains(".");
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
