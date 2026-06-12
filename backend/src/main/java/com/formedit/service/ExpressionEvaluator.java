package com.formedit.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ExpressionEvaluator {

    public static boolean evaluate(String expression, Map<String, Object> context) {
        String substituted = substituteVariables(expression, context);
        List<Token> tokens = tokenize(substituted);
        Parser parser = new Parser(tokens);
        boolean result = parser.parseOr();
        if (parser.hasMore()) {
            throw new IllegalArgumentException("表达式存在多余内容，位置 " + parser.pos + ": "
                    + tokens.subList(parser.pos, tokens.size()));
        }
        return result;
    }

    public static void validateSyntax(String expression) {
        if (expression == null) return;
        String expr = expression.trim();
        if (expr.isEmpty()) return;
        List<Token> tokens = tokenize(expr);
        Parser parser = new Parser(tokens);
        parser.parseOr();
        if (parser.hasMore()) {
            throw new IllegalArgumentException("表达式存在多余内容，位置 " + parser.pos + ": "
                    + tokens.subList(parser.pos, tokens.size()));
        }
    }

    private static String substituteVariables(String expression, Map<String, Object> context) {
        String expr = expression;
        List<String> sortedKeys = new ArrayList<>(context.keySet());
        sortedKeys.sort(Comparator.comparingInt(String::length).reversed());

        for (String key : sortedKeys) {
            Object rawValue = context.get(key);
            String value = rawValue != null ? rawValue.toString() : "";
            String escaped = value.replace("\\", "\\\\").replace("$", "\\$");
            expr = expr.replace("${" + key + "}", escaped);
            expr = expr.replace("{" + key + "}", escaped);
        }

        for (String key : sortedKeys) {
            Object rawValue = context.get(key);
            String value = rawValue != null ? rawValue.toString() : "";
            String escaped = value.replace("\\", "\\\\").replace("$", "\\$");
            expr = expr.replaceAll("\\b" + Pattern.quote(key) + "\\b", escaped);
        }

        return expr;
    }

    public enum TokenType { LPAREN, RPAREN, AND, OR, GT, GTE, LT, LTE, EQ, NEQ, STRING, NUMBER, IDENT, TRUE, FALSE }

    public static class Token {
        public final TokenType type;
        public final String value;
        public Token(TokenType type, String value) { this.type = type; this.value = value; }
        @Override public String toString() { return type + "(" + value + ")"; }
    }

    public static List<Token> tokenize(String expr) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = expr.length();
        while (i < len) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { tokens.add(new Token(TokenType.LPAREN, "(")); i++; continue; }
            if (c == ')') { tokens.add(new Token(TokenType.RPAREN, ")")); i++; continue; }
            if (c == '&' && i + 1 < len && expr.charAt(i + 1) == '&') {
                tokens.add(new Token(TokenType.AND, "&&")); i += 2; continue;
            }
            if (c == '|' && i + 1 < len && expr.charAt(i + 1) == '|') {
                tokens.add(new Token(TokenType.OR, "||")); i += 2; continue;
            }
            if (c == '>' && i + 1 < len && expr.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.GTE, ">=")); i += 2; continue;
            }
            if (c == '>' ) { tokens.add(new Token(TokenType.GT, ">")); i++; continue; }
            if (c == '<' && i + 1 < len && expr.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.LTE, "<=")); i += 2; continue;
            }
            if (c == '<') { tokens.add(new Token(TokenType.LT, "<")); i++; continue; }
            if (c == '=' && i + 1 < len && expr.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.EQ, "==")); i += 2; continue;
            }
            if (c == '!' && i + 1 < len && expr.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.NEQ, "!=")); i += 2; continue;
            }
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = i;
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < len && expr.charAt(i) != quote) {
                    if (expr.charAt(i) == '\\' && i + 1 < len) {
                        char next = expr.charAt(i + 1);
                        if (next == 'n') sb.append('\n');
                        else if (next == 't') sb.append('\t');
                        else sb.append(next);
                        i += 2;
                    } else {
                        sb.append(expr.charAt(i));
                        i++;
                    }
                }
                if (i >= len) {
                    throw new IllegalArgumentException("字符串未闭合: " + expr.substring(start));
                }
                i++;
                tokens.add(new Token(TokenType.STRING, sb.toString()));
                continue;
            }
            if (Character.isDigit(c) || (c == '-' && i + 1 < len && Character.isDigit(expr.charAt(i + 1)))) {
                int start = i;
                if (c == '-') i++;
                while (i < len && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                tokens.add(new Token(TokenType.NUMBER, expr.substring(start, i)));
                continue;
            }
            if (Character.isLetter(c) || c == '_' || c == '$') {
                int start = i;
                while (i < len && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_'
                        || expr.charAt(i) == '$' || expr.charAt(i) == '-' || expr.charAt(i) == '.')) i++;
                String word = expr.substring(start, i);
                if ("true".equalsIgnoreCase(word)) { tokens.add(new Token(TokenType.TRUE, word)); }
                else if ("false".equalsIgnoreCase(word)) { tokens.add(new Token(TokenType.FALSE, word)); }
                else { tokens.add(new Token(TokenType.IDENT, word)); }
                continue;
            }
            throw new IllegalArgumentException("无法识别的字符 '" + c + "' 在位置 " + i + "，表达式: " + expr);
        }
        return tokens;
    }

    public static class Parser {
        public final List<Token> tokens;
        public int pos;
        public Parser(List<Token> tokens) { this.tokens = tokens; this.pos = 0; }

        public boolean hasMore() { return pos < tokens.size(); }

        public Token peek() { return pos < tokens.size() ? tokens.get(pos) : null; }
        public Token consume() { return tokens.get(pos++); }
        public Token expect(TokenType type, String msg) {
            Token t = peek();
            if (t == null || t.type != type) {
                throw new IllegalArgumentException(msg + "，当前位置 " + pos
                        + " 的 Token: " + (t != null ? t : "<EOF>"));
            }
            return consume();
        }

        public boolean parseOr() {
            boolean left = parseAnd();
            while (peek() != null && peek().type == TokenType.OR) {
                consume();
                boolean right = parseAnd();
                left = left || right;
            }
            return left;
        }

        public boolean parseAnd() {
            boolean left = parsePrimary();
            while (peek() != null && peek().type == TokenType.AND) {
                consume();
                boolean right = parsePrimary();
                left = left && right;
            }
            return left;
        }

        public boolean parsePrimary() {
            Token t = peek();
            if (t == null) {
                throw new IllegalArgumentException("表达式意外结束");
            }
            if (t.type == TokenType.LPAREN) {
                consume();
                boolean v = parseOr();
                expect(TokenType.RPAREN, "缺少右括号 ')'");
                return v;
            }
            if (t.type == TokenType.TRUE) { consume(); return true; }
            if (t.type == TokenType.FALSE) { consume(); return false; }
            return parseComparison();
        }

        public Object parseOperand() {
            Token t = peek();
            if (t == null) {
                throw new IllegalArgumentException("缺少操作数");
            }
            if (t.type == TokenType.NUMBER) {
                consume();
                try {
                    if (t.value.contains(".")) {
                        return Double.parseDouble(t.value);
                    }
                    try {
                        return Long.parseLong(t.value);
                    } catch (NumberFormatException e) {
                        return Double.parseDouble(t.value);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无效数字: " + t.value);
                }
            }
            if (t.type == TokenType.STRING) {
                consume();
                return t.value;
            }
            if (t.type == TokenType.IDENT) {
                consume();
                if ("true".equalsIgnoreCase(t.value)) return true;
                if ("false".equalsIgnoreCase(t.value)) return false;
                return t.value;
            }
            throw new IllegalArgumentException("期望操作数，但遇到: " + t);
        }

        public int compareValues(Object a, Object b) {
            if (a instanceof Number && b instanceof Number) {
                double da = ((Number) a).doubleValue();
                double db = ((Number) b).doubleValue();
                return Double.compare(da, db);
            }
            String sa = String.valueOf(a);
            String sb = String.valueOf(b);
            try {
                double da = Double.parseDouble(sa);
                double db = Double.parseDouble(sb);
                return Double.compare(da, db);
            } catch (NumberFormatException e) {
                return sa.compareTo(sb);
            }
        }

        public boolean parseComparison() {
            Object left = parseOperand();
            Token op = peek();
            if (op == null) {
                if (left instanceof Boolean) return (Boolean) left;
                if (left instanceof Number) return ((Number) left).doubleValue() != 0;
                String s = String.valueOf(left);
                if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)
                        || "是".equals(s) || "批准".equals(s)) return true;
                if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)
                        || "否".equals(s) || "拒绝".equals(s)) return false;
                throw new IllegalArgumentException("无法作为布尔值: '" + left
                        + "'，请使用比较运算符（如 > < == !=）");
            }
            TokenType opType = op.type;
            if (opType != TokenType.GT && opType != TokenType.GTE && opType != TokenType.LT
                    && opType != TokenType.LTE && opType != TokenType.EQ && opType != TokenType.NEQ) {
                if (left instanceof Boolean) return (Boolean) left;
                if (left instanceof Number) return ((Number) left).doubleValue() != 0;
                String s = String.valueOf(left);
                if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)
                        || "是".equals(s) || "批准".equals(s)) return true;
                if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)
                        || "否".equals(s) || "拒绝".equals(s)) return false;
                throw new IllegalArgumentException("无效的表达式结构，缺少运算符，当前 Token: " + op);
            }
            consume();
            Object right = parseOperand();
            switch (opType) {
                case GT:  return compareValues(left, right) > 0;
                case GTE: return compareValues(left, right) >= 0;
                case LT:  return compareValues(left, right) < 0;
                case LTE: return compareValues(left, right) <= 0;
                case EQ:
                    if (left == null) return right == null;
                    if (left instanceof Number && right instanceof Number) {
                        return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue()) == 0;
                    }
                    return String.valueOf(left).equals(String.valueOf(right));
                case NEQ:
                    if (left == null) return right != null;
                    if (left instanceof Number && right instanceof Number) {
                        return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue()) != 0;
                    }
                    return !String.valueOf(left).equals(String.valueOf(right));
                default:
                    throw new IllegalArgumentException("不支持的运算符: " + op);
            }
        }
    }
}
