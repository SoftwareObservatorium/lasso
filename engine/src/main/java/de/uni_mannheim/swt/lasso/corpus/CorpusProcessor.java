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

import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.datasource.maven.support.RandomMavenCentralRepository;
import de.uni_mannheim.swt.lasso.datasource.maven.util.HttpUtils;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * Load executable corpus dynamically from a resource.
 *
 * @author Marcus Kessel
 */
public class CorpusProcessor implements BeanFactoryPostProcessor, EnvironmentAware, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(CorpusProcessor.class);

    private Environment env;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        Resource resource = resourceLoader.getResource(env.getProperty("corpus"));
        ExecutableCorpus corpus;
        try {
            corpus = CorpusUtils.read(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Load configuration for executable corpus:\n{}", ToStringBuilder.reflectionToString(corpus));

        // register bean
        configurableListableBeanFactory.registerSingleton(corpus.getId(), corpus);

        for(Datasource ds : corpus.getDatasources()) {
            HttpClient client = HttpUtils.createHttpClient(ds.getUser(), ds.getPass());

            SolrClient solrClient = new HttpSolrClient.Builder(ds.getHost())
                    .withHttpClient(client).build();
            RandomMavenCentralRepository mavenCentralRepository = new RandomMavenCentralRepository(solrClient);

            MavenDataSource mavenDataSource = new MavenDataSource(new MavenCentralIndex(mavenCentralRepository,
                    new QueryBuilder()));

            mavenDataSource.setId(ds.getId());
            mavenDataSource.setName(ds.getName());
            mavenDataSource.setDescription(ds.getDescription());

            // register bean
            configurableListableBeanFactory.registerSingleton(mavenDataSource.getId(), mavenDataSource);
        }
    }
}
