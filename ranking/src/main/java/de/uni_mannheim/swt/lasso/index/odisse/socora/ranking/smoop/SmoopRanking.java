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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.helper.Slf4JConsole;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.perf.RankingPerformanceCollector;
import org.apache.commons.io.FilenameUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;

/**
 * Smoop-based ranking. Wraps smoop.js using Mozilla's Rhino {@link Context} and
 * {@link Scriptable}. We do not use sun internals Rhino as we have to rely on
 * internal classes that are not available at compile time.
 * 
 * JS files are loaded by default from classpath.
 * 
 * This class should be thread-safe.
 * 
 * @author Marcus Kessel
 *
 */
public class SmoopRanking {

    /**
     * @see sMoop.WS_DEFAULT
     */
    public static final String METHOD_WEIGHTED_SUM = "DEFAULT";

    /**
     * @see sMoop.WS_METRIC_EUCLIDEAN_DISTANCE
     */
    public static final String METHOD_WEIGHTED_METRIC = "EUCLIDEAN";

    private static final List<String> JS_FILES = Arrays.asList(
            "/js/libs/jstat.min.js", "/js/libs/lodash.min.js",
            "/js/libs/sylvester.js", "/js/smoop.js", "/js/ranking_bridge.js");

    private ScriptableObject scope = null;

    /**
     * Loading libs from classpath (classpath:/js/*.js)
     * 
     * @throws IOException
     *             I/O error with JS loading
     * 
     * @see #JS_FILES
     */
    public SmoopRanking() throws IOException {
        try {
            // local context (thread-dependent!)
            Context cx = Context.enter();

            // global scope can be shared
            scope = cx.initStandardObjects();

            // add console.log bridge to slf4j
            ScriptableObject.defineClass(scope, Slf4JConsole.class);
            ScriptableObject.defineProperty(scope, "console",
                    cx.newObject(scope, "Slf4JConsole"),
                    ScriptableObject.READONLY | ScriptableObject.PERMANENT);

            // add all js libs (load from classpath!)
            for (String jsFile : JS_FILES) {
                cx.evaluateReader(scope,
                        new BufferedReader(new InputStreamReader(
                                SmoopRanking.class.getResourceAsStream(jsFile),
                                StandardCharsets.UTF_8)), FilenameUtils
                                .getName(jsFile), 1, null);
            }

            // create smoop.js instance
            eval("var sMoopObj = new sMoop(true);");
        } catch (Throwable e) {
            throw new IOException("Could not initialize smoop.js", e);
        } finally {
            Context.exit();
        }
    }

    /**
     * @param javaScript
     *            Javascript to evaluate
     * @return some {@link Object} instance if value returned by JS
     */
    private Object eval(String javaScript) {
        try {
            Context cx = Context.enter();

            return cx.evaluateString(scope, javaScript, "<cmd>", 1, null);
        } finally {
            Context.exit();
        }
    }

    /**
     * Call a JavaScript function
     * 
     * @param functionName
     *            Function name
     * @param functionArgs
     *            Function arguments
     * @return some {@link Object} instance if value returned by JS
     */
    private Object call(String functionName, Object... functionArgs) {
        try {
            Context cx = Context.enter();

            Function function = (Function) scope.get(functionName, scope);

            Object result = function.call(cx, scope, scope, functionArgs);

            return result;
        } finally {
            Context.exit();
        }
    }

    /**
     * Single-objective (ranking method + score is set to
     * {@link CandidateItem#getRanking()})
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void single(Criterion[] criteria, CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // single ranking
            Object result = call("single", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException("Could not get result for single ranking", e);
        }
    }

    /**
     * Hybrid, non-dominated sorting (ranking method + score is set to
     * {@link CandidateItem#getRanking()}
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void hds(Criterion[] criteria, CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // HDS ranking
            Object result = call("hds", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException("Could not get result for HDS ranking", e);
        }
    }

    /**
     * Hybrid, non-dominated sorting extended with Reference Point distance
     * (ranking method + score is set to {@link CandidateItem#getRanking()}
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void rphds(Criterion[] criteria, CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // RPHDS ranking
            Object result = call("rphds", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException("Could not get result for RPHDS ranking", e);
        }
    }

    /**
     * Non-dominated sorting extended with Reference Point distance (ranking
     * method + score is set to {@link CandidateItem#getRanking()}
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void rpnds(Criterion[] criteria, CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // RPNDS ranking
            Object result = call("rpnds", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException("Could not get result for RPNDS ranking", e);
        }
    }

    /**
     * Non-dominated sorting (ranking method + score is set to
     * {@link CandidateItem#getRanking()}
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void nds(Criterion[] criteria, CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // NDS ranking
            Object result = call("nds", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException("Could not get result for NDS ranking", e);
        }
    }

    /**
     * Normalized weight distance to Reference Point (ranking method + score is
     * set to {@link CandidateItem#getRanking()}
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void normWeightDistance(Criterion[] criteria,
            CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // RPHDS ranking
            Object result = call("normWeightDistance", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException(
                    "Could not get result for normWeightDistance ranking", e);
        }
    }

    /**
     * Linear recursive sorting (ranking method + score is set to
     * {@link CandidateItem#getRanking()}
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void lrr(Criterion[] criteria, CandidateItem[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // LRR ranking
            Object result = call("lrr", (Object[]) criteria,
                    (Object[]) candidateItems, rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException("Could not get result for LRR ranking", e);
        }
    }

    /**
     * Weighted-sum (ranking method + score is set to
     * {@link CandidateItem#getRanking()})
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            array of {@link CandidateItem}s
     * @param weightingMethod
     *            {@link #METHOD_WEIGHTED_METRIC} or
     *            {@link #METHOD_WEIGHTED_SUM}
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @throws IOException
     *             I/O JavaScript execution
     */
    public void weightedSum(Criterion[] criteria,
            CandidateItem[] candidateItems, String weightingMethod,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        try {
            // weightedsum ranking
            Object result = call("weightedSum", (Object[]) criteria,
                    (Object[]) candidateItems, weightingMethod,
                    rankingPerformanceCollector);

            // return result;
        } catch (Throwable e) {
            throw new IOException(
                    "Could not get result for weightedSum ranking: "
                            + weightingMethod, e);
        }
    }
}
