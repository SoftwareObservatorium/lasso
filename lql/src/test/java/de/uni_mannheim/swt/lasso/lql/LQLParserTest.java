/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.lql;

import de.uni_mannheim.swt.lasso.lql.listener.InterfaceListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
public class LQLParserTest {

    @Test
    public void test_stack() {
        String lqlQuery = "Stack {\n" +
                "    push(java.lang.Object)->java.lang.Object\n" +
                "    pop()->java.lang.Object\n" +
                "    peek()->java.lang.Object\n" +
                "    size()->int}";

        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());

        assertEquals(1, listener.getParseResult().getInterfaceSpecification().getMethods().get(0).getInputs().size());
    }

    @Test
    public void test_stack_filter() {
        String lqlQuery = "Stack {\n" +
                "    push(java.lang.Object)->java.lang.Object\n" +
                "    pop()->java.lang.Object\n" +
                "    peek()->java.lang.Object\n" +
                "    size()->int}\n" +
                "!name_fq:Queue^10 !name_fq:Deque^10";

        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());

        assertEquals(1, listener.getParseResult().getInterfaceSpecification().getMethods().get(0).getInputs().size());
        assertEquals(Arrays.asList("!name_fq:Queue^10", "!name_fq:Deque^10"), listener.getParseResult().getFilters());
    }

    @Test
    public void test_generics() {
        String lqlQuery = "HumanEval_95_check_dict_case {\n" +
                "  checkDictCase(java.util.HashMap<java.lang.String, java.lang.String>)->boolean\n" +
                "}";

        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());

        assertEquals(1, listener.getParseResult().getInterfaceSpecification().getMethods().get(0).getInputs().size());
    }

    @Test
    public void test_placeholder() {
        String lqlQuery = "$ {\n" +
                "  checkDictCase(java.util.HashMap<java.lang.String, java.lang.String>)->boolean\n" +
                "}";

        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());

        assertEquals(1, listener.getParseResult().getInterfaceSpecification().getMethods().get(0).getInputs().size());
    }

    @Test
    public void test_generics_nested() {
        String lqlQuery = "HumanEval_115_max_fill {\n" +
                "  maxFill(ArrayList<ArrayList<Long>>,long)->long\n" +
                "}";

        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());

        assertEquals(2, listener.getParseResult().getInterfaceSpecification().getMethods().get(0).getInputs().size());
    }

    @Test
    public void testParse_mixed() {
        String lqlQuery = "SampleClass{doSomething(one.sub,two)->three.sub,four.sub}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_qualified_deep() {
        String lqlQuery = "SampleClass{doSomething(one.sub.sub.sub,two)->three.sub,four.sub}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_simple() {
        String lqlQuery = "SampleClass{doSomething(One,Two)->Three,Four}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_constructor() {
        String lqlQuery = "SampleClass{SampleClass(One,Two)->Three,Four}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_constructor_empty() {
        String lqlQuery = "SampleClass{SampleClass()}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_method_empty() {
        String lqlQuery = "SampleClass{doSomething()->Object}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_formatted() {
        String lqlQuery = "SampleClass{\n" +
                "method1(one.sub,two)->three.sub,four.sub\n" +
                "method2(one.sub,two)->three.sub,four.sub\n" +
                "}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_formatted_spaces() {
        String lqlQuery = "SampleClass {\n" +
                "method1(one.sub, two)->three.sub, four.sub\n" +
                "method2(one.sub, two)->three.sub, four.sub\n" +
                "    }";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_filters() {
        String lqlQuery = "SampleClass{doSomething(one.sub,two)->three.sub,four.sub} lala:huhu nana:mimi";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_singlefilter() {
        String lqlQuery = "Stack";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_singlefilter_star() {
        String lqlQuery = "*:*";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_arrays() {
        String lqlQuery = "SampleClass{doSomething(one.sub[],Two[][])->three.sub,four.sub}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_filterOnly() {
        String lqlQuery = "lala:huhu nana:mimi";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    @Test
    public void testParse_namedparam() {
        String lqlQuery = "SampleClass{doSomething(myparam=one.sub,second=two)->bla=three.sub}";
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        print(parser, tree);
        System.out.println(listener.getParseResult());
    }

    void print(LQLParser parser, LQLParser.ParseContext tree) {
        List<String> ruleNamesList = Arrays.asList(parser.getRuleNames());
        String prettyTree = TreeUtils.toPrettyTree(tree, ruleNamesList);
        System.out.println(prettyTree);
    }
}
