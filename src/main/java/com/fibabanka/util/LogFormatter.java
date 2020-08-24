package com.fibabanka.util;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogFormatter {

    public static void replaceLogMessage(String current, StringLiteralExpr logMessage, String className, String methodName) {
        final String regex = ".*?\\(\\)";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        final Matcher matcher = pattern.matcher(current);
        if (matcher.find()) {
            current = StringUtils.replace(current, matcher.group(0), "");
            current = StringUtils.replace(current, className + "." + matcher.group(0), "");
            current = StringUtils.replace(current, className + "." + methodName, "");
        }

        if (className.equals("") || methodName.equals("")) {
            return;
        }
        String prefix = "[" + className + "][" + methodName + "]";
        if (current.contains(methodName)) {
            current = StringUtils.replace(current, methodName + "()", "");
            current = StringUtils.replace(current, methodName, "");
        }
        current = StringUtils.replace(current, prefix, "");

        current = StringUtils.replace(current, prefix, "");
        current = StringUtils.replace(current, "\\\\", "");
        current = StringUtils.replace(current, "\\\\", "");
        current = StringUtils.replace(current, "\\\\", "");
        current = StringUtils.replace(current, "\\\\", "");
        current = StringUtils.replace(current, "\\\\", "");
        current = StringUtils.replace(current, "\\\\", "");
        current = StringUtils.replace(current, "[]", "");
        current = StringUtils.replace(current, " -> ", "");
        current = StringUtils.replace(current, "[" + className + "][]", "");
        current = StringUtils.replace(current, "[" + className + "]", "");
        current = StringUtils.replace(current, "[" + methodName + "]", "");

        logMessage.setString(prefix + " -> " + current.trim());
    }

    public static void change(File f) {
        try {
            boolean changed = false;
            CompilationUnit compilationUnit = StaticJavaParser.parse(f);
            List<FieldDeclaration> all = compilationUnit.findAll(FieldDeclaration.class);
            List<FieldDeclaration> logger = all.stream().filter(c -> c.getVariables().get(0).getName().asString().equalsIgnoreCase("LOGGER") || StringUtils.containsIgnoreCase(c.getVariables().get(0).getName().asString(), "Logger")).collect(Collectors.toList());

            for (FieldDeclaration fieldDeclaration : logger) {
                VariableDeclarator variableDeclarator = fieldDeclaration.getVariables().get(0);
                String loggerVariableName = variableDeclarator.getNameAsString();

                List<ExpressionStmt> callableDeclarations = compilationUnit.findAll(ExpressionStmt.class);
                for (ExpressionStmt callableDeclaration : callableDeclarations) {
                    Node node = callableDeclaration;
                    String methodName = "";
                    String className = "";
                    String loggerType = "";
                    if (callableDeclaration.toExpressionStmt().toString().contains(loggerVariableName)) {
                        while (true) {
                            if (node.getParentNode().isPresent()) {
                                if (node.getParentNode().get() instanceof MethodDeclaration) {

                                    MethodDeclaration methodDeclaration = (MethodDeclaration) node.getParentNode().get();

                                    //System.out.println(methodDeclaration);

                                    if (methodDeclaration.getParentNode().get() instanceof TypeDeclaration) {
                                        TypeDeclaration typeDeclaration = (TypeDeclaration) methodDeclaration.getParentNode().get();

                                        methodName = methodDeclaration.getNameAsString();
                                        className = typeDeclaration.getNameAsString();
                                        // System.out.println("Method Name :" + methodName);
                                        //  System.out.println("Class Name : " + className);
                                        break;
                                    } else {
                                        System.out.println("Bir sıkıntı var " + methodDeclaration);
                                        break;
                                    }

                                } else {
                                    node = node.getParentNode().get();
                                }
                            } else {
                                break;
                            }
                        }
                        if (callableDeclaration.getExpression() instanceof MethodCallExpr) {
                            MethodCallExpr methodCallExpr = (MethodCallExpr) callableDeclaration.getExpression();
                            NodeList<Expression> expressionNodeList = methodCallExpr.getArguments();

                            if (expressionNodeList.size() == 0) {
                                System.out.println("Sorunlu " + methodCallExpr);
                                continue;
                            }
                            Expression expression = expressionNodeList.get(0);

                            loggerType = methodCallExpr.getNameAsString();


                            String current = "";
                            StringLiteralExpr logMessage = null;
                            if (expression instanceof BinaryExpr) {
                                Node temp = ((BinaryExpr) expression).getLeft();
                                while (temp instanceof BinaryExpr) {
                                    temp = ((BinaryExpr) temp).getLeft();
                                }
                                if (temp instanceof StringLiteralExpr) {
                                    current = ((StringLiteralExpr) temp).asString();
                                    logMessage = ((StringLiteralExpr) (temp));
                                    replaceLogMessage(current, logMessage, className, methodName);
                                    changed = true;

                                }

                            } else if (expression instanceof StringLiteralExpr) {

                                logMessage = (StringLiteralExpr) expression;
                                current = ((StringLiteralExpr) expression).asString();

                                replaceLogMessage(current, logMessage, className, methodName);
                                changed = true;
                            } else {

                            }


                        }
                    }


                }
            }
            FileUtils.writeStringToFile(f, compilationUnit.toString(), Charset.defaultCharset().name());
            if (changed) {
                System.out.println("Replaced => " + f.getAbsolutePath());
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void walk(String path) {

        File root = new File(path);
        File[] list = root.listFiles();

        if (list == null) return;

        for (File f : list) {
            if (f.isDirectory()) {
                walk(f.getAbsolutePath());
                //System.out.println("Dir:" + f.getAbsoluteFile());
            } else {
                if (f.getName().endsWith("java")) {
                    change(f);
                }
            }
        }
    }

    public static void main(String[] args) {
        walk("");


    }
}

