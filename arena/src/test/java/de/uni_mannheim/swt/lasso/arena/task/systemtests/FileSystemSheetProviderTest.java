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
package de.uni_mannheim.swt.lasso.arena.task.systemtests;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.task.load.FileSystemSheetProvider;
import de.uni_mannheim.swt.lasso.arena.task.load.ResolvedSheets;
import de.uni_mannheim.swt.lasso.arena.task.load.SheetMatch;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
public class FileSystemSheetProviderTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);
    CodeSearch codeSearch = new CodeSearch();

    @Test
    public void test_findSheets() throws IOException, ParseException {
        File path = new File("sheets/evosuiteRef_f204cfa2-7b5e-4d4b-acce-20e7e048b2f1");
        System.out.println(path.getAbsolutePath());

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, new CandidatePool(mavenRepository));

        List<SheetMatch> sheets = provider.findSheets();

        assertEquals(5, sheets.size());

        for(SheetMatch entry : sheets) {
            System.out.println(entry.getImplementation() + " => " + entry.getFile());
        }
    }

    @Test
    public void test_findSheets_XLSX() throws IOException, ParseException {
        String mql = "Stack{" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/execute_9e733344-e8a3-4910-93d2-403a302828a6");
        System.out.println(path.getAbsolutePath());

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, new CandidatePool(mavenRepository));
        provider.setInterfaceSpecification(specification);

        List<SheetMatch> sheets = provider.findSheets();

        assertEquals(8, sheets.size());

        for(SheetMatch entry : sheets) {
            System.out.println(entry.getImplementation() + " => " + entry.getFile());
        }

        List<ResolvedSheets> resolvedSheetList = provider.resolve();


    }
}
