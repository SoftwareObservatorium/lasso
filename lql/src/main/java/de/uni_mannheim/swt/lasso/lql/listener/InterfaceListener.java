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
package de.uni_mannheim.swt.lasso.lql.listener;

import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import de.uni_mannheim.swt.lasso.lql.LQLBaseListener;
import de.uni_mannheim.swt.lasso.lql.LQLParser;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parser listener to create {@link Interface}s.
 *
 * @author Marcus Kessel
 */
public class InterfaceListener extends LQLBaseListener {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceListener.class);

    private LQLParseResult parseResult = new LQLParseResult();

    @Override
    public void enterInterfaceSpec(LQLParser.InterfaceSpecContext ctx) {
        super.enterInterfaceSpec(ctx);

        //LOG.debug("discovered system '{}'", ctx.NAME().getText());

        parseResult.setInterfaceSpecification(new Interface());

        parseResult.getInterfaceSpecification().setName(ctx.NAME().getText());
    }

    @Override
    public void enterMethodSig(LQLParser.MethodSigContext ctx) {
        super.enterMethodSig(ctx);

//        LOG.debug("discovered method '{}'", ctx.NAME().getText());
//        LOG.debug("discovered inputs '{}'", ctx.inputs().getText());
//        LOG.debug("discovered outputs '{}'", ctx.outputs().getText());

        MethodSignature method = new MethodSignature();
        method.setName(ctx.NAME().getText());
        method.setConstructor(parseResult.getInterfaceSpecification().getName().equals(method.getName()));

        if(ctx.inputs() != null) {
            //method.setInputs(Arrays.asList(StringUtils.split(ctx.inputs().getText(), ",")));
            List<String> inputs = new ArrayList<>(ctx.inputs().parameters().getChildCount());
            for(int i = 0; i < ctx.inputs().parameters().getChildCount(); i++) {
                if(!ctx.inputs().parameters().getChild(i).getText().equals(",")) {
                    inputs.add(ctx.inputs().parameters().getChild(i).getText());
                }
            }
            method.setInputs(inputs);
        }

        if(ctx.outputs() != null) {
            //method.setOutputs(Arrays.asList(StringUtils.split(ctx.outputs().getText(), ",")));
            List<String> outputs = new ArrayList<>(ctx.outputs().parameters().getChildCount());
            for(int i = 0; i < ctx.outputs().parameters().getChildCount(); i++) {
                if(!ctx.outputs().parameters().getChild(i).getText().equals(",")) {
                    outputs.add(ctx.outputs().parameters().getChild(i).getText());
                }
            }
            method.setOutputs(outputs);
        } else {
            method.setOutputs(Collections.singletonList("void"));
        }

        parseResult.getInterfaceSpecification().getMethods().add(method);
    }

    @Override
    public void enterFilter(LQLParser.FilterContext ctx) {
        super.enterFilter(ctx);

        //LOG.debug("discovered filter '{}'", ctx.FILTERVALUE().getText());

        if(ctx.FILTERVALUE() != null) {
            parseResult.getFilters().add(ctx.FILTERVALUE().getText());
        }
    }

    public LQLParseResult getParseResult() {
        return parseResult;
    }
}
