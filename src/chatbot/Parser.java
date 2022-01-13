package chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
public class Parser {

    private final List<ScannedToken> expression;

    public Parser(List<ScannedToken> expression) {
        this.expression = expression;
    }

    /*
    We need a triple in a sliding window fashion where middle is current token so we can contextualize what
    needs to be parsed into correct tokens
     */
    public List<ScannedToken> parse() {
        TokenType prev = null;
        TokenType curr = null;
        TokenType next = null;

        List<ScannedToken> properlyParsedExpression = new ArrayList<>();

        List<TokenType> types = expression.stream().map(ScannedToken::type).collect(Collectors.toList());
        List<Integer> indexes = new ArrayList<>();
        List<ScannedToken> negativeValues = new ArrayList<>();

        for (int i = 0; i < types.size() - 1; i++) {
            prev = i == 0 ? null : types.get(i - 1);
            curr = types.get(i);
            next = types.get(i + 1);
            if (prev == null && curr == TokenType.SUB && next == TokenType.VALUE) {
                ScannedToken negativeValue =
                    new ScannedToken("" + (-1 * Double.parseDouble(expression.get(i + 1).expression())),
                        TokenType.VALUE);
                indexes.add(i);
                negativeValues.add(negativeValue);

            } else if (prev == TokenType.LPAR && curr == TokenType.SUB && next == TokenType.VALUE) {
                ScannedToken negativeValue =
                    new ScannedToken("" + (-1 * Double.parseDouble(expression.get(i + 1).expression())),
                        TokenType.VALUE);
                indexes.add(i);
                negativeValues.add(negativeValue);
            }
        }

        int maxIterations = expression.size();
        int i = 0;
        int j = 0;
        while (i < maxIterations) {
            if (indexes.contains(i) && j < negativeValues.size()) {
                properlyParsedExpression.add(negativeValues.get(j));
                j++;
                i++;
            }
            else {
                properlyParsedExpression.add(expression.get(i));
            }
            i++;
        }
        return properlyParsedExpression;
    }

}