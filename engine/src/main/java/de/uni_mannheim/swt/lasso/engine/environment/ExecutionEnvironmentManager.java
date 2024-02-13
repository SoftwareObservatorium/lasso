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
package de.uni_mannheim.swt.lasso.engine.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 *
 * @author Marcus Kessel
 *
 */
public class ExecutionEnvironmentManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(ExecutionEnvironmentManager.class);

    public static final String MAVEN = "maven";
    public static final String BASIC = "basic";
    public static final String ARENA = "arena";

    private final String proxyRegistry;
    private final int pullTimeout;
    private final String mavenDefaultImage;

    public ExecutionEnvironmentManager(String proxyRegistry, int pullTimeout, String mavenDefaultImage) {
        this.proxyRegistry = proxyRegistry;
        this.pullTimeout = pullTimeout;
        this.mavenDefaultImage = mavenDefaultImage;
    }

    protected String generateExecutionEnvironmentInstanceId(String environmentId) {
        return environmentId + "_" + UUID.randomUUID().toString();
    }

    public ExecutionEnvironment createExecutionEnvironment(String environmentId) {
        String executionEnvironmentId = generateExecutionEnvironmentInstanceId(environmentId);

        ExecutionEnvironment environment = null;

        switch(environmentId) {
            case MAVEN: {
                environment = new MavenExecutionEnvironment(executionEnvironmentId, proxyRegistry, pullTimeout, mavenDefaultImage);

                break;
            }
            case BASIC: {
                environment = new BasicExecutionEnvironment(executionEnvironmentId, proxyRegistry, pullTimeout);

                break;
            }
            case ARENA: {
                environment = new ArenaExecutionEnvironment(executionEnvironmentId, proxyRegistry, pullTimeout);

                break;
            }
            default: {
                throw new IllegalStateException(String.format("Unknown environment id %s", environmentId));
            }
        }

        return environment;
    }

    public void run(ExecutionEnvironment executionEnvironment) {
        try {
            executionEnvironment.run();
        } catch (InterruptedException e) {
            LOG.warn("Run failed", e);
        }
    }

    public void kill(ExecutionEnvironment executionEnvironment) {
        executionEnvironment.kill();
    }

    public void delete(ExecutionEnvironment executionEnvironment) {
        executionEnvironment.delete();
    }
}
