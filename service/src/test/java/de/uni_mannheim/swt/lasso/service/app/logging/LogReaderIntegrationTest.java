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
package de.uni_mannheim.swt.lasso.service.app.logging;

import de.uni_mannheim.swt.lasso.service.app.LassoApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Marcus Kessel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {LassoApplication.class}, properties = {"spring.main.allow-bean-definition-overriding=true"})
public class LogReaderIntegrationTest {

    @Autowired
    private Environment env;

    @Test
    public void test() throws IOException {
        LogReader logReader = new LogReader();
        List<String> lines = logReader.tail(new File(env.getProperty("lasso.logging.file", String.class)), 100);

        assertThat(lines.size(), equalTo(100));

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + lines);
    }
}
