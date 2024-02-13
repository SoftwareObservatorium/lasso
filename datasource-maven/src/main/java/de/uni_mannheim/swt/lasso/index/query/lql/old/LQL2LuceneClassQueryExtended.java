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

import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureExpansion;
import de.uni_mannheim.swt.lasso.datasource.expansion.wordnet.Thesaurus;
import de.uni_mannheim.swt.lasso.datasource.expansion.wordnet.Word;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy;
import de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import net.sf.extjwnl.JWNLException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Default candidate index query (supports free text searches as well as LQL
 * interface signatures searches).
 *
 * @author Marcus Kessel
 */
public class LQL2LuceneClassQueryExtended implements LQLLuceneStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(LQL2LuceneClassQueryExtended.class);

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

    public LQL2LuceneClassQueryExtended(SearchOptions searchOptions) {
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
                new String[]{"name_fq", "packagename_fq", "method_fq", "content"}, analyzer, LQL2LuceneClassQueryExtended.FIELD_BOOSTS);

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

    /**
     * Create an 'AND' like component query (Lucene {@link Query}).
     * <p>
     * Attention: This method assumes that query expansion (e.g. camel case
     * tokenization is done at SolR querying time). See SolR schema.xml used.
     *
     * @param className             Subject class name
     * @param constraints           Lucene constraints
     * @param constructorSignatures Constructor signatures
     * @param methodSignatures      Method signatures
     * @return Lucene component {@link Query}
     */
    protected Query createAndComponentQuery(String className, String constraints,
                                            MethodSignature[] constructorSignatures, MethodSignature[] methodSignatures) {

        // sanity check
        if (ArrayUtils.isEmpty(methodSignatures) && constraints == null) {
            return null;
        }

        // boolean query
        BooleanQuery.Builder interfaceSignatureQuery = new BooleanQuery.Builder();

        // XXX class name handling: camel casing is already conducted by Solr in terms of synonyms expansion

        List<BooleanQuery.Builder> thesaurusQueries = new LinkedList<>();
        List<BooleanQuery.Builder> typeExpansionQueries = new LinkedList<>();

        // methods
        if (ArrayUtils.isNotEmpty(methodSignatures)) {
            // method signatures
            for (MethodSignature methodSignature : methodSignatures) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Component method : " + methodSignature.toString());
                }

                // methodSignatureParamsOrderedSyntax
                TermQuery methodSignatureParamsOrderedSyntaxQuery = new TermQuery(
                        new Term(toMethodField("methodSignatureParamsOrderedSyntaxKeywordsFq_sigs"),
                                methodSignature.toStringParameterOrderedSyntax(isFullyQualified(), true)));
                //methodSignatureParamsOrderedSyntaxQuery.setBoost(2f);
                interfaceSignatureQuery.add(new BoostQuery(methodSignatureParamsOrderedSyntaxQuery, 2f), Occur.SHOULD);

                if (methodSignature.getPermutations() != null) {
                    for (MethodSignature alt : methodSignature.getPermutations()) {
                        TermQuery altMethodSignatureParamsOrderedSyntaxQuery = new TermQuery(
                                new Term(toMethodField("methodSignatureParamsOrderedSyntaxKeywordsFq_sigs"),
                                        alt.toStringParameterOrderedSyntax(isFullyQualified(), true)));
                        //altMethodSignatureParamsOrderedSyntaxQuery.setBoost(2f);
                        interfaceSignatureQuery.add(new BoostQuery(altMethodSignatureParamsOrderedSyntaxQuery, 2f), Occur.SHOULD);
                    }
                }

                // OR-query
                BooleanQuery.Builder aq = new BooleanQuery.Builder();

                // methodSignatureParamsOrdered
                aq.add(new TermQuery(new Term(toMethodField("methodSignatureParamsOrderedKeywordsFq_sigs"),
                        methodSignature.toStringParameterOrdered(isFullyQualified(), true))), Occur.SHOULD);

                // method
                TermQuery tq = new TermQuery(new Term("method_fqs", methodSignature.getMethodName()));
                //tq.setBoost(1.0f);
                aq.add(new BoostQuery(tq, 1.0f), Occur.SHOULD);

                //aq.setBoost(2.0f);
                interfaceSignatureQuery.add(new BoostQuery(aq.build(), 2.0f), Occur.SHOULD);

                // include inherited methods in search?
                if (searchOptions.isIncludeInheritedMethods()) {
                    TermQuery inheritedMethodSignatureParamsOrderedSyntaxQuery = new TermQuery(
                            new Term(toMethodField("inheritedmethodSignatureParamsOrderedSyntaxKeywordsFq_sigs"),
                                    methodSignature.toStringParameterOrderedSyntax(isFullyQualified(), true)));
                    //inheritedMethodSignatureParamsOrderedSyntaxQuery.setBoost(2f);

                    interfaceSignatureQuery.add(new BoostQuery(inheritedMethodSignatureParamsOrderedSyntaxQuery, 2f), Occur.SHOULD);

                    TermQuery inheritedMethodSignatureParamsOrderedKeywords = new TermQuery(new Term(toMethodField("inheritedmethodSignatureParamsOrderedKeywordsFq_sigs"),
                            methodSignature.toStringParameterOrdered(isFullyQualified(), true)));

                    if (methodSignature.getPermutations() != null) {
                        for (MethodSignature alt : methodSignature.getPermutations()) {
                            TermQuery altMethodSignatureParamsOrderedSyntaxQuery = new TermQuery(
                                    new Term(toMethodField("inheritedmethodSignatureParamsOrderedKeywordsFq_sigs"),
                                            alt.toStringParameterOrderedSyntax(true, true)));
                            //altMethodSignatureParamsOrderedSyntaxQuery.setBoost(2f);
                            interfaceSignatureQuery.add(new BoostQuery(altMethodSignatureParamsOrderedSyntaxQuery, 2f), Occur.SHOULD);
                        }
                    }

                    // FIXME here is something fishy
                    TermQuery inheritedMethods = new TermQuery(new Term("inheritedmethod_fqs", methodSignature.getMethodName()));
                    //inheritedMethods.setBoost(1f);
                    BooleanQuery.Builder inheritedAq = new BooleanQuery.Builder();
                    inheritedAq.add(inheritedMethodSignatureParamsOrderedKeywords, Occur.SHOULD);
                    inheritedAq.add(new BoostQuery(inheritedMethods, 1f), Occur.SHOULD);

                    //inheritedAq.setBoost(2f);

                    interfaceSignatureQuery.add(new BoostQuery(inheritedAq.build(), 2f), Occur.SHOULD);
                }

                // XXX what about 'methodSignatureParamsOrderedVisibility'?

                // FIXME query expansion
                boolean expand = false;
                String sigField = "methodSignatureParamsOrderedSyntaxFq_ssigs_sexact";

                if(expand) {
                    BooleanQuery.Builder sigBool = new BooleanQuery.Builder();

                    //sigBool.add(new TermQuery(new Term(sigField, sb.toString())), Occur.SHOULD);

                    SignatureExpansion queryExpansion = new SignatureExpansion();
                    List<MethodSignature> expansionList;
                    try {
                        expansionList = queryExpansion.expand(methodSignature);
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
                        typeExpansionQueries.add(sigBool);
                    }
                }

                // FIXME WordNet
                boolean thesaurus = false;

                if(thesaurus) {

                    BooleanQuery.Builder thesaurusQuery = new BooleanQuery.Builder();
                    // synonyms / antonyms
                    try {
                        Word word = Thesaurus.getVerbMeaningAsCamelCase(methodSignature.getMethodName());

                        if(word != null) {
                            // set limit
                            int limit = 5;
                            // synonyms
                            if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(word.getSynonyms())) {
                                int s = 0;
                                for(String syn : word.getSynonyms()) {
                                    if(s >= limit) {
                                        break;
                                    }

                                    thesaurusQuery.add(new TermQuery(new Term("method_fq", syn)), Occur.SHOULD);

                                    s++;
                                }
                            }

                            // antonyms
                            if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(word.getAntonyms())) {
                                int s = 0;
                                for(String ant : word.getAntonyms()) {
                                    if(s >= limit) {
                                        break;
                                    }

                                    TermQuery termQuery = new TermQuery(new Term("method_fq", ant));
                                    // FIXME boosting seem to have no effect here
                                    //Query boostedTermQuery = new BoostQuery(termQuery, 10);
                                    thesaurusQuery.add(termQuery, Occur.MUST_NOT);

                                    s++;
                                }
                            }
                        }
                    } catch (JWNLException e) {
                        if(LOG.isWarnEnabled()) {
                            LOG.warn("Synonyms failed for '{}'", methodSignature.getMethodName());
                            LOG.warn("Exception", e);
                        }
                    }

                    thesaurusQueries.add(thesaurusQuery);
                }
            }
        }

        // final query
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // validate constraints
        if (constraints == null) {
            constraints = "";
        }

        // add constraints
        QueryParser constraintsQueryParser = new QueryParser("content",
                new SimpleAnalyzer());
        constraintsQueryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
        try {
            query.add(interfaceSignatureQuery.build(), Occur.MUST);

            // thesaurus
            if(CollectionUtils.isNotEmpty(thesaurusQueries)) {
                thesaurusQueries.stream().filter(tq -> tq.build().clauses().size() > 0).peek(tq -> LOG.info("TSQ => {}", tq.build())).forEach(tq -> query.add(tq.build(), Occur.SHOULD));
            }

            // query expansion
            if(CollectionUtils.isNotEmpty(typeExpansionQueries)) {
                typeExpansionQueries.stream().filter(tq -> tq.build().clauses().size() > 0).peek(tq -> LOG.info("QEQ => {}", tq.build())).forEach(tq -> query.add(tq.build(), Occur.MUST));
            }

            // add constraints
            if (StringUtils.isNotBlank(constraints)) {
                query.add(constraintsQueryParser.parse(constraints), Occur.MUST);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("constraints added: " + constraints);
            }
        } catch (ParseException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not parse constraints: " + constraints, e);
            }
        }

        return query.build();
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
        // XXX strictly speaking, we have do ORing for constructors as well

        // are we dealing with a single method query? if yes, return default
        // strategy
//        if (methodSignatures != null && methodSignatures.length == 1) {
//            return createAndComponentQuery(className, constraints, constructorSignatures, methodSignatures);
//        }

        // original query minus constraints (we shift constraints to the end of
        // the entire query)
        Query originalQuery = createAndComponentQuery(className, null, constructorSignatures, methodSignatures);

        // ORing
        BooleanQuery.Builder q = new BooleanQuery.Builder();

        //------ paste from createAndComponentQuery

        TermQuery classNameTerm = new TermQuery(new Term("name_fq", className));
        //classNameTerm.setBoost(MQL_CLASS_NAME_BOOST);

        q.add(new BoostQuery(classNameTerm, LQL_CLASS_NAME_BOOST), Occur.SHOULD);

        // add constructor
        boolean addConstructor = true;
        if (addConstructor) {
            if (ArrayUtils.isNotEmpty(constructorSignatures)) {
                for (MethodSignature constructor : constructorSignatures) {
                    // methodSignatureParamsOrdered
                    q.add(new TermQuery(new Term(toMethodField("methodSignatureParamsOrderedKeywordsFq_sigs"),
                            constructor.toStringParameterOrdered(isFullyQualified(), true))), Occur.SHOULD);
                }
            }
        }

        // ---------

        if (originalQuery != null) {
            q.add(originalQuery, Occur.SHOULD);
        }

        if (methodSignatures != null) {
            // for each method
            for (MethodSignature signature : methodSignatures) {
                Query signatureQuery = createAndComponentQuery(className, null, constructorSignatures,
                        new MethodSignature[]{signature});

                if (signatureQuery != null) {
                    q.add(signatureQuery, Occur.SHOULD);
                }
            }
        }

        // add constraints at the end
        QueryParser qp = new QueryParser("content", new SimpleAnalyzer());
        qp.setDefaultOperator(QueryParser.AND_OPERATOR);
        try {
            // add constraints
            if (constraints != null && constraints.length() > 2) {
                q.add(qp.parse(constraints), Occur.MUST);
            }

            LOG.info("constraints added: " + constraints);
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
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
