package de.ids_mannheim.korap.web.utils;

/**
 * @author hanl
 * @date 12/04/2014
 */
public class HTMLBuilder {

    private StringBuilder html;
    private StringBuilder body;
    private String bodyAttr;


    public HTMLBuilder () {
        html = new StringBuilder();
        body = new StringBuilder();
        bodyAttr = "";
        html.append("<html>");
    }


    public void addHeader (String header, int h) {
        html.append("<h" + h + ">");
        html.append(header);
        html.append("</h" + h + ">");
    }


    public void addToBody (String body) {
        this.body.append(body);
    }


    public void addToBody (String body, String attributes) {
        this.body.append(body);
        bodyAttr = attributes;
    }


    public String build () {
        if (bodyAttr.isEmpty())
            html.append("<body>");
        else {
            html.append("<body ");
            html.append(bodyAttr);
            html.append(">");
        }

        html.append(body);
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }


    @Override
    public String toString () {
        return build();
    }
}
