package de.ids_mannheim.de.init;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.config.NamedVCLoader;

public class VCLoaderImpl implements VCLoader{

    @Autowired
    private NamedVCLoader vcLoader;
    
    @Override
    public void recachePredefinedVC () {
//        KrillCollection.cache.removeAll();
        Thread t = new Thread(vcLoader);
        t.start();
    }
}
