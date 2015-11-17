package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/**
 * #ELEM(W ANA=N)
 * <p/>
 * {
 * "@context": "http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld",
 * "errors": [],
 * "warnings": [],
 * "messages": [],
 * "collection": {},
 * "query": {
 * "@type": "koral:span",
 * "key": "w",
 * "attr": {
 * "@type": "koral:term",
 * "layer": "p",
 * "key": "N",
 * "match": "match:eq"
 * }
 * },
 * "meta": {}
 * }
 * <p/>
 * <p/>
 * email reference:
 * Hallo Michael,
 * mir fiel gestern bei der neuen KoralQuery Serialisierung noch ein Fall
 * für default-Werte ein, die zumindest für viele Beispiele, die wir haben,
 * relevant ist: Wenn ein koral:term in einem koral:span gewrappt ist, dann
 * kann er eventuell nur einen Schlüssel haben ("s" oder "p" von "<s>" oder
 * "<p>". In diesem Fall wäre der default layer "s" und die default foundry
 * "base". (Im alten KoralQuery wurden spans nicht gewrappt - der Fall
 * sollte aber erstmal weiter unterstützt werden.)
 * Viele Grüße,
 * Nils
 *
 * @author hanl
 * @date 02/07/2015
 */
public class TreeConstraint implements RewriteTask.RewriteNodeAt {

    private String pointer;

    public TreeConstraint() {
        super();
    }

    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {
        System.out.println("FIND PATH " + node.rawNode().findParent(pointer));

        return node.rawNode();
    }

    @Override
    public JsonNode postProcess(KoralNode node) {
        return null;
    }

    @Override
    public String at() {
        return null;
    }
}
