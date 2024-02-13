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

import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureExpansion;

import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default candidate index query (supports free text searches as well as LQL
 * interface signatures searches).
 *
 * @author Marcus Kessel
 */
public class LQL2LuceneClassQueryBareBone implements LQLLuceneStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(LQL2LuceneClassQueryBareBone.class);

    /**
     * Default field boosts (basically taken and adapted from old merobase
     * heuristics).
     */
    public static final Map<String, Float> FIELD_BOOSTS = new HashMap<String, Float>();

    static {
        FIELD_BOOSTS.put("name_fq", 5f);
        // FIELD_BOOSTS.put("interface", 3f);
        // FIELD_BOOSTS.put("superclass", 3f);
        FIELD_BOOSTS.put("packagename_fq", 4f);
        FIELD_BOOSTS.put("method_fq", 1f);
        FIELD_BOOSTS.put("content", 0.5f);
    }

    /**
     * Class name boost for LQL Lucene {@link Query}s
     */
    protected static final float LQL_CLASS_NAME_BOOST = 2.5f;

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

    public LQL2LuceneClassQueryBareBone(SearchOptions searchOptions) {
        this.searchOptions = searchOptions;

        setKeywordsAndConstraints(searchOptions.getKeywordsAndConstraints());
    }

    @Override
    public LQLParseResult getLQLParseResult() {
        return lqlParseResult;
    }

    /**
     * @return Lucene {@link Query}
     * @throws ParseException
     */
    @Override
    public Query getLuceneQuery() throws IOException {
        try {
            if (isLQL()) {
                return translateLQLToLuceneQuery();
            } else {
                return translateKeywordsAndConstraintsToLuceneQuery();
            }
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    /**
     * @return true if LQL (interface signature query), false otherwise
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
     * Freetext (Lucene compliant) query
     *
     * @return Lucene {@link Query}
     * @throws ParseException {@link MultiFieldQueryParser} error
     */
    protected Query translateKeywordsAndConstraintsToLuceneQuery() throws ParseException {
        return createLuceneKeywordsAndConstraintsQuery(keywordsAndConstraints);
    }

    /**
     * Freetext (Lucene compliant) query
     *
     * @param searchQuery Lucene query {@link String} for {@link MultiFieldQueryParser}.
     * @return Lucene {@link Query}
     * @throws ParseException {@link MultiFieldQueryParser} error
     */
    protected Query createLuceneKeywordsAndConstraintsQuery(String searchQuery) throws ParseException {
        // default analyzer
        StandardAnalyzer analyzer = new StandardAnalyzer();
        MultiFieldQueryParser qp = new MultiFieldQueryParser(
                new String[]{"name_fq", "packagename_fq", "method_fq", "content"}, analyzer, LQL2LuceneClassQueryBareBone.FIELD_BOOSTS);

        qp.setDefaultOperator(QueryParser.AND_OPERATOR);
        qp.setFuzzyMinSim(0.8f);
        qp.setAllowLeadingWildcard(true);

        // parse
        Query query = qp.parse(searchQuery);

        return query;
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
        List<MethodSignature> mSignatures = new LinkedList<>();
        List<MethodSignature> cSignatures = new LinkedList<>();
        String className = system.getName();

        // construct ordered signatures for searching
        List<de.uni_mannheim.swt.lasso.core.model.MethodSignature> methods = system.getMethods();
        if (methods != null && methods.size() > 0) {
            //mSignatures = new MethodSignature[methods.size()];
            for (int i = 0; i < methods.size(); i++) {
                de.uni_mannheim.swt.lasso.core.model.MethodSignature method = methods.get(i);
                if (!method.isConstructor()) {
                    mSignatures.add(new MethodSignature("public", method.getName(),
                            method.getInputs(), method.getOutputs().get(0)));
                } else {
                    cSignatures.add(new MethodSignature("public", "<init>",
                            method.getInputs(), "void"));
                }
            }
        }

        Query query = createComponentQuery(className, keywordsAndConstraints, cSignatures.toArray(new MethodSignature[0]), mSignatures.toArray(new MethodSignature[0]));

        queries.add(query);

        return queries.getFirst();
    }

    @Override
    public Query createComponentQuery(String className, String constraints, MethodSignature[] constructorSignatures,
                                      MethodSignature... methodSignatures) {
        //
        List<MethodSignature> all = new LinkedList<>();
        if(ArrayUtils.isNotEmpty(constructorSignatures)) {
            all.addAll(Arrays.asList(constructorSignatures));
        }
        if(ArrayUtils.isNotEmpty(methodSignatures)) {
            all.addAll(Arrays.asList(methodSignatures));
        }

        // ORing
        BooleanQuery.Builder q = new BooleanQuery.Builder();

        // single terms
        q.add(new TermQuery(new Term("content", className)), Occur.SHOULD);

        int slop = 10; // edit distance

        // -- 2. relaxed, signature only (without naming)
        BooleanQuery.Builder mRelaxed = new BooleanQuery.Builder();

        if(CollectionUtils.isNotEmpty(all)) {
            for (MethodSignature method : all) {
                mRelaxed.add(new PhraseQuery.Builder().setSlop(slop).add(new Term(toMethodField("methodSignatureParamsOrderedKeywordsFq_sigs"),
                        method.toStringParameterOrderedSyntax(isFullyQualified(), true))).build(), Occur.MUST);

                if(!StringUtils.equalsAnyIgnoreCase("<init>", method.getMethodName())) {
                    q.add(new TermQuery(new Term("content", method.getMethodName())), Occur.SHOULD);
                }
            }
        }

        // boost
        q.add(new BoostQuery(mRelaxed.build(), 5f), Occur.SHOULD);

        // different strategy, showing desired types
        int typesExp = 1; //
        if(typesExp > 0) {
            if(ArrayUtils.isNotEmpty(methodSignatures)) {
                BooleanQuery.Builder expBool = new BooleanQuery.Builder();
                SignatureExpansion queryExpansion = new SignatureExpansion();

                for (MethodSignature method : methodSignatures) {
                    List<MethodSignature> expansionList;
                    try {
                        expansionList = queryExpansion.expand(method);
                    } catch (Exception e) {
                        LOG.warn("Could not expand signature", e);

                        expansionList = Collections.emptyList();
                    }

                    if(CollectionUtils.isNotEmpty(expansionList)) {
                        // collect all inputs/outputs
                        Set<String> inputs = expansionList.stream().flatMap(m -> m.getParameterTypes().stream()).collect(Collectors.toSet());
                        Set<String> returns = expansionList.stream().map(m -> m.getReturnValue()).collect(Collectors.toSet());

                        BooleanQuery.Builder ins = new BooleanQuery.Builder();
                        for(String in : inputs) {
                            try {
                                ins.add(new TermQuery(new Term(toMethodField("methodSignatureParamsOrderedKeywordsFq_sigs"),
                                        "pt_" + in)), Occur.SHOULD);
                            } catch (BooleanQuery.TooManyClauses e) {
                                //e.printStackTrace();
                                LOG.warn("Query expansion: Could NOT set all clauses to query\n {}", expansionList.size());

                                break;
                            }
                        }

                        BooleanQuery.Builder outs = new BooleanQuery.Builder();
                        for(String out : returns) {
                            try {
                                outs.add(new TermQuery(new Term(toMethodField("methodSignatureParamsOrderedKeywordsFq_sigs"),
                                        "rv_" + out)), Occur.SHOULD);
                            } catch (BooleanQuery.TooManyClauses e) {
                                //e.printStackTrace();
                                LOG.warn("Query expansion: Could NOT set all clauses to query\n {}", expansionList.size());

                                break;
                            }
                        }

                        if(CollectionUtils.isNotEmpty(inputs)) {
                            expBool.add(ins.build(), Occur.MUST);
                        }

                        if(CollectionUtils.isNotEmpty(returns)) {
                            expBool.add(outs.build(), Occur.MUST);
                        }
                    }
                }

                BooleanQuery expBoolQ = expBool.build();
                if(CollectionUtils.isNotEmpty(expBoolQ.clauses())) {
                    q.add(new BoostQuery(expBoolQ, 0.2f), Occur.SHOULD);
                }
            }
        }

        return q.build();
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
        this.keywordsAndConstraints = keywordsAndConstraints;
    }

    @Override
    public void setLQLParseResult(LQLParseResult lqlParseResult) {
        this.lqlParseResult = lqlParseResult;
    }
}
