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
package de.uni_mannheim.swt.lasso.gai.openai.util;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Markdown parser to extract code blocks.
 *
 * @author Marcus Kessel
 */
public class ContentParser {

    private String regex = "```[\\s\\S]*?```";

    public List<String> extractCode(String content) {
        // FIXME move
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        List<String> matches = new LinkedList<>();

        while (matcher.find()) {
            String codeBlock = matcher.group();
            matches.add(StringUtils.replaceEach(codeBlock, new String[]{"```java\n", "```"}, new String[]{"", ""}));
        }

        return matches;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }
}
