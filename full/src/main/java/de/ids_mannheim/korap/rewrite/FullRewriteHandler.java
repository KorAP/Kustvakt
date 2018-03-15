package de.ids_mannheim.korap.rewrite;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.resource.rewrite.CollectionCleanRewrite;
import de.ids_mannheim.korap.resource.rewrite.DocMatchRewrite;
import de.ids_mannheim.korap.resource.rewrite.IdWriter;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;

/** Defines rewrite handling methods relevant only in full version. 
 * 
 * @author margaretha
 *
 */
public class FullRewriteHandler extends RewriteHandler{

    public FullRewriteHandler (FullConfiguration config) {
        super(config);
    }
    
    public void defaultRewriteConstraints () {
      this.add(CollectionRewrite.class);
      this.add(IdWriter.class);
      this.add(DocMatchRewrite.class);
      this.add(CollectionCleanRewrite.class);
  }
}
