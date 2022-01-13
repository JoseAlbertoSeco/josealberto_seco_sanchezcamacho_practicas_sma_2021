package chatbot;

public class ScannedToken {
	  private String expressionPiece;
	  private TokenType type;

	  public ScannedToken(String exp, TokenType type){
	    this.expressionPiece = exp;
	    this.type = type;
	  }

	  @Override
	  public String toString(){
	    return "(Expr:"+ expressionPiece+ ", Token:"+ type+")";
	  }

	  public TokenType type(){
	    return type;
	  }

	  public String expression(){
	    return expressionPiece;
	  }

	}