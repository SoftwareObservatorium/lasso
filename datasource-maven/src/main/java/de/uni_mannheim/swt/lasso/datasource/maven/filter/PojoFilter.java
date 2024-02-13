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
package de.uni_mannheim.swt.lasso.datasource.maven.filter;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenCodeUnitUtils;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.Clazz;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import de.uni_mannheim.swt.lasso.index.filter.CandidateFilter;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcus Kessel
 */
public class PojoFilter implements CandidateFilter {

    private static final Logger LOG = LoggerFactory
            .getLogger(PojoFilter.class);

    @Override
    public boolean accept(CandidateDocument candidateDocument) {
        try {
            SolrCandidateDocument c = (SolrCandidateDocument) candidateDocument;
            CodeUnit impl = MavenCodeUnitUtils.toImplementation(c.getSolrDocument());

            boolean accepted = accept(SignatureUtils.create(impl));

            if(!accepted && LOG.isWarnEnabled()) {
                LOG.warn("Dropping '{}'", candidateDocument.getId());
            }

            return accepted;
        }catch (Throwable e) {
            LOG.warn("Pojo filter failed for " + candidateDocument.getId(), e);
        }

        return true;
    }

    private boolean accept(Clazz clazz) {
        return !clazz.getMethods().stream()
                // filter out java.lang.Object
                .filter(m -> !(m.getInputTypes().size() == 0 && StringUtils.equalsAnyIgnoreCase(m.getName(), "toString", "hashCode")))
                .filter(m -> !(m.getInputTypes().size() == 1 && StringUtils.equalsAnyIgnoreCase(m.getName(), "equals")))
                .allMatch(m -> StringUtils.startsWithAny(m.getName().toLowerCase(), "set", "get", "is"));
    }
}
