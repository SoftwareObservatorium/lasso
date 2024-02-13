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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.query.lql.old.LQL2LuceneClassQuery;
import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy;
import de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature;
import de.uni_mannheim.swt.lasso.datasource.expansion.wordnet.Thesaurus;
import de.uni_mannheim.swt.lasso.datasource.expansion.wordnet.Word;
import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import net.sf.extjwnl.JWNLException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Marcus Kessel
 */
public class ExactMethodSignatureQuery2 implements LQLLuceneStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ExactMethodSignatureQuery2.class);

//    static {
//        // double clause count
//        // org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024
//        BooleanQuery.setMaxClauseCount(BooleanQuery.getMaxClauseCount() * 2);
//    }

    protected LQLParseResult lqlParseResult;

    protected String keywordsAndConstraints;

    private final SearchOptions searchOptions;

    @Override
    public boolean isFullyQualified() {
        return fullyQualified;
    }

    @Override
    public void setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
    }

    private boolean fullyQualified;

    public ExactMethodSignatureQuery2(SearchOptions searchOptions) {
        //
        this.searchOptions = searchOptions;

        setKeywordsAndConstraints(searchOptions.getKeywordsAndConstraints());
    }

    /**
     * @return Lucene {@link Query}
     * @throws ParseException
     */
    @Override
    public Query getLuceneQuery() throws IOException {
        try {
            if (isLQL()) {
                return translateMqlToLuceneQuery();
            } else {
                return translateKeywordsAndConstraintsToLuceneQuery();
            }
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    /**
     * Freetext (Lucene compliant) query
     *
     * @return Lucene {@link Query}
     * @throws ParseException
     *             {@link MultiFieldQueryParser} error
     */
    protected Query translateKeywordsAndConstraintsToLuceneQuery() throws ParseException {
        return createLuceneKeywordsAndConstraintsQuery(keywordsAndConstraints);
    }

    /**
     * Freetext (Lucene compliant) query
     *
     * @param searchQuery
     *            Lucene query {@link String} for {@link MultiFieldQueryParser}.
     * @return Lucene {@link Query}
     * @throws ParseException
     *             {@link MultiFieldQueryParser} error
     */
    protected Query createLuceneKeywordsAndConstraintsQuery(String searchQuery) throws ParseException {
        // default analyzer
        StandardAnalyzer analyzer = new StandardAnalyzer();
        MultiFieldQueryParser qp = new MultiFieldQueryParser(
                new String[] { "name_fq", "packagename_fq", "method_fq", "content" }, analyzer, LQL2LuceneClassQuery.FIELD_BOOSTS);

        qp.setDefaultOperator(QueryParser.AND_OPERATOR);
        qp.setFuzzyMinSim(0.8f);
        qp.setAllowLeadingWildcard(true);

        // parse
        Query query = qp.parse(searchQuery);

        return query;
    }

    /**
     * @return true if MQL (interface signature query), false otherwise
     */
    @Override
    public boolean isLQL() {
        return lqlParseResult.getInterfaceSpecification() != null;
    }

    /**
     * @return true if no query at all
     */
    @Override
    public boolean isEmpty() {
        return !isLQL() && StringUtils.isBlank(keywordsAndConstraints);
    }

    /**
     * @return MQL interface signatures transformed to Lucene {@link Query}
     */
    protected Query translateMqlToLuceneQuery() {
        // MQL
        // currently, limited to first query
        LinkedList<Query> queries = new LinkedList<>();

        //for (SourceClass mClass : mqlQueryComponent.getClasses()) {
        Interface system = lqlParseResult.getInterfaceSpecification();
        List<MethodSignature> mSignatures = new LinkedList<>();
        List<MethodSignature> cSignatures = new LinkedList<>();
        String className = system.getName();

        // construct ordered signatures for searching
        List<de.uni_mannheim.swt.lasso.core.model.MethodSignature> methods = system.getMethods();
        if (methods != null && methods.size() > 0) {
            //mSignatures = new MethodSignature[methods.size()];
            for (int i = 0; i < methods.size(); i++) {
                de.uni_mannheim.swt.lasso.core.model.MethodSignature method = methods.get(i);
                if(!method.isConstructor()) {
                    mSignatures.add(new MethodSignature("public", method.getName(),
                            method.getInputs(), method.getOutputs().get(0)));
                } else {
                    cSignatures.add(new MethodSignature("public", "<init>",
                            method.getInputs(), "void"));
                }
            }
        }

        LOG.info("constraints " + keywordsAndConstraints);

        Query query = createComponentQuery(className, keywordsAndConstraints, cSignatures.toArray(new MethodSignature[0]), mSignatures.toArray(new MethodSignature[0]));

        queries.add(query);

        //LOG.info("Adding query "+ query.toString());
        //}

        return queries.getFirst();
    }

    /**
     * Creates an 'OR' like component query to support the concept of
     * 'Functional Sufficiency'.
     *
     * @param className             Subject class name
     * @param constraints           Lucene constraints
     * @param constructorSignatures Constructor signatures
     * @param methodSignatures      Method signatures
     * @return Lucene component {@link Query}
     */
    @Override
    public Query createComponentQuery(String className, String constraints, MethodSignature[] constructorSignatures,
                                      MethodSignature... methodSignatures) {

        if (methodSignatures.length > 1) {
            throw new UnsupportedOperationException("Only one method signature supported right now");
        }

        MethodSignature signature = methodSignatures[0];

//        PermutationIterator<String> permutationIterator = new PermutationIterator<>(signature.getParameterTypes());
//
//        List<MethodSignature> signatures = new LinkedList<>();
//        permutationIterator.forEachRemaining(l -> {
//            MethodSignature sig = new MethodSignature("public", signature.getMethodName(),
//                    l, signature.getReturnValue());
//
//            signatures.add(sig);
//        });

        // OR query
        BooleanQuery.Builder query = new BooleanQuery.Builder();//.setMinimumNumberShouldMatch(1); // ?

        boolean allowSubstituteNames = true;

        if(allowSubstituteNames) {
            BooleanQuery.Builder sigBool = new BooleanQuery.Builder();

            sigBool.add(new TermQuery(new Term("name_fq", className)), Occur.SHOULD);

            sigBool.add(new TermQuery(new Term("name_fq", signature.getMethodName())), Occur.SHOULD);

            query.add(sigBool.build(), Occur.SHOULD);
        } else {
            query.add(new TermQuery(new Term("name_fq", className)), Occur.SHOULD);
        }

        // add boost to naming?
        //query.add(new BoostQuery(new TermQuery(new Term("method_fq", signature.getMethodName())), 2f), Occur.SHOULD);



        boolean thesaurus = true;

        if(thesaurus) {
            BooleanQuery.Builder sigBool = new BooleanQuery.Builder();

            sigBool.add(new TermQuery(new Term("method_fq", signature.getMethodName())), Occur.SHOULD);

            if(allowSubstituteNames) {
                sigBool.add(new TermQuery(new Term("method_fq", className)), Occur.SHOULD);
            }

//            // synonyms
//            try {
//                List<String> synonyms = Thesaurus.getVerbSynonymsAsCamelCase(signature.getMethodName());
//
//                if(CollectionUtils.isNotEmpty(synonyms)) {
//                    for(String syn : synonyms) {
//                        sigBool.add(new TermQuery(new Term("method_fq", syn)), Occur.SHOULD);
//                    }
//                }
//            } catch (JWNLException e) {
//                if(LOG.isWarnEnabled()) {
//                    LOG.warn("Synonyms failed for '{}'", signature.getMethodName());
//                    LOG.warn("Exception", e);
//                }
//            }

            try {
                Word word = Thesaurus.getVerbMeaningAsCamelCase(signature.getMethodName());

                if(word != null) {
                    // set limit
                    int limit = 5;
                    // synonyms
                    if(CollectionUtils.isNotEmpty(word.getSynonyms())) {
                        int s = 0;
                        for(String syn : word.getSynonyms()) {
                            if(s >= limit) {
                                break;
                            }

                            sigBool.add(new TermQuery(new Term("method_fq", syn)), Occur.SHOULD);

                            s++;
                        }
                    }

                    // antonyms
                    if(CollectionUtils.isNotEmpty(word.getAntonyms())) {
                        int s = 0;
                        for(String ant : word.getAntonyms()) {
                            if(s >= limit) {
                                break;
                            }

                            TermQuery termQuery = new TermQuery(new Term("method_fq", ant));
                            // FIXME boosting seem to have no effect here
                            //Query boostedTermQuery = new BoostQuery(termQuery, 10);
                            sigBool.add(termQuery, Occur.MUST_NOT);

                            s++;
                        }
                    }
                }
            } catch (JWNLException e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Synonyms failed for '{}'", signature.getMethodName());
                    LOG.warn("Exception", e);
                }
            }

            query.add(sigBool.build(), Occur.SHOULD);
        } else {
            query.add(new TermQuery(new Term("method_fq", signature.getMethodName())), Occur.SHOULD);
        }

        // TODO add name concept or something?

//        signatures.stream().map(s -> {
//            StringBuilder sb = new StringBuilder();
//            sb.append("rv_")
//                    .append(s.getReturnValue().toLowerCase());
//
//            s.getParameterTypes().stream().forEach(p -> {
//                sb.append(";")
//                        .append("pt_")
//                        .append(p.toLowerCase());
//            });
//
//            return new TermQuery(new Term("methodSignatureParamsOrderedSyntaxFq_ssigs_sexact", sb.toString()));
//        }).forEach(t -> {
//            query.add(t, Occur.SHOULD);
//        });

        // ordered! (so no perms necessary!)
        List<String> orderedParams = new ArrayList<>(signature.getParameterTypes());
        signature.sortParameters(orderedParams);

        StringBuilder sb = new StringBuilder();
        sb.append("rv_")
                .append(signature.getReturnValue().toLowerCase());

        orderedParams.stream().forEach(p -> {
            sb.append(";")
                    .append("pt_")
                    .append(p.toLowerCase());
        });

        // QUERY EXPANSION
        boolean expand = true;

        String sigField = "methodSignatureParamsOrderedSyntaxFq_ssigs_sexact";

        if(expand) {
            BooleanQuery.Builder sigBool = new BooleanQuery.Builder();

            sigBool.add(new TermQuery(new Term(sigField, sb.toString())), Occur.SHOULD);

            SignatureExpansion queryExpansion = new SignatureExpansion();
            List<MethodSignature> expansionList;
            try {
                expansionList = queryExpansion.expand(signature);
            } catch (Exception e) {
                LOG.warn("Could not expand signature", e);

                expansionList = Collections.emptyList();
            }

            for(MethodSignature m : expansionList) {
                StringBuilder msb = new StringBuilder();
                msb.append("rv_")
                        .append(m.getReturnValue().toLowerCase());

                m.getParameterTypes().stream().forEach(p -> {
                    msb.append(";")
                            .append("pt_")
                            .append(p.toLowerCase());
                });

                //
                try {
                    sigBool.add(new TermQuery(new Term(sigField, msb.toString())), Occur.SHOULD);
                } catch (org.apache.lucene.search.BooleanQuery.TooManyClauses e) {
                    //e.printStackTrace();
                    LOG.warn("Query expansion: Could NOT set all clauses to query\n {}", expansionList.size());

                    break;
                }
            }

            if(!expansionList.isEmpty()) {
                query.add(sigBool.build(), Occur.MUST);
            }
        } else {
            // MUST occur
            query.add(new TermQuery(new Term(sigField, sb.toString())), Occur.MUST);
        }

        // add topic as content (guess from first camel case word) - assumption: remaining words too generic
        // e.g. Base64Utils --> [Base64, Utils] --> Base64
        // e.g. HTTPManager --> [HTTP, Manager] --> HTTP
        // class name
        String classTopic = Thesaurus.getFirstWordFromName(className);
        query.add(new TermQuery(new Term("content", classTopic)), Occur.SHOULD);
        String methodTopic = Thesaurus.getFirstWordFromName(signature.getMethodName());
        query.add(new TermQuery(new Term("content", methodTopic)), Occur.SHOULD);

        // add constraints at the end
        QueryParser qp = new QueryParser("content", new SimpleAnalyzer());
        qp.setDefaultOperator(QueryParser.AND_OPERATOR);
        try {
            // add constraints
            if (constraints != null && constraints.length() > 2) {
                query.add(qp.parse(constraints), Occur.MUST);
            }

            LOG.info("constraints added: " + constraints);
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }

        return query.build();
    }

    /**
     * @return the keywordsAndConstraints
     */
    @Override
    public String getKeywordsAndConstraints() {
        return keywordsAndConstraints;
    }

    /**
     * @param keywordsAndConstraints the keywordsAndConstraints to set
     */
    @Override
    public void setKeywordsAndConstraints(String keywordsAndConstraints) {
        if (StringUtils.isBlank(keywordsAndConstraints)) {
            return;
        }

        this.keywordsAndConstraints = keywordsAndConstraints;
    }

    @Override
    public void setLQLParseResult(LQLParseResult lqlParseResult) {
        this.lqlParseResult = lqlParseResult;
    }

    @Override
    public LQLParseResult getLQLParseResult() {
        return lqlParseResult;
    }
}
