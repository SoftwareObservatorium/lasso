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
package de.uni_mannheim.swt.lasso.corpus;

import de.uni_mannheim.swt.lasso.datasource.dummy.DummyDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 *
 * @author Marcus Kessel
 */
@Configuration
public class CorpusConfig {

    @Autowired
    private Environment env;

    @Bean
    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new CorpusProcessor();
    }

    @Bean("dummy")
    public DummyDataSource dummy() {
        DummyDataSource dummyDataSource = new DummyDataSource();

        dummyDataSource.setId("dummy");
        dummyDataSource.setName("dummy");
        dummyDataSource.setDescription("A dummy datasource for demonstration purposes");

        return dummyDataSource;
    }
}
