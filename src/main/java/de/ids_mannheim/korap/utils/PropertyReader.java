package de.ids_mannheim.korap.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author hanl
 * @date 27/09/2014
 */
public abstract class PropertyReader {


    protected Map<String, Properties> read(String path) throws IOException {
        Map<String, Properties> res = new HashMap<>();
        Properties s = new Properties();
        s.load(new FileInputStream(new File(path)));
        for (Map.Entry<Object, Object> e : s.entrySet()) {
            String key = e.getKey().toString().split("\\.")[0];
            Properties in = res.get(key);
            if (in == null) {
                in = new Properties();
                res.put(key, in);
            }
            in.setProperty(e.getKey().toString(), e.getValue().toString());
        }
        return res;
    }


    public abstract void load();

}
