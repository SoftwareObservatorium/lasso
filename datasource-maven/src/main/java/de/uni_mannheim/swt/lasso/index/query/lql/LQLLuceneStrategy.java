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
package de.uni_mannheim.swt.lasso.index.query.lql;

import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;

import java.io.IOException;

/**
 *
 * @author Marcus Kessel
 */
public interface LQLLuceneStrategy {

    LQLParseResult getLQLParseResult();

    Query getLuceneQuery() throws IOException;

    boolean isLQL();

    boolean isEmpty();

    Query createComponentQuery(String className, String constraints, MethodSignature[] constructorSignatures,
                               MethodSignature... methodSignatures);

    String getKeywordsAndConstraints();

    void setKeywordsAndConstraints(String keywordsAndConstraints);

    void setLQLParseResult(LQLParseResult lqlParseResult);

    boolean isFullyQualified();
    void setFullyQualified(boolean fullyQualified);

    default String toMethodField(String field) {
        if(!isFullyQualified()) {
            if(StringUtils.contains(field, "Fq_")) {
                return StringUtils.replaceOnce(field, "Fq_", "_");
            }
        } else {
            if(!StringUtils.contains(field, "Fq_")) {
                return StringUtils.replaceOnce(field, "_", "Fq_");
            }
        }

        return field;
    }
}
