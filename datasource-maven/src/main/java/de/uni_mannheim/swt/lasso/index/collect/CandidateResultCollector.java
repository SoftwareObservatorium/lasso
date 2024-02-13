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
package de.uni_mannheim.swt.lasso.index.collect;

import de.uni_mannheim.swt.lasso.index.filter.CandidateFilter;
import de.uni_mannheim.swt.lasso.index.filter.CandidateFilterManager;
import de.uni_mannheim.swt.lasso.index.filter.MavenInvalidIdFilter;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Candidate result collector
 *
 * @author Marcus Kessel
 */
public class CandidateResultCollector {

    private static final Logger LOG = LoggerFactory
            .getLogger(CandidateResultCollector.class);

    protected SolrClient solrClient;
    protected SolrQuery solrQuery;

    private boolean collapseBySimilarNaming = false;

    protected long total = -1;

    public static int FETCH_SIZE = 1000;

    /**
     * Fetch more (or even less) than required (this is a good option to increase efficiency in post filters)
     */
    @Deprecated
    protected double rowSizeOverflow = 1.0;
    /**
     * Minimum cursor size
     */
    @Deprecated
    protected int minimumCursorSize = 50;
    /**
     * How many empty rounds before we stop querying
     */
    @Deprecated
    private int maxRounds = 10;

    /**
     * Generic filters always applied.
     */
    protected CandidateFilter genericFilter = new CandidateFilterManager(
            Arrays.asList(new MavenInvalidIdFilter()), null);

    /**
     * Constructor
     *
     * @param solrClient {@link SolrClient} instance
     * @param solrQuery  {@link SolrQuery} instance
     */
    public CandidateResultCollector(SolrClient solrClient, SolrQuery solrQuery) {
        this.solrClient = solrClient;

        this.solrQuery = solrQuery;
    }

    public List<CandidateDocument> collect(int page, int rows, CandidateFilter candidateFilter) throws IOException {
        return collect(page, rows, candidateFilter, null, null);
    }

    public List<CandidateDocument> collect(int page, int rows, CandidateFilter candidateFilter, DocumentHandler documentHandler) throws IOException {
        return collect(page, rows, candidateFilter, documentHandler, null);
    }

    /**
     * Collect
     *
     * @param page            cursor size
     * @param rows            total rows to fetch
     * @param candidateFilter
     * @param documentHandler
     * @param comparator
     * @return
     * @throws IOException
     * @see <a href="https://lucene.apache.org/solr/guide/7_4/pagination-of-results.html">Solr Cursor</a>
     */
    public List<CandidateDocument> collect(int page, int rows, CandidateFilter candidateFilter, DocumentHandler documentHandler, Comparator<CandidateDocument> comparator) throws IOException {
        try {
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;

            int fetchSize = FETCH_SIZE;
            int remove = -1;
            int lastTotal = -1;

            // adjust to rows if page size larger than total row size
            if(page > rows) {
                page = rows;
            }

            SolrDocumentList candidates = new SolrDocumentList();

            while (!done) {
                // use fixed page size
                int cursorSize = fetchSize;

                //
                if(rows == 1) {
                    cursorSize = rows;
                }

                if (LOG.isInfoEnabled()) {
                    LOG.info("Cursor size adapted to " + cursorSize);
                }

                solrQuery.setRows(cursorSize);
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse rsp = solrClient.query(solrQuery, SolrRequest.METHOD.POST);
                String nextCursorMark = rsp.getNextCursorMark();

                if(LOG.isInfoEnabled()) {
                    LOG.info(String.format("No. of total results %s", rsp.getResults().getNumFound()));
                }

                processRawResults(candidates, rsp, candidateFilter, documentHandler, comparator);

                int total = candidates.size();

                if (total >= rows) {
                    done = true;

                    remove = total - rows;
                }

                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }

                cursorMark = nextCursorMark;
                lastTotal = total;

                if(LOG.isInfoEnabled()) {
                    LOG.info("Current no. of candidates '{}'", candidates.size());
                }
            }

            Stream<CandidateDocument> stream = candidates.stream()
                    .map(SolrCandidateDocument::new);

//            // generic filtering
//            if (genericFilter != null) {
//                stream = stream.filter(genericFilter::accept);
//            }
//
//            // custom visiting (e.g altering etc.)
//            if (documentHandler != null) {
//                stream = stream.peek(documentHandler::handle);
//            }
//
//            // user filtering
//            if (candidateFilter != null) {
//                stream = stream.filter(candidateFilter::accept);
//            }

            // custom sorting
            if (comparator != null) {
                stream = stream.sorted(comparator);
            }

            if (remove > 0) {
                //candidates = candidates.subList(0, candidates.size() - remove);

                return stream.limit(candidates.size() - remove).collect(Collectors.toList());
            } else {
                return stream.collect(Collectors.toList());
            }
        } catch (Throwable e) {
            throw new IOException("Candidates query failed", e);
        }
    }

    private SolrDocumentList processRawResults(SolrDocumentList existingResult, QueryResponse response, CandidateFilter candidateFilter, DocumentHandler documentHandler, Comparator<CandidateDocument> comparator) {
        // results
        SolrDocumentList results = response.getResults();

        if (total < 0) {
            total = results.getNumFound();
        }

        // -- apply filtering
        // now start with processing
        Stream<CandidateDocument> stream = results.stream()
                .map(SolrCandidateDocument::new);

        // generic filtering
        if (genericFilter != null) {
            stream = stream.filter(genericFilter::accept);
        }

        // custom visiting (e.g altering etc.)
        if (documentHandler != null) {
            stream = stream.peek(documentHandler::handle);
        }

        // user filtering
        if (candidateFilter != null) {
            stream = stream.filter(candidateFilter::accept);
        }
        // --

        results = stream.map(d -> ((SolrCandidateDocument)d).getSolrDocument()).collect(Collectors.toCollection(SolrDocumentList::new));

        if(LOG.isInfoEnabled()) {
            LOG.info(String.format("results %s", results.size()));
        }

        // has expanded results?
        boolean expanded = response.getExpandedResults() != null;
        if(expanded) {
            // lookup field {!collapse field=nvuri_s nullPolicy=expand}
            expand(response, candidateFilter, documentHandler);
        }

        // merge results
        existingResult.addAll(results);

        if(LOG.isInfoEnabled()) {
            LOG.info(String.format("existingResult results %s", existingResult.size()));
        }

//        // now collapse by hash
//        collapseByHashClone(existingResult);

        if(LOG.isInfoEnabled()) {
            LOG.info(String.format("existingResult results %s", existingResult.size()));
        }

        // now find similar
        if(isCollapseBySimilarNaming()) {
            collapseBySimilarNaming(existingResult);
        }

        if(LOG.isInfoEnabled()) {
            LOG.info(String.format("existingResult results %s", existingResult.size()));
        }

        if(LOG.isInfoEnabled()) {
            LOG.info(String.format("existingResult results %s", existingResult.size()));
        }

        return existingResult;
    }
    /**
     * Expand each doc (if possible) for possible alternatives.
     *
     * e.g. {!collapse field=uri nullPolicy=expand}
     *
     * @param response
     * @param candidateFilter
     * @param documentHandler
     */
    protected void expand(QueryResponse response, CandidateFilter candidateFilter, DocumentHandler documentHandler) {
        SolrDocumentList results = response.getResults();

        // lookup field {!collapse field=uri nullPolicy=expand}
        Optional<String> collapseFilter = Arrays.stream(solrQuery.getFilterQueries()).filter(filter -> StringUtils.startsWith(filter.trim(), "{!collapse field=")).findAny();

        String collapseField = StringUtils.substringBetween(collapseFilter.get(), "field=", " ");

        if(LOG.isInfoEnabled()) {
            LOG.info("collapse field: " + collapseField + ". Results " + response.getExpandedResults().size());
        }

        results.stream().forEach(doc -> {
            if(doc.containsKey(collapseField)) {
                String docFieldValue = (String) doc.getFirstValue(collapseField);
                if(StringUtils.isNotEmpty(docFieldValue)) {
                    SolrDocumentList alternativeList = response.getExpandedResults().get(docFieldValue);
                    if(CollectionUtils.isNotEmpty(alternativeList)) {
                        if(LOG.isInfoEnabled()) {
                            LOG.info("Found alternatives for " + docFieldValue);
                        }

                        // -- apply filtering
                        // now start with processing
                        Stream<CandidateDocument> stream = alternativeList.stream()
                                .map(SolrCandidateDocument::new);

                        // generic filtering
                        if (genericFilter != null) {
                            stream = stream.filter(genericFilter::accept);
                        }

                        // custom visiting (e.g altering etc.)
                        if (documentHandler != null) {
                            stream = stream.peek(documentHandler::handle);
                        }

                        // user filtering
                        if (candidateFilter != null) {
                            stream = stream.filter(candidateFilter::accept);
                        }

                        alternativeList = stream.map(d -> ((SolrCandidateDocument)d).getSolrDocument()).collect(Collectors.toCollection(SolrDocumentList::new));

                        // set alternatives
                        if(CollectionUtils.isNotEmpty(alternativeList)) {
                            doc.put("alternatives", alternativeList);
                        }
                    }
                }
            }
        });
    }

//    /**
//     *
//     *
//     * @param results
//     */
//    protected void collapseByHashClone(SolrDocumentList results) {
//        // try to avoid to much memory consumption and use inline updates
//        Map<String, SolrDocument> byHash = new HashMap<>();
//        Iterator<SolrDocument> it = results.iterator();
//        while(it.hasNext()) {
//            SolrDocument doc = it.next();
//
//            String hash = (String) doc.getFirstValue("hash");
//
//            // FIXME problematic if "hash" is null
//
//            if(!byHash.containsKey(hash)) {
//                byHash.put(hash, doc);
//            } else {
//                SolrDocument masterDoc = byHash.get(hash);
//                if(!masterDoc.containsKey("clone")) {
//                    masterDoc.put("clone", new SolrDocumentList());
//                }
//
//                // ordered implicitly by "score"
//                SolrDocumentList clones = (SolrDocumentList) masterDoc.get("clone");
//                clones.add(doc);
//
//                // finally, remove from List
//                it.remove();
//            }
//        }
//    }

    /**
     *
     *
     * @param results
     */
    protected void collapseBySimilarNaming(SolrDocumentList results) {
        // try to avoid to much memory consumption and use inline updates
        Map<String, SolrDocument> byHash = new HashMap<>();
        Iterator<SolrDocument> it = results.iterator();
        while(it.hasNext()) {
            SolrDocument doc = it.next();

            // use signature
            String fieldValue = (String) doc.getFirstValue("bytecodename_s");

            if(!byHash.containsKey(fieldValue)) {
                byHash.put(fieldValue, doc);
            } else {
                SolrDocument masterDoc = byHash.get(fieldValue);
                if(!masterDoc.containsKey("similar")) {
                    masterDoc.put("similar", new SolrDocumentList());
                }

                // ordered implicitly by "score"
                SolrDocumentList similar = (SolrDocumentList) masterDoc.get("similar");
                similar.add(doc);

                // finally, remove from List
                it.remove();
            }
        }
    }

    public long getTotal() {
        return total;
    }

    public void setRowSizeOverflow(double rowSizeOverflow) {
        this.rowSizeOverflow = rowSizeOverflow;
    }

    public void setMinimumCursorSize(int minimumCursorSize) {
        this.minimumCursorSize = minimumCursorSize;
    }

    public CandidateFilter getGenericFilter() {
        return genericFilter;
    }

    public void setGenericFilter(CandidateFilter genericFilter) {
        this.genericFilter = genericFilter;
    }

    public boolean isCollapseBySimilarNaming() {
        return collapseBySimilarNaming;
    }

    public void setCollapseBySimilarNaming(boolean collapseBySimilarNaming) {
        this.collapseBySimilarNaming = collapseBySimilarNaming;
    }

    /**
     * Document handler.
     *
     * @author Marcus Kessel
     */
    public interface DocumentHandler {
        void handle(CandidateDocument candidateDocument);
    }
}
