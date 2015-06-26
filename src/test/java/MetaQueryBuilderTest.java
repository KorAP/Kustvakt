import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hanl
 * @date 26/06/2015
 */
public class MetaQueryBuilderTest {

    private static Pattern p = Pattern
            .compile("^/\\s*\\d+-(?:c(?:hars?)?|t(?:okens)?)$/");

    private static Pattern p3 = Pattern
            .compile("\\d+-(c(?:hars?)?|t(?:okens)?)?");

    @Test
    public void testRegex() {
        Matcher m = p.matcher("1-tokens,2-chars");
        while (m.find()) {
            int size = m.groupCount();
            System.out.println("FOUND ");
            for (int i = 0; i < size; i++)
                System.out.println("GROUP " + m.group(i));
        }
    }

    @Test
    public void testMetaBuilder() {
        Set<String> f = new HashSet<>();
        f.add("docID");
        f.add("corpusID");
        MetaQueryBuilder m = new MetaQueryBuilder();
        m.addEntry("fields", f);
        JsonNode n = JsonUtils.readTree(JsonUtils.toJSON(m.raw()));
        Assert.assertEquals("[\"docID\",\"corpusID\"]", n.at("/fields").toString());
    }
}
