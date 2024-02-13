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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Post filter based on same owner class (only applicable for Methods).
 *
 * @author Marcus Kessel
 */
public class SameOwnerClassFilter implements CandidateFilter {

    private static final Logger LOG = LoggerFactory
            .getLogger(SameOwnerClassFilter.class);

    Set<String> pids = new HashSet<>();

    @Override
    public boolean accept(CandidateDocument candidateDocument) {
        try {
            SolrCandidateDocument c = (SolrCandidateDocument) candidateDocument;

            if(StringUtils.equals("method", c.getDocType())) {
                Validate.notBlank(c.getParentId(), "Parent ID cannot be blank '%s'", c.getId());

                if(pids.contains(c.getParentId())) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Rejecting method '{}'", c.getId());
                    }

                    return false;
                }

                pids.add(c.getParentId());
            }

            return true;
        } catch (Throwable e) {
            LOG.warn(String.format("Could not apply filter on implementation '%s'", candidateDocument.getId()), e);

            // is OK
            return true;
        }
    }
}
