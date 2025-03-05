package de.ids_mannheim.korap.rewrite;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.utils.JsonUtils;

public class KoralRewriteBuilder {

	private List<KoralRewrite> rewrites;

	public KoralRewriteBuilder () {
		this.rewrites = new ArrayList<>();
	}

	@Deprecated
	public KoralRewriteBuilder add (String op, Object scope) {
		KoralRewrite rewrite = new KoralRewrite();
		rewrite.setOperation(op);
		if (scope != null) {
			rewrite.setScope(scope.toString());
		}
		this.rewrites.add(rewrite);
		return this;
	}

	public KoralRewriteBuilder add (String op, RewriteIdentifier ri) {
		KoralRewrite rewrite = new KoralRewrite();
		rewrite.setOperation(op);
		if (ri.getScope() != null) {
			rewrite.setScope(ri.getScope());
		}
		if (ri.getOriginal() != null) {
			rewrite.setOriginal(ri.getOriginal());
		}
		if (ri.getComment() != null) {
			rewrite.setComment(ri.getComment());
		}
		this.rewrites.add(rewrite);
		return this;
	}

	public JsonNode build (JsonNode node) {
		for (KoralRewrite rewrite : this.rewrites) {
			if (rewrite.map.get("operation") == null)
				throw new UnsupportedOperationException(
						"operation not set properly");

			if (node.has("rewrites")) {
				ArrayNode n = (ArrayNode) node.path("rewrites");
				n.add(JsonUtils.valueToTree(rewrite.map));
			}
			else if (node.isObject()) {
				ObjectNode n = (ObjectNode) node;
				List l = new LinkedList<>();
				l.add(JsonUtils.valueToTree(rewrite.map));
				n.put("rewrites", JsonUtils.valueToTree(l));
			}
			else {
				//fixme: matches in result will land here. rewrites need to be placed under root node - though then there might be unclear where they belong to
			}

		}
		this.rewrites.clear();
		return node;
	}

}
