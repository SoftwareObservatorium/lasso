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
package de.uni_mannheim.swt.lasso.index.filter;

import de.uni_mannheim.swt.lasso.index.match.SignatureMatch;
import de.uni_mannheim.swt.lasso.index.match.SignatureMatchFilter;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class CandidateFilterManager implements CandidateFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CandidateFilterManager.class);

    private final List<CandidateFilter> filters;
    private final List<SignatureMatchFilter> signatureMatchFilters;

    public CandidateFilterManager() {
        this(new LinkedList<>(), new LinkedList<>());
    }

    public CandidateFilterManager(List<CandidateFilter> filters, List<SignatureMatchFilter> signatureMatchFilters) {
        this.filters = filters;
        this.signatureMatchFilters = signatureMatchFilters;
    }

    @Override
    public boolean accept(CandidateDocument candidateDocument) {
        if(CollectionUtils.isNotEmpty(filters)) {
            for(CandidateFilter filter : filters) {
                LOG.debug("doing {}", candidateDocument.getFQName());
                try {
                    if(!filter.accept(candidateDocument)) {
                        if(LOG.isWarnEnabled()) {
                            LOG.warn("Filter reject => " + filter.getClass() + " / " + candidateDocument.getFQName());
                        }

                        return false;
                    }
                } catch (Exception e) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Filter failed => " + filter.getClass() + " / " + candidateDocument.getId());
                    }
                }
            }
        }

        if(CollectionUtils.isNotEmpty(signatureMatchFilters)) {
            // constructors
            List<SignatureMatch> constructorSignatureMatches = candidateDocument.getConstructorSignatureMatches();

            if(CollectionUtils.isNotEmpty(constructorSignatureMatches)) {
                Iterator<SignatureMatch> cit = constructorSignatureMatches.iterator();
                while(cit.hasNext()) {
                    SignatureMatch match = cit.next();

                    for (SignatureMatchFilter filter : signatureMatchFilters) {
                        if(!filter.accept(match, true)) {
                            cit.remove();
                            // end loop
                            break;
                        }
                    }
                }
            }

            // TODO kick out if no constructor?

            List<SignatureMatch> methodSignatureMatches = candidateDocument.getMethodSignatureMatches();
            if(CollectionUtils.isNotEmpty(methodSignatureMatches)) {
                Iterator<SignatureMatch> mit = methodSignatureMatches.iterator();
                while(mit.hasNext()) {
                    SignatureMatch match = mit.next();

                    for (SignatureMatchFilter filter : signatureMatchFilters) {
                        if(!filter.accept(match, false)) {
                            mit.remove();
                            // end loop
                            break;
                        }
                    }
                }
            }

            // empty after first step? kick out
            if(CollectionUtils.isEmpty(methodSignatureMatches)) {
                return false;
            }
        }

        return true;
    }

    public void addFilter(CandidateFilter candidateFilter) {
        this.filters.add(candidateFilter);
    }

    public void addFilter(SignatureMatchFilter signatureMatchFilter) {
        this.signatureMatchFilters.add(signatureMatchFilter);
    }
}
