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
package de.uni_mannheim.swt.lasso.sheets.service.config;

import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.sheets.service.SheetsManager;
import de.uni_mannheim.swt.lasso.sheets.service.cut.CodeGeneration;
import de.uni_mannheim.swt.lasso.sheets.service.cut.InterfaceGeneration;
import de.uni_mannheim.swt.lasso.sheets.service.cut.SheetGeneration;
import de.uni_mannheim.swt.lasso.sheets.service.srh.InMemorySRH;
import de.uni_mannheim.swt.lasso.sheets.service.driver.LocalSimpleTestDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.sql.SQLException;

/**
 * Basic engine config.
 *
 * @author Marcus Kessel
 */
@Configuration
public class EngineConfig {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private Environment env;

    @Bean
    public SheetsManager sheetsManager(MavenRepository mavenRepository, InMemorySRH inMemorySRH, CodeGeneration codeGeneration, SheetGeneration sheetGeneration) {
        SheetsManager sheetsManager = new SheetsManager(new LocalSimpleTestDriver(mavenRepository, inMemorySRH, codeGeneration, sheetGeneration));

        return sheetsManager;
    }

    @Bean
    public MavenRepository mavenRepository() {
        File localRepo = new File(env.getProperty("maven.repo.local"));
        DependencyResolver resolver = new DependencyResolver(env.getProperty("maven.repo.url"), localRepo.getAbsolutePath());
        return new MavenRepository(resolver);
    }

    @Bean
    public InMemorySRH inMemorySRH() throws SQLException {
        InMemorySRH inMemorySRH = new InMemorySRH();
        inMemorySRH.initialize();

        return inMemorySRH;
    }

    @Bean
    public CodeSearch codeSearch() {
        SolrInstance solrInstance = new SolrInstance(env.getProperty("codesearch.solr.core"), env.getProperty("codesearch.solr.user"), env.getProperty("codesearch.solr.pass"), env.getProperty("codesearch.solr.url"));
        CodeSearch codeSearch = new CodeSearch(solrInstance);

        return codeSearch;
    }

    @Bean
    public CodeGeneration codeGeneration(MavenRepository mavenRepository) {
        CodeGeneration codeGeneration = new CodeGeneration(mavenRepository, env.getProperty("codegen.ollama.baseurl"));

        return codeGeneration;
    }

    @Bean
    public InterfaceGeneration interfaceGeneration() {
        InterfaceGeneration interfaceGeneration = new InterfaceGeneration(env.getProperty("codegen.ollama.baseurl"));

        return interfaceGeneration;
    }

    @Bean
    public SheetGeneration sheetGeneration() {
        SheetGeneration sheetGeneration = new SheetGeneration(env.getProperty("codegen.ollama.baseurl"));

        return sheetGeneration;
    }
}
