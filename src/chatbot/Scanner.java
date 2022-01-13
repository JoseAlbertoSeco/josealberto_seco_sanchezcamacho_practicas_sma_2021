package chatbot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Scanner {

    private final String expression;

    public Scanner(String expr) {
        this.expression = expr;
    }

    public List<ScannedToken> scan() {
        StringBuilder value = new StringBuilder();
        List<ScannedToken> scannedExpr = new ArrayList<>();
        for (char c : expression.toCharArray()) {
            TokenType type = TokenType.fromString(new String(new char[] {c}));
            if (!type.equals(TokenType.VALUE)) {
                if (value.length() > 0) {
                    //Add the full value TOKEN
                    ScannedToken st = new ScannedToken(value.toString(), TokenType.VALUE);
                    scannedExpr.add(st);
                }
                value = new StringBuilder(new String(new char[] {c}));
                ScannedToken st = new ScannedToken(value.toString(), type);
                scannedExpr.add(st);
                value = new StringBuilder();
            } else {
                value.append(new String(new char[] {c}));

            }
        }
        if (value.length() > 0) {
            //Add the full value TOKEN
            ScannedToken st = new ScannedToken(value.toString(), TokenType.VALUE);
            scannedExpr.add(st);
        }

        return scannedExpr;
    }

    public double evaluate(List<ScannedToken> tokenizedExpression) {

        if (tokenizedExpression.size() == 1) {
            return Double.parseDouble(tokenizedExpression.get(0).expression());
        }
        //Eval order is PEMDAS - Parenthesis, exponents, multiply, divide, add, subtract
        List<ScannedToken> simpleExpr = new ArrayList<>();

        int idx =
            tokenizedExpression.stream()
                .map(ScannedToken::type)
                .collect(Collectors.toList())
                .lastIndexOf(TokenType.LPAR);
        int matchingRPAR = -1;
        if (idx >= 0) {
            for (int i = idx + 1; i < tokenizedExpression.size(); i++) {
                ScannedToken curr = tokenizedExpression.get(i);
                if (curr.type() == TokenType.RPAR) {
                    matchingRPAR = i;
                    break;
                } else {
                    simpleExpr.add(tokenizedExpression.get(i));
                }
            }
        } else {
            simpleExpr.addAll(tokenizedExpression);
            return evaluateSimpleExpression(tokenizedExpression);
        }

        double value = evaluateSimpleExpression(simpleExpr);
     //   System.out.println("val is " + value);
        List<ScannedToken> partiallyEvaluatedExpression = new ArrayList<>();
        for (int i = 0; i < idx; i++) {
            partiallyEvaluatedExpression.add(tokenizedExpression.get(i));
        }
        partiallyEvaluatedExpression.add(new ScannedToken(Double.toString(value), TokenType.VALUE));
        for (int i = matchingRPAR + 1; i < tokenizedExpression.size(); i++) {
            partiallyEvaluatedExpression.add(tokenizedExpression.get(i));
        }

        // from idx find first ), extract, evaluate, replace, call recursively
      //  System.out.println("Expr to eval indexes: " + idx + ", " + matchingRPAR);
        return evaluate(partiallyEvaluatedExpression);
    }

    //A simple expression won't contain parenthesis
    public double evaluateSimpleExpression(List<ScannedToken> expression) {
        if (expression.size() == 1) {
            return Double.parseDouble(expression.get(0).expression());
        } else {
            List<ScannedToken> newExpression = new ArrayList<>();
            int idx = expression.stream().map(ScannedToken::type).collect(Collectors.toList()).indexOf(TokenType.POW);
            if (idx != -1) {
                double base = Double.parseDouble(expression.get(idx - 1).expression());
                double exp = Double.parseDouble(expression.get(idx + 1).expression());
                DecimalFormat df = new DecimalFormat(".00");
                double ans = Math.pow(base, exp);
                for (int i = 0; i < idx - 1; i++) {
                    newExpression.add(expression.get(i));
                }
                newExpression.add(new ScannedToken(ans + "", TokenType.VALUE));
                for (int i = idx + 2; i < expression.size(); i++) {
                    newExpression.add(expression.get(i));
                }
                return evaluateSimpleExpression(newExpression);
            } else {
                int mulIdx = expression.stream()
                    .map(ScannedToken::type)
                    .collect(Collectors.toList())
                    .indexOf(TokenType.MUL);
                int divIdx = expression.stream()
                    .map(ScannedToken::type)
                    .collect(Collectors.toList())
                    .indexOf(TokenType.DIV);
                int computationIdx = (mulIdx >= 0 && divIdx >= 0) ? Math.min(mulIdx, divIdx) : Math.max(mulIdx, divIdx);
                if (computationIdx != -1) {
                    double left = Double.parseDouble(expression.get(computationIdx - 1).expression());
                    double right = Double.parseDouble(expression.get(computationIdx + 1).expression());
                    DecimalFormat df = new DecimalFormat(".00");
                    double ans = computationIdx == mulIdx ? left * right : left / right * 1.0;
                    for (int i = 0; i < computationIdx - 1; i++) {
                        newExpression.add(expression.get(i));
                    }
                    newExpression.add(new ScannedToken(ans + "", TokenType.VALUE));
                    for (int i = computationIdx + 2; i < expression.size(); i++) {
                        newExpression.add(expression.get(i));
                    }
                    return evaluateSimpleExpression(newExpression);
                } else {
                    int addIdx = expression.stream()
                        .map(e -> e.type())
                        .collect(Collectors.toList())
                        .indexOf(TokenType.ADD);
                    int subIdx = expression.stream()
                        .map(e -> e.type())
                        .collect(Collectors.toList())
                        .indexOf(TokenType.SUB);
                    int computationIdx2 = (addIdx >= 0 && subIdx >= 0) ?
                        Math.min(addIdx, subIdx) :
                        Math.max(addIdx, subIdx);
                    if (computationIdx2 != -1) {
                        double left = Double.parseDouble(expression.get(computationIdx2 - 1).expression());
                        double right = Double.parseDouble(expression.get(computationIdx2 + 1).expression());
                        DecimalFormat df = new DecimalFormat(".00");
                        double ans = computationIdx2 == addIdx ? left + right : (left - right) * 1.0;
                        for (int i = 0; i < computationIdx2 - 1; i++) {
                            newExpression.add(expression.get(i));
                        }
                        newExpression.add(new ScannedToken(ans + "", TokenType.VALUE));
                        for (int i = computationIdx2 + 2; i < expression.size(); i++) {
                            newExpression.add(expression.get(i));
                        }
                        return evaluateSimpleExpression(newExpression);
                    }
                }

            }
        }
        return -1.0;
    }
}