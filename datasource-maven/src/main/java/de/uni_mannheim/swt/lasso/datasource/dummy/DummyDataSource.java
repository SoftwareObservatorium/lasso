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
package de.uni_mannheim.swt.lasso.datasource.dummy;

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/**
 * A dummy data source for demonstration purposes.
 *
 * @author Marcus Kessel
 */
public class DummyDataSource extends DataSource {

    private static final Logger LOG = LoggerFactory
            .getLogger(DummyDataSource.class);

    @Override
    public Object createQueryModelForLSL() {
        return new DummyQuery();
    }

    @Override
    public QueryResult query(Object queryModel) throws IOException {
        DummyQuery dummyQuery = (DummyQuery) queryModel;

        QueryResult queryResult = new QueryResult();
        queryResult.setNumFound(1);

        CodeUnit unit = new CodeUnit();
        unit.setId(UUID.randomUUID().toString());
        unit.setDataSource(getId());
        unit.setName(dummyQuery.getClassName());
        unit.setPackagename(dummyQuery.getPackageName());
        unit.setContent(dummyQuery.getSource());
        unit.setUnitType(CodeUnit.CodeUnitType.CLASS);

        // FIXME some fake data
        unit.setGroupId("junit");
        unit.setArtifactId("junit");
        unit.setVersion("4.13.2");
        queryResult.setImplementations(new ArrayList<>(Collections.singletonList(unit)));

        return queryResult;
    }
}
