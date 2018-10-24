package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.resource.rewrite.CollectionCleanRewrite;
import de.ids_mannheim.korap.resource.rewrite.DocMatchRewrite;
// import de.ids_mannheim.korap.resource.rewrite.IdWriter;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;

/**
 * Defines rewrite handling methods relevant only in full version.
 * 
 * @author margaretha
 *
 */
public class FullRewriteHandler extends RewriteHandler {

    @Autowired
    private VirtualCorpusRewrite vcRewrite;
    
    public FullRewriteHandler (FullConfiguration config) {
        super(config);
    }

    public void defaultRewriteConstraints () {
        super.defaultRewriteConstraints();
        this.addProcessor(vcRewrite);
        this.add(CollectionRewrite.class);
        // this.add(IdWriter.class);
        this.add(DocMatchRewrite.class);
        this.add(CollectionCleanRewrite.class);
    }
}
