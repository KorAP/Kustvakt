import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.utils.SqlBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author hanl
 * @date 26/11/2015
 */
public class TestUtil {

    @Test
    public void testSqlBuilderSELECT () {
        SqlBuilder b = new SqlBuilder("users");
        b.select(Attributes.USERNAME, Attributes.PASSWORD).where(
                Attributes.USERNAME + "=?");
        Assert.assertEquals("query does not match",
                "SELECT username, password FROM users WHERE username=?;",
                b.toString());
    }


    @Test
    public void testSqlBuilderINSERT () {
        SqlBuilder b = new SqlBuilder("users");
        b.insert(Attributes.USERNAME, Attributes.PASSWORD).params("user",
                "pass");
        Assert.assertEquals("query does not match",
                "INSERT INTO users (username, password) VALUES (user, pass);",
                b.toString());
    }


    @Test
    public void testSqlBuilderINSERTExcludeWhere () {
        SqlBuilder b = new SqlBuilder("users");
        b.insert(Attributes.USERNAME, Attributes.PASSWORD)
                .params("user", "pass").where("some=?");
        Assert.assertEquals("query does not match",
                "INSERT INTO users (username, password) VALUES (user, pass);",
                b.toString());
    }


    @Test
    public void testSqlBuilderDELETE () {
        SqlBuilder b = new SqlBuilder("users");
        b.delete().where(Attributes.PERSISTENT_ID + "=?");
        Assert.assertEquals("query does not match",
                "DELETE FROM users WHERE persistent_id=?;", b.toString());
    }


    @Test
    public void testSqlBuilderUPDATE () {
        SqlBuilder b = new SqlBuilder("users");
        b.update(Attributes.USERNAME, Attributes.PASSWORD).params("user",
                "pass");
        Assert.assertEquals("query does not match",
                "UPDATE users SET username=user, password=pass;", b.toString());
    }
}
