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
import org.apache.commons.collections4.CollectionUtils;
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

        if(ctx.simpletype() != null) {
            parseResult.getInterfaceSpecification().setName(ctx.simpletype().getText());
        }

        if(ctx.qualifiedtype() != null) {
            parseResult.getInterfaceSpecification().setName(ctx.qualifiedtype().getText());
        }
    }

    @Override
    public void enterMethodSig(LQLParser.MethodSigContext ctx) {
        super.enterMethodSig(ctx);

//        LOG.debug("discovered method '{}'", ctx.NAME().getText());
//        LOG.debug("discovered inputs '{}'", ctx.inputs().getText());
//        LOG.debug("discovered outputs '{}'", ctx.outputs().getText());

        MethodSignature method = new MethodSignature();
        method.setName(ctx.NAME().getText());
        method.setConstructor(parseResult.getInterfaceSpecification().getSimpleName().equals(method.getName()));

        if(ctx.inputs() != null) {
            //method.setInputs(Arrays.asList(StringUtils.split(ctx.inputs().getText(), ",")));
            List<String> inputs = new ArrayList<>(ctx.inputs().parameters().getChildCount());
            List<String> inputNames = new ArrayList<>(ctx.inputs().parameters().getChildCount());

            for(int i = 0; i < ctx.inputs().parameters().getChildCount(); i++) {
                if(!ctx.inputs().parameters().getChild(i).getText().equals(",")) {
                    String text = ctx.inputs().parameters().getChild(i).getText();
                    if(StringUtils.contains(text, "=")) {
                        String[] parts = StringUtils.split(text, "=");
                        inputs.add(parts[1]);
                        inputNames.add(parts[0]);
                    } else {
                        inputs.add(text);
                    }
                }
            }
            method.setInputs(inputs);
            if(CollectionUtils.isNotEmpty(inputNames)) {
                method.setInputNames(inputNames);
            }
        }

        if(ctx.outputs() != null) {
            //method.setOutputs(Arrays.asList(StringUtils.split(ctx.outputs().getText(), ",")));
            List<String> outputs = new ArrayList<>(ctx.outputs().parameters().getChildCount());
            List<String> outputNames = new ArrayList<>(ctx.outputs().parameters().getChildCount());

            for(int i = 0; i < ctx.outputs().parameters().getChildCount(); i++) {
                if(!ctx.outputs().parameters().getChild(i).getText().equals(",")) {
                    String text = ctx.outputs().parameters().getChild(i).getText();
                    if(StringUtils.contains(text, "=")) {
                        String[] parts = StringUtils.split(text, "=");
                        outputs.add(parts[1]);
                        outputNames.add(parts[0]);
                    } else {
                        outputs.add(text);
                    }
                }
            }
            method.setOutputs(outputs);
            if(CollectionUtils.isNotEmpty(outputNames)) {
                method.setOutputNames(outputNames);
            }
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
