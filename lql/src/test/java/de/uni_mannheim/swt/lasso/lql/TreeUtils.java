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

import java.util.List;

import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;

// copied from here: https://stackoverflow.com/questions/50064110/antlr4-java-pretty-print-parse-tree-to-stdout
public class TreeUtils {

    /** Platform dependent end-of-line marker */
    public static final String Eol = System.lineSeparator();
    /** The literal indent char(s) used for pretty-printing */
    public static final String Indents = "  ";
    private static int level;

    private TreeUtils() {}

    /**
     * Pretty print out a whole tree. {@link #getNodeText} is used on the node payloads to get the text
     * for the nodes. (Derived from Trees.toStringTree(....))
     */
    public static String toPrettyTree(final Tree t, final List<String> ruleNames) {
        level = 0;
        return process(t, ruleNames).replaceAll("(?m)^\\s+$", "").replaceAll("\\r?\\n\\r?\\n", Eol);
    }

    private static String process(final Tree t, final List<String> ruleNames) {
        if (t.getChildCount() == 0) return Utils.escapeWhitespace(Trees.getNodeText(t, ruleNames), false);
        StringBuilder sb = new StringBuilder();
        sb.append(lead(level));
        level++;
        String s = Utils.escapeWhitespace(Trees.getNodeText(t, ruleNames), false);
        sb.append(s + ' ');
        for (int i = 0; i < t.getChildCount(); i++) {
            sb.append(process(t.getChild(i), ruleNames));
        }
        level--;
        sb.append(lead(level));
        return sb.toString();
    }

    private static String lead(int level) {
        StringBuilder sb = new StringBuilder();
        if (level > 0) {
            sb.append(Eol);
            for (int cnt = 0; cnt < level; cnt++) {
                sb.append(Indents);
            }
        }
        return sb.toString();
    }
}
