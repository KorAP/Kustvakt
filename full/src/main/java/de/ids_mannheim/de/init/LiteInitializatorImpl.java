package de.ids_mannheim.de.init;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.annotation.AnnotationParser;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;

public class LiteInitializatorImpl implements Initializator {

    @Autowired
    private AnnotationParser annotationParser;
    
    @Override
    public void init () throws IOException, QueryException, KustvaktException {
        annotationParser.run();
    }

    @Override
    public void initTest () throws IOException, KustvaktException {
        annotationParser.run();
    }

}
