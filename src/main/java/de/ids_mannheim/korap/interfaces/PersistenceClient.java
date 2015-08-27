package de.ids_mannheim.korap.interfaces;

import lombok.Getter;

import java.io.*;

@Getter
public abstract class PersistenceClient<SOURCE> {

    private SOURCE source;
    private TYPE type;
    @Deprecated
    protected String database;
    private InputStream schema;

    public PersistenceClient(String database, TYPE type) {
        this.type = type;
        this.database = database;
    }

    public PersistenceClient(TYPE type) {
        this.type = type;
    }

    public PersistenceClient() {
    }

    protected void setSource(SOURCE conn) {
        this.source = conn;
    }

    public void setDatabase(String name) {
        this.database = name;
    }

    public void setSchema(String schema_path) throws FileNotFoundException {
        this.schema = new FileInputStream(new File(schema_path));
    }

    // for spring configuration
    @Deprecated
    public void setSchema(InputStream schema) throws IOException {
        this.schema = schema;
    }

    public abstract boolean checkDatabase() throws Exception;

    public abstract void createDatabase() throws IOException;

    public enum TYPE {
        SQL, CASSANDRA
    }
}
