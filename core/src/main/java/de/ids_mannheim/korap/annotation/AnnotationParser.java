package de.ids_mannheim.korap.annotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.constant.AnnotationType;
import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.entity.Annotation;
import de.ids_mannheim.korap.entity.AnnotationKey;
import de.ids_mannheim.korap.entity.AnnotationLayer;

@Component
public class AnnotationParser {

    private Logger log = LogManager.getLogger(AnnotationDao.class);

    public static final Pattern quotePattern = Pattern.compile("\"([^\"]*)\"");

    @Autowired
    private AnnotationDao annotationDao;

    private Annotation foundry = null;
    private AnnotationLayer layer = null;
    private AnnotationKey key = null;

    private Set<AnnotationKey> keys = new HashSet<>();
    private Set<Annotation> values = new HashSet<>();

    public void run () throws IOException {
        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(
                        getClass().getClassLoader());
        Resource[] resources = resolver
                .getResources("classpath:annotation-scripts/foundries/*.js");

        if (resources.length < 1) return;

        for (Resource r : resources) {
//            log.debug(r.getFilename());
            readFile(r.getInputStream());
        }
    }

    private void readFile (InputStream inputStream) throws IOException {
        BufferedReader br =
                new BufferedReader(new InputStreamReader(inputStream), 1024);

        foundry = null;

        String line, annotationCode = "", annotationType = "";
        Matcher m;
        ArrayList<String> array;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("ah")) {
                m = quotePattern.matcher(line);
                if (m.find()) {
                    annotationCode = m.group(1);
                    annotationType = computeAnnotationType(annotationCode);
                }
                m.reset();
            }
            else if (line.startsWith("];")) {
                if (!keys.isEmpty()) {
                    layer.setKeys(keys);
                    annotationDao.updateAnnotationLayer(layer);
                }
                if (!values.isEmpty()) {
                    key.setValues(values);
                    annotationDao.updateAnnotationKey(key);
                }
                keys.clear();
                values.clear();
                layer = null;
                key = null;
            }
            else if (line.startsWith("[")) {
                array = computeValues(line);
                parseArray(annotationCode, annotationType, array);
            }

        }
        br.close();
    }

    public static ArrayList<String> computeValues (String line) {
        ArrayList<String> values;
        Matcher m = quotePattern.matcher(line);
        values = new ArrayList<String>();
        while (m.find()) {
            values.add(m.group(1));
        }
        return values;
    }

    private void parseArray (String annotationCode, String annotationType,
            ArrayList<String> array) {
        if (annotationType.equals(AnnotationType.FOUNDRY)) {
            String code = array.get(1).substring(0, array.get(1).length() - 1);
            foundry = retrieveOrCreateAnnotation(code, AnnotationType.FOUNDRY,
                    null, array.get(0));
        }
        else if (annotationType.equals(AnnotationType.LAYER)) {
            String code = array.get(1);
            if (code.endsWith("=")) {
                code = code.substring(0, code.length() - 1);
            }
            Annotation layer = retrieveOrCreateAnnotation(code, annotationType,
                    null, array.get(0));
            try {
                AnnotationLayer annotationLayer =
                        annotationDao.retrieveAnnotationLayer(foundry.getCode(),
                                layer.getCode());
                if (annotationLayer == null) {
                    annotationDao.createAnnotationLayer(foundry, layer);
                }
            }
            catch (Exception e) {
                log.debug("Duplicate annotation layer: " + foundry.getCode()
                        + "/" + layer.getCode());
            }
        }
        else if (annotationType.equals(AnnotationType.KEY))

        {
            if (layer == null) {
                computeLayer(annotationCode);
            }

            Annotation annotation = null;
            if (array.size() == 2) {
                String code = array.get(1);
                if (code.endsWith("=") || code.endsWith(":")) {
                    code = code.substring(0, code.length() - 1);
                }
                annotation = retrieveOrCreateAnnotation(code, annotationType,
                        null, array.get(0));
            }
            else if (array.size() == 3) {
                annotation = retrieveOrCreateAnnotation(array.get(0),
                        annotationType, array.get(1), array.get(2));
            }
            if (annotation != null) {
                AnnotationKey annotationKey =
                        annotationDao.retrieveAnnotationKey(layer, annotation);
                if (annotationKey == null) {
                    annotationDao.createAnnotationKey(layer, annotation);
                }
                this.keys.add(annotationKey);
            }
        }
        else if (annotationType.equals(AnnotationType.VALUE)) {
            if (this.key == null) {
                computeKey(annotationCode);
            }
            Annotation value = retrieveOrCreateAnnotation(array.get(0),
                    AnnotationType.VALUE, array.get(1), array.get(2));
            if (value != null) {
                values.add(value);
            }
        }
    }

    private void computeKey (String code) {
        String[] codes = code.split("=");
        if (codes.length > 1) {
            computeLayer(codes[0]);
            String keyCode = codes[1];
            if (keyCode.endsWith(":") || keyCode.endsWith("-")) {
                keyCode = keyCode.substring(0, keyCode.length() - 1);
            }
            Annotation key = annotationDao.retrieveAnnotation(keyCode,
                    AnnotationType.KEY);
            this.key = annotationDao.retrieveAnnotationKey(layer, key);
        }

    }

    private void computeLayer (String code) {
        String[] codes = code.split("/");
        if (codes.length > 1) {
            String layerCode = codes[1];
            if (layerCode.endsWith("=")) {
                layerCode = layerCode.substring(0, layerCode.length() - 1);
            }
            this.layer =
                    annotationDao.retrieveAnnotationLayer(codes[0], layerCode);
            if (layer == null) {
                log.warn("Layer is null for " + code);
            }
        }
    }

    private Annotation retrieveOrCreateAnnotation (String code, String type,
            String text, String description) {
        Annotation annotation = annotationDao.retrieveAnnotation(code, type);
        if (annotation == null) {
            annotation = annotationDao.createAnnotation(code, type, text,
                    description);
        }
        return annotation;
    }

    private String computeAnnotationType (String code) {
        String[] codes = code.split("/");
        if (codes.length == 1) {
            if (codes[0].equals("-")) {
                return AnnotationType.FOUNDRY;
            }
            return AnnotationType.LAYER;
        }
        else if (codes.length == 2) {
            if (codes[1].endsWith(":") || codes[1].endsWith("-")) {
                return AnnotationType.VALUE;
            }
            else {
                return AnnotationType.KEY;
            }
        }

        return "unknown";
    }

}
