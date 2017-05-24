package de.ids_mannheim.korap.resource.rewrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resource.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * @author margaretha
 * @date 22 May 2017
 */
public class CollectionRewrite implements RewriteTask.RewriteQuery {

	private static Logger jlog = LoggerFactory.getLogger(CollectionRewrite.class);

	public CollectionRewrite() {
		super();
	}

	@Override
	public JsonNode rewriteQuery(KoralNode node, KustvaktConfiguration config, User user) throws KustvaktException {
		JsonNode subnode = node.rawNode();
		KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
		if (subnode.at("/collection").isMissingNode()) {
			if (subnode.has("collection")) {
				builder.setBaseQuery(JsonUtils.toJSON(subnode));
			}
			// EM 
			// fix me: later store the collection queries as KoralQuery in the database
			switch (user.getCorpusAccess()) {
			case PUBLIC:
				builder = new KoralCollectionQueryBuilder();
				builder.with("availability = /CC-BY.*/ | availablity = /ACA.*/");
				break;

			case ALL:
				builder = new KoralCollectionQueryBuilder();
				builder.with("availability = /QAO.*/ | availablity = /ACA.*/ |  availablity = /CC-BY.*/");
				break;

			default: // FREE
				builder = new KoralCollectionQueryBuilder();
				builder.with("availability	 = /CC-BY.*/");
				break;
			}

			JsonNode rewritten = JsonUtils.readTree(builder.toJSON()).at("/collection");
			RewriteIdentifier identifier = new KoralNode.RewriteIdentifier(Attributes.AVAILABILITY,
					rewritten.at("/value"));
			node.set("collection", rewritten, identifier);
			jlog.debug(node.at("/collection").toString());
		}

		return node.rawNode();
	}
}
