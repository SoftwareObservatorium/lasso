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
package de.uni_mannheim.swt.lasso.arena.task.load;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Provider for locating sheets.
 *
 * @author Marcus Kessel
 */
public class DefaultSheetProvider extends FileSystemSheetProvider {

    private final List<System> implementationList;

    public DefaultSheetProvider(File path, CandidatePool pool, List<System> implementationList) {
        super(path, null, pool);

        this.implementationList = implementationList;
    }

    @Override
    protected ClassUnderTest retrieve(String implementation) throws IOException {
        Optional<ClassUnderTest> classUnderTestOptional = implementationList.stream()
                .filter(i -> StringUtils.equals(implementation, i.getId()))
                .map(ClassUnderTest::new).findFirst();

        return classUnderTestOptional.get();
    }
}
