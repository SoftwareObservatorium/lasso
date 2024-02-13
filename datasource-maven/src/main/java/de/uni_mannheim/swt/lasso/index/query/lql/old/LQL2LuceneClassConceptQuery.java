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
import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Simple concept query (using content:Concept like content:Base64)
 *
 * @author Marcus Kessel
 */
public class LQL2LuceneClassConceptQuery extends LQL2LuceneClassQuery {

    private static final Logger LOG = LoggerFactory.getLogger(LQL2LuceneClassConceptQuery.class);

    //
    private boolean includeMethods = false;

    public LQL2LuceneClassConceptQuery(SearchOptions searchOptions) {
        super(searchOptions);

        setKeywordsAndConstraints(searchOptions.getKeywordsAndConstraints());
    }

    /**
     * @return MQL interface signatures transformed to Lucene {@link Query}
     */
    protected Query translateLQLToLuceneQuery() {
        if (!isLQL()) {
            return null;
        }

        // LQL
        // currently, limited to first query
        LinkedList<Query> queries = new LinkedList<>();

        Interface system = lqlParseResult.getInterfaceSpecification();
        String className = system.getName();
        String[] methodNames = system.getMethods().stream().map(MethodSignature::getName).toArray(String[]::new);

        queries.add(toQuery(className, methodNames));

        return queries.getFirst();
    }

    /**
     * Respects camel casing of concepts.
     * <p>
     * This is more like a bag of words approach.
     *
     * @param className
     * @param methodNames
     * @return
     */
    private Query toQuery(String className, String[] methodNames) {
        // TODO also add method names as a pointer?
        String[] names = splitCamelCase(className);

        BooleanQuery.Builder query = new BooleanQuery.Builder();
        // add full name
        query.add(new TermQuery(new Term("content", className)), BooleanClause.Occur.SHOULD);

        if (ArrayUtils.isNotEmpty(names) && names.length > 1) {
            Arrays.stream(names)
                    .map(name -> new TermQuery(new Term("content", name)))
                    .forEach(q -> query.add(q, BooleanClause.Occur.SHOULD));
        }

        if (includeMethods && ArrayUtils.isNotEmpty(methodNames)) {
            // apply camelcase split as well
            Arrays.stream(methodNames)
                    .flatMap(name -> Arrays.stream(splitCamelCase(name)))
                    .map(name -> new TermQuery(new Term("content", name)))
                    .forEach(q -> query.add(q, BooleanClause.Occur.SHOULD));
        }

        return query.build();
    }

    private String[] splitCamelCase(String className) {
        //
        return StringUtils.splitByCharacterTypeCamelCase(className);
    }

    public boolean isIncludeMethods() {
        return includeMethods;
    }

    public void setIncludeMethods(boolean includeMethods) {
        this.includeMethods = includeMethods;
    }
}
