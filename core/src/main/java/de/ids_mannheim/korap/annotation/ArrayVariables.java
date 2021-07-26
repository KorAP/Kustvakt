package de.ids_mannheim.korap.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.ids_mannheim.korap.constant.AnnotationType;
import de.ids_mannheim.korap.entity.Annotation;

/**
 * Helper class to parse annotation scripts variables. It prints out
 * corenlp constituency layer for each negranodes.
 * 
 * @author margaretha
 *
 */
@Deprecated
public class ArrayVariables {

    public static HashMap<String, List<Annotation>> annotationMap =
            new HashMap<>();

    public static void main (String[] args) throws IOException {
        ArrayVariables variables = new ArrayVariables();
        variables.extractVariables();

        List<Annotation> negranodes = annotationMap.get("negranodes");
        for (Annotation n : negranodes) {
            System.out.println("ah[\"corenlp/c=" + n.getCode() + "-\"] = [");
            int i = 1;
            List<Annotation> negraedges = annotationMap.get("negraedges");
            for (Annotation edge : negraedges) {
                System.out.print(
                        "  [\"" + edge.getCode() + "\", \"" + edge.getText()
                                + "\", \"" + edge.getDescription() + "\"]");
                if (i < negraedges.size()) {
                    System.out.println(",");
                }
                else {
                    System.out.println();
                }
                i++;
            }
            System.out.println("];");
            System.out.println();
        }
    }

    public void extractVariables () throws IOException {
        String dir = "annotation-scripts/variables";
        if (dir.isEmpty()) return;

        File d = new File(dir);
        if (!d.isDirectory()) {
            throw new IOException("Directory " + dir + " is not valid");
        }

        for (File file : d.listFiles()) {
            if (!file.exists()) {
                throw new IOException("File " + file + " is not found.");
            }
            readFile(file);
        }

    }

    private void readFile (File file) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)));

        String line;
        ArrayList<String> values;
        List<Annotation> annotations = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("[")) {
                values = AnnotationParser.computeValues(line);

                Annotation annotation = new Annotation(values.get(0),
                        AnnotationType.VALUE, values.get(1), values.get(2));
                annotations.add(annotation);
            }
        }
        br.close();

        String filename = file.getName();
        filename = filename.substring(0, filename.length() - 3);
        annotationMap.put(filename, annotations);
    }
}
