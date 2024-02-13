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
package de.uni_mannheim.swt.lasso.lql.parser;

import de.uni_mannheim.swt.lasso.lql.LQLLexer;
import de.uni_mannheim.swt.lasso.lql.LQLParser;
import de.uni_mannheim.swt.lasso.lql.listener.InterfaceListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * LASSO Query Language Parser
 *
 * @author Marcus Kessel
 */
public class LQL {

    /**
     * Parse LQL query.
     *
     * @param lqlQuery
     * @return
     */
    public static LQLParseResult parse(String lqlQuery) {
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lqlQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener= new InterfaceListener();

        walker.walk(listener, tree);

        LQLParseResult parseResult = listener.getParseResult();
        if(parseResult.hasInterfaceSpecification()) {
            parseResult.getInterfaceSpecification().setLqlQuery(lqlQuery);
        }

        return parseResult;
    }
}
