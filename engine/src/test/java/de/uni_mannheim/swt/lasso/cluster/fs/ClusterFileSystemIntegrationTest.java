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
package de.uni_mannheim.swt.lasso.cluster.fs;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.compute.test.TestApplication;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestApplication.class})
public class ClusterFileSystemIntegrationTest {

    @Autowired
    ClusterEngine clusterEngine;


    @BeforeEach
    public void beforeEach() {
        if(!clusterEngine.getIgnite().cluster().active()) {
            clusterEngine.getIgnite().cluster().active(true);
        }
    }

    @AfterEach
    public void afterEach() throws IOException {
        // destroy
        clusterEngine.getFileSystem().clear();
    }

    @Test
    public void test() throws IOException {
        ClusterFileSystem clusterFileSystem = new ClusterFileSystem(clusterEngine);

        Path file = Files.createTempFile("bla", "txt");

        FileUtils.writeLines(file.toFile(), Arrays.asList("Hello", "World"));

        clusterFileSystem.write(file.toFile().getAbsolutePath(), file.toFile());

        InputStream in = clusterFileSystem.read(file.toFile().getAbsolutePath());

        String str = IOUtils.toString(in, "UTF-8");

        assertEquals(str, "Hello\nWorld\n");

        List<String> files = clusterFileSystem.listFiles(file.toFile().getParentFile().getAbsolutePath());
        files.forEach(System.out::println);

        assertEquals(1, files.size());
    }
}
