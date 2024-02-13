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
package de.uni_mannheim.swt.lasso.arena.classloader.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initialize Sandbox
 *
 * @author Marcus Kessel
 */
public class Sandbox {

    private static final Logger LOG = LoggerFactory
            .getLogger(Sandbox.class);

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    public static void initialize(Path basePath) {
        if(!initialized.get()) {
            String path = String.format("%s%s-", basePath.toString(), File.separator);

            if(LOG.isInfoEnabled()) {
                LOG.info("Setting up policies for CUT execution with base path '{}'", path);
            }

            //
            //Policy.setPolicy(new SandboxSecurityPolicy(path));
            System.setSecurityManager(new SandboxSecurityManager(path));

            initialized.set(true);

            if(LOG.isInfoEnabled()) {
                LOG.info("Sandbox has been initialized");
            }
        }
    }

    public static boolean isInitialized() {
        return initialized.get();
    }
}
