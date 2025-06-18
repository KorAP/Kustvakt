package de.ids_mannheim.korap.rewrite;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;

public class TimeoutRewrite implements RewriteTask.RewriteQuery {

	@Override
	public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
			User user) throws KustvaktException {
		CorpusAccess access = user.getCorpusAccess();
		if (node.has("meta")) {
            node = node.at("/meta");
            int timeout = (access.equals(CorpusAccess.FREE)) ?
				config.getGuestTimeout() : config.getLoginTimeout();
            
			if (node.has("timeout")) {
				RewriteIdentifier id = new RewriteIdentifier("timeout",
						node.get("timeout"), "Timeout has been replaced. "
						+ "The original value is described in the original "
						+ "property.");
				node.replace("timeout", timeout, id);
			}
            else {
            	RewriteIdentifier id = new RewriteIdentifier("timeout", null, 
            			"Timeout has been added.");
            	node.set("timeout", timeout, id);
            }
        }
		return node;
	}

}
