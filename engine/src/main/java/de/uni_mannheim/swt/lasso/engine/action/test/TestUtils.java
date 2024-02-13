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
package de.uni_mannheim.swt.lasso.engine.action.test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 *
 * @author Marcus Kessel
 */
public class TestUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(TestUtils.class);

    public static void setUpTestClass(System executable, String source) throws IOException {
        JavaParser javaParser = new JavaParser();

        ParseResult<CompilationUnit> result = javaParser.parse(source);
        Optional<CompilationUnit> cuOp = result.getResult();

        CompilationUnit cu = cuOp.orElseThrow(() -> new IOException("Could not read test class for " + executable.getId()));

        de.uni_mannheim.swt.lasso.core.model.CompilationUnit testUnit = new de.uni_mannheim.swt.lasso.core.model.CompilationUnit();
        testUnit.setPkg(executable.getCode().getPackagename());
        testUnit.setName(cu.getType(0).getNameAsString());
        testUnit.setSourceCode(cu.toString());

        executable.getProject().writeCompilationUnit(testUnit, true);

        LOG.info("Wrote test suite to '{}'", executable.getProject().getSrcTest().getAbsolutePath());
    }
}
