package de.ids_mannheim.korap.rewrite;

public class RewriteIdentifier {

    private String scope,comment = "";
    private Object source;

    public RewriteIdentifier (String scope, String value, String comment) {
        this.scope = scope;
        this.source = value;
        this.comment = comment;
    }
    
	public RewriteIdentifier (String scope, Object source, String comment) {
		this.scope = scope;
		this.source = source;
		this.comment = comment;
	}
    
    public String getScope () {
        return scope;
    }
    
    public Object getSource () {
        return source;
    }
    
    public String getComment () {
		return comment;
	}
    
//    @Override
//    public String toString () {
//        return scope + "(" + value + ")";
//    }
}

