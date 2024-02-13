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
package de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;

import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for adapter generation.
 *
 * @author Marcus Kessel
 */
@Deprecated
public class TestAdaptationManager extends MavenProjectManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(TestAdaptationManager.class);

    public static final String LASSO_REPORTS_PERM_PATH =
            String.format("%s%sperm-success", LASSO_REPORTS_PATH, File.separator);

    public static final String ODISSE_RECORD_PATH = "odisse.record.path";
    public static final String PASSTHROUGH_PATH = "passthrough.path";

    public static String getPassthroughPathProperty() {
        return String.format("-D%s=%s/", PASSTHROUGH_PATH, LASSO_REPORTS_PERM_PATH);
    }

    /**
     * Adapter name postfix
     */
    public static final String ADAPTER_NAME_POSTFIX = "_Adapter";

    /**
     * Package where test class goes
     */
    public static final String ADAPTER_TEST_PKG = "de.uni_mannheim.swt.lasso.test";

    public TestAdaptationManager(LSLExecutionContext lslExecutionContext) throws IOException {
        super(lslExecutionContext);
    }
}
