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
package de.uni_mannheim.swt.lasso.service;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;
import de.uni_mannheim.swt.lasso.service.persistence.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Tester providing some utilities for convenience
 * 
 * @author Marcus Kessel
 *
 */
public class Tester {
    
    /**
     * Load resources from classpath
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String getResource(String fileName) throws IOException {
        return IOUtils.toString(Tester.class
                .getResourceAsStream(fileName));
    }

    public static File getResourceFile(String fileName) {
        return FileUtils.toFile(Tester.class
                .getResource(fileName));
    }

    public static UserInfo userInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setRemoteIpAddress("127.0.0.1");

        User user = new User();
        user.setUsername("ladmin");
        userInfo.setUserDetails(user);

        return userInfo;
    }

    public static de.uni_mannheim.swt.lasso.core.model.System system(String id, String name, String pkg) {
        return system(id, name, pkg, null);
    }

    public static de.uni_mannheim.swt.lasso.core.model.System system(String id, String name, String pkg, String code) {
        CodeUnit codeUnit = new CodeUnit();
        codeUnit.setId(id);
        codeUnit.setName(name);
        codeUnit.setPackagename(pkg);
        codeUnit.setContent(code);
        //MavenProject mavenProject = new MavenProject(new File("/tmp/project_" + id + "_" + System.currentTimeMillis()), true);
        de.uni_mannheim.swt.lasso.core.model.System system = new de.uni_mannheim.swt.lasso.core.model.System(codeUnit);

        return system;
    }
}
