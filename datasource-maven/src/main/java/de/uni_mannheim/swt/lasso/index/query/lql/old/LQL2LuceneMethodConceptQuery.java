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
package de.uni_mannheim.swt.lasso.index.query.lql.old;

import de.uni_mannheim.swt.lasso.index.SearchOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple concept query (using content:Concept like content:Base64)
 * 
 * @author Marcus Kessel
 * 
 */
public class LQL2LuceneMethodConceptQuery extends LQL2LuceneClassConceptQuery {

    private static final Logger LOG = LoggerFactory.getLogger(LQL2LuceneMethodConceptQuery.class);

    public LQL2LuceneMethodConceptQuery(SearchOptions searchOptions) {
        super(searchOptions);

        setKeywordsAndConstraints(searchOptions.getKeywordsAndConstraints());

        // set
        setIncludeMethods(true);
    }
}
