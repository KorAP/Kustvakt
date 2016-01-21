package de.ids_mannheim.korap.utils;

/**
 * @author hanl
 * @date 26/11/2015
 */
public class SqlBuilder {

    private StringBuffer buffer;
    private String table;
    private String[] fields;
    private String where;

    public SqlBuilder(String table) {
        this.buffer = new StringBuffer();
        this.table = table;
    }

    public SqlBuilder select(String... fields) {
        this.buffer.append("SELECT ");
        if (fields.length > 0) {
            for (int i = 0; i < fields.length; i++) {
                if (i > 0)
                    this.buffer.append(", ");
                this.buffer.append(fields[i]);
            }
        }else
            this.buffer.append("*");
        this.buffer.append(" FROM ").append(table);
        return this;
    }

    public SqlBuilder update(String... fields) {
        this.buffer.append("UPDATE ").append(table);
        this.fields = fields;
        return this;
    }

    public SqlBuilder insert(String... fields) {
        this.buffer.append("INSERT INTO ").append(table);
        this.fields = fields;
        return this;
    }

    public SqlBuilder delete() {
        this.buffer.append("DELETE FROM ").append(table);
        return this;
    }

    public SqlBuilder params(String... values) {
        if (values.length != fields.length)
            return this;
        if (this.buffer.lastIndexOf("INSERT INTO") != -1) {
            this.buffer.append(" (");
            for (int i = 0; i < this.fields.length; i++) {
                if (i > 0)
                    this.buffer.append(", ");
                this.buffer.append(fields[i]);
            }

            StringBuffer b = new StringBuffer();
            for (int i = 0; i < values.length; i++) {
                if (i > 0)
                    b.append(", ");
                b.append(values[i]);
            }
            this.buffer.append(") VALUES (").append(b.toString()).append(")");
        }
        if (this.buffer.lastIndexOf("UPDATE") != -1) {
            this.buffer.append(" SET ");
            for (int i = 0; i < this.fields.length; i++) {
                if (i > 0)
                    this.buffer.append(", ");
                this.buffer.append(fields[i]).append("=").append(values[i]);
            }
        }
        return this;
    }

    public SqlBuilder where(String where) {
        this.where = where;
        return this;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer(this.buffer);
        //exclude where clauses from insert statements
        if (this.where != null && this.buffer.lastIndexOf("INSERT INTO") == -1)
            b.append(" WHERE ").append(where);
        return b.append(";").toString();
    }

}
