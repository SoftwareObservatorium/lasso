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
package de.uni_mannheim.swt.lasso.cluster.config;

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import org.springframework.context.ApplicationContext;

/**
 * Spring-based {@link LassoConfiguration} using {@link ApplicationContext} to resolve resources and beans.
 *
 * @author Marcus Kessel
 *
 */
public class SpringLassoConfiguration extends LassoConfiguration {
    private final ApplicationContext applicationContext;

    public SpringLassoConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public DataSource getDataSource(String id) {
        DataSource dataSource = applicationContext
                .getBean(id, DataSource.class);

        return dataSource;
    }

    @Override
    public ExecutableCorpus getExecutableCorpus() {
        return applicationContext.getBean(ExecutableCorpus.class);
    }

    public <T> T getProperty(String name, Class<T> tClass) {
        return applicationContext.getEnvironment().getProperty(name, tClass);
    }

    public <T> T getService(Class<T> type) {
        return applicationContext.getBean(type);
    }
}
