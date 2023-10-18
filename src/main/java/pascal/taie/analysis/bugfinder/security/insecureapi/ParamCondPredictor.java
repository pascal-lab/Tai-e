/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.bugfinder.security.insecureapi;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.collection.Maps;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParamCondPredictor {

    private static final Map<String, ExprTreeNode> exprMap = Maps.newMap();

    public static boolean test(List<Var> varList, String expr) {
        ExprTreeNode exprTree = exprMap.get(expr);
        if(exprTree == null) {
            exprTree = ExprParser.parse(expr, ExprLexer.scan(expr));
            exprMap.put(expr, exprTree);
        }
        return eval(varList, exprTree);
    }

    private static boolean eval(List<Var> varList, ExprTreeNode exprTree) {
        return switch(exprTree.token.type) {
            case EQ, NEQ -> evalAtomExpr(exprTree.left.token, exprTree.token,
                    exprTree.right.token, varList);
            case AND -> eval(varList, exprTree.left) && eval(varList, exprTree.right);
            case OR -> eval(varList, exprTree.left) || eval(varList, exprTree.right);
            default -> throw new AnalysisException("Invalid expression");
        };
    }

    private static boolean evalAtomExpr(ExprToken index, ExprToken opr,
                                       ExprToken regex, List<Var> varList) {
        int i = Integer.parseInt(index.token());
        return switch (opr.type) {
            case EQ -> paramString(varList.get(i - 1)).matches(regex.token());
            case NEQ -> !paramString(varList.get(i - 1)).matches(regex.token());
            default -> throw new AnalysisException(opr.type + " should not be in an atom expression");
        };
    }

    private static String paramString(Var v){
        return v.isConst() ? v.getConstValue().toString() : v.toString();
    }

    private enum ExprTokenType {
        NO_TYPE,
        INDEX,
        REGEX,
        EQ,
        NEQ,
        AND,
        OR,
        LEFT_PARENTHESES,
        RIGHT_PARENTHESES
    }

    private record ExprToken(ExprTokenType type, String token) {
    }

    private record ExprTreeNode(ExprToken token, ExprTreeNode left, ExprTreeNode right) {
    }

    private static class ExprLexer {

        private static final List<Rule> rules = List.of(
                Rule.getRule(" +", ExprTokenType.NO_TYPE),
                Rule.getRule("p([1-9][0-9]*)", ExprTokenType.INDEX),
                Rule.getRule("/((?:[^/\\\\]|\\\\.)*)/", ExprTokenType.REGEX),
                Rule.getRule("=", ExprTokenType.EQ),
                Rule.getRule("!=", ExprTokenType.NEQ),
                Rule.getRule("&", ExprTokenType.AND),
                Rule.getRule("\\|", ExprTokenType.OR),
                Rule.getRule("\\(", ExprTokenType.LEFT_PARENTHESES),
                Rule.getRule("\\)", ExprTokenType.RIGHT_PARENTHESES)
        );

        private ExprLexer() {
        }

        public static List<ExprToken> scan(String expr) {
            List<ExprToken> tokenList = new ArrayList<>();
            List<Matcher> matchers = Lists.map(rules, rule -> rule.pattern.matcher(expr));
            int position = 0;
            outer:
            while(position < expr.length()) {
                for(int i = 0; i < rules.size(); i++) {
                    Matcher matcher = matchers.get(i);
                    if(matcher.region(position, expr.length()).lookingAt()) {
                        ExprTokenType type = rules.get(i).type;
                        switch(type) {
                            case NO_TYPE -> {}
                            case INDEX -> tokenList.add(new ExprToken(type, matcher.group(1)));
                            case REGEX -> tokenList.add(new ExprToken(type,
                                    matcher.group(1).replace("\\/", "/")));
                            default -> tokenList.add(new ExprToken(type, matcher.group()));
                        }
                        position = matcher.end();
                        continue outer;
                    }
                }
                throw new AnalysisException("Failed to match tokens at position " + position + " in " + expr);
            }
            return tokenList;
        }

        private record Rule(Pattern pattern, ExprTokenType type) {

            public static Rule getRule(String regex, ExprTokenType type) {
                return new Rule(Pattern.compile(regex), type);
            }
        }
    }

    private static class ExprParser {

        private ExprParser() {
        }
        public static ExprTreeNode parse(String expr, List<ExprToken> tokens) {
            Stack<ExprTreeNode> operandStack = new Stack<>();
            Stack<ExprToken> operatorStack = new Stack<>();
            try {
                for(ExprToken token : tokens) {
                    switch (token.type()) {
                        case LEFT_PARENTHESES -> operatorStack.push(token);
                        case INDEX, REGEX -> operandStack.push(new ExprTreeNode(token, null, null));
                        case EQ, NEQ, AND, OR -> {
                            while(!operatorStack.isEmpty()
                                    && operatorStack.peek().type() != ExprTokenType.LEFT_PARENTHESES
                                    && getPriority(token.type()) <= getPriority(operatorStack.peek().type())) {
                                operandStack.push(extractExpr(operandStack, operatorStack));
                            }
                            operatorStack.push(token);
                        }
                        case RIGHT_PARENTHESES -> {
                            while(operatorStack.peek().type() != ExprTokenType.LEFT_PARENTHESES) {
                                operandStack.push(extractExpr(operandStack, operatorStack));
                            }
                            operatorStack.pop();
                        }
                    }
                }
                while(!operatorStack.isEmpty()) {
                    operandStack.push(extractExpr(operandStack, operatorStack));
                }
                if(operandStack.size() != 1)
                    throw new AnalysisException("Invalid grammar in expression " + expr);
            } catch (EmptyStackException e) {
                throw new AnalysisException("Invalid grammar in expression " + expr, e);
            }
            return operandStack.peek();
        }

        private static int getPriority(ExprTokenType type) {
            return switch (type) {
                case OR -> 0;
                case AND -> 1;
                case EQ, NEQ -> 2;
                default -> throw new AnalysisException(type + "isn't an operator");
            };
        }

        private static ExprTreeNode extractExpr(Stack<ExprTreeNode> operandStack,
                                                Stack<ExprToken> operatorStack) {
            ExprToken operator = operatorStack.pop();
            ExprTreeNode right = operandStack.pop();
            ExprTreeNode left = operandStack.pop();
            return new ExprTreeNode(operator, left, right);
        }
    }
}
