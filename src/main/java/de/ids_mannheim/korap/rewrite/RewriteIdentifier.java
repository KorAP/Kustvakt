package de.ids_mannheim.korap.rewrite;

public class RewriteIdentifier {

    private String scope,comment = "";
    private Object original;

    public RewriteIdentifier (String scope, String value, String comment) {
        this.scope = scope;
        this.original = value;
        this.comment = comment;
    }
    
	public RewriteIdentifier (String scope, Object source, String comment) {
		this.scope = scope;
		this.original = source;
		this.comment = comment;
	}
    
    public String getScope () {
        return scope;
    }
    
    public Object getOriginal () {
        return original;
    }
    
    public String getComment () {
		return comment;
	}
    
//    @Override
//    public String toString () {
//        return scope + "(" + value + ")";
//    }
}

