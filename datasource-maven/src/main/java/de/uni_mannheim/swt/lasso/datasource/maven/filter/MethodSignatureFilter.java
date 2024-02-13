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

import de.uni_mannheim.swt.lasso.index.filter.CandidateFilter;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenCodeUnitUtils;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.Clazz;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureFilter;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post filter based on strict type matching on all method signatures
 *
 * @author Marcus Kessel
 */
public class MethodSignatureFilter implements CandidateFilter {

    private static final Logger LOG = LoggerFactory
            .getLogger(MethodSignatureFilter.class);

    SignatureFilter signatureFilter = new SignatureFilter();

    @Override
    public boolean accept(CandidateDocument candidateDocument) {
        try {
            SolrCandidateDocument c = (SolrCandidateDocument) candidateDocument;
            CodeUnit impl = MavenCodeUnitUtils.toImplementation(c.getSolrDocument());

            boolean accepted = accept(SignatureUtils.create(impl));

            if(LOG.isDebugEnabled()) {
                LOG.debug("Filtering {} {}", candidateDocument.getId(), accepted);
            }

            return accepted;
        } catch (Throwable e) {
            LOG.warn(String.format("Could not apply filter on implementation '%s'", candidateDocument.getId()), e);

            // is OK
            return true;
        }
    }

    protected boolean accept(Clazz clazz) {
        return clazz.getMethods().stream().allMatch(signatureFilter::accept);
    }

//    protected boolean accept(Clazz clazz) {
//        return clazz.getMethods().stream().anyMatch(signatureFilter::accept); // at least one match
//    }

    public SignatureFilter getSignatureFilter() {
        return signatureFilter;
    }

    public void setSignatureFilter(SignatureFilter signatureFilter) {
        this.signatureFilter = signatureFilter;
    }

}
