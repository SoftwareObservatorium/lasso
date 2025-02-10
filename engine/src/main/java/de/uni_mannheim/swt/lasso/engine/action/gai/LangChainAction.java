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
package de.uni_mannheim.swt.lasso.engine.action.gai;

import com.github.javaparser.JavaParser;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.gai.openai.Prompt;
import groovy.lang.Closure;
import org.apache.commons.collections4.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Abstract GAI Action based on langchain4j.
 *
 * @author Marcus Kessel
 */
public abstract class LangChainAction extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(LangChainAction.class);

    protected List<Prompt> readPrompts(LSLExecutionContext context, ActionConfiguration actionConfiguration, String defaultModel) {
        List<Prompt> prompts = new LinkedList<>();
        if(actionConfiguration.getConfiguration().containsKey("prompt")) {
            Closure closure = (Closure) ((Object[]) actionConfiguration.getConfiguration().get("prompt"))[0];

            LOG.debug("FOUND PROMPT METHOD " + closure);
            // get query model
            Prompt myPrompt = new Prompt();
            context.getLassoContext().register(myPrompt);
            List<Map> promptMaps = (List<Map>) context.getLassoContext().getActionContainerSpec().getActions().get(getName()).applyCustomCommand(closure, myPrompt, actionConfiguration.getAbstraction());

            LOG.debug("FOUND PROMPT MODELS " + promptMaps);

            prompts = promptMaps.stream().map(m -> {
                // FIXME add more properties
                Prompt prompt = new Prompt();
                prompt.setPromptContent(m.get("promptContent").toString()); // GString
                prompt.setId(m.get("id").toString()); // GString

                if(m.containsKey("model")) {
                    prompt.setModel(m.get("model").toString());
                } else {
                    prompt.setModel(defaultModel);
                }

                return prompt;
            }).toList();
        }

        return prompts;
    }

    /**
     * Override
     *
     * @param prompt
     * @param endpoint
     * @return
     */
    protected abstract String generate(Prompt prompt, String endpoint);

    /**
     * Loop through list of servers in a round-robin fashion
     *
     * @param servers
     * @return
     */
    protected Iterator<String> getRoundRobinIterator(List<String> servers) {
        return IteratorUtils.loopingListIterator(servers);
    }

    protected CodeUnit parse(String code, String pkg) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Parsing code\n{}", code);
        }

        try {
            JavaParser javaParser = new JavaParser();
            com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(code).getResult().get();

            // parse name
            CodeUnit unit = new CodeUnit();
            unit.setId(UUID.randomUUID().toString());
            unit.setName(cu.getType(0).getNameAsString());

            // add package name
            cu.setPackageDeclaration(pkg);

            unit.setPackagename(pkg);
            unit.setContent(cu.toString());
            unit.setUnitType(CodeUnit.CodeUnitType.CLASS);

            return unit;
        } catch (Throwable e) {
            LOG.warn("failed to parse code", e);
            return null;
        }
    }
}
