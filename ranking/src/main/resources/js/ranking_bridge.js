/**
* Copyright 2015 Marcus Kessel
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
* Bridging Library between Java and 'smoop' for the SOCORA ranking engine.
*
* @author Marcus Kessel (kessel@informatik.uni-mannheim.de)
* @version 0.0.1
*/

// selected metrics to problem
toProblem = function(selectedMetrics) {
	var objectives = [];
	var weights = [];
	var priorities = [];
	var ids = [];
	for (var j = 0; j < selectedMetrics.length; j++) {
		objectives.push(selectedMetrics[j].objective);
		weights.push(selectedMetrics[j].weight);
		priorities.push(selectedMetrics[j].priority);
		ids.push(selectedMetrics[j].id);
	}

	return {
		'objectives' : objectives,
		'weights' : weights,
		'priorities' : priorities,
		'ids' : ids
	};
};

// selected metrics and components to points
toPoints = function(selectedMetrics, components) {
	// incomplete components where at least on measure is missing (=undefined)
	var incompleteComponents = [];

	// metric measures (with selectedMetrics.length dimensions)
	var points = [];
	// bookkeeping :-)
	var componentMap = {};
	// collect measures
	for (var i = 0; i < components.length; i++) {
		// point with selectedMetrics.length dimensions
		var point = [];
		for (var j = 0; j < selectedMetrics.length; j++) {
			var measure = parseFloat(components[i].metrics[selectedMetrics[j].id]);

			if (isNaN(measure)) {
				// console.log("Could not find measure " + selectedMetrics[j].id
				// + " for component " + components[i].id);
				// if undefined, continue
				break;
			}

			// add
			point.push(measure);
		}

		// in case we were not able to get all measures for component i
		if (point.length < selectedMetrics.length) {
			// skip this component
			incompleteComponents.push(components[i]);

			continue;
		}

		// measures
		points.push(point);

		// keep track for fast access
		var cKey = point.toString();

		var entry = componentMap[cKey];
		if (!entry) {
			// add
			entry = [];

			componentMap[cKey] = entry;
		}

		entry.push(components[i]);
	}

	return {
		'incomplete' : incompleteComponents,
		'points' : points,
		'lookup' : componentMap
	};
};

// compute hybrid, non-dominated sorting with ranked objectives
hds = function(selectedMetrics, components, rankingPerformanceCollector) {
	// problem description
	var problem = this.toProblem(selectedMetrics);

	// to points
	var pointsObj = this.toPoints(selectedMetrics, components);

	// collector for set size
	var rankingAnalysisCollectorObj = new rankingAnalysisCollector();

	// compute non-dominated sorting, index = 0, level 1, index = 1 level 2 etc.
	problem.points = pointsObj.points;
	// NDS + ranked objectives
	var P = sMoopObj
			.nonDominatedSortingRO(problem, rankingAnalysisCollectorObj);

	var rankingId = 'HDS_SMOOP';

	// sorted points, join all non-dominated sets
	var sortedPoints = [];
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		for (var j = 0; j < pi.length; j++) {
			sortedPoints.push(pi[j]);
		}
	}

	// sanity check
	if (sortedPoints.length != problem.points.length) {
		throw "Returned points of HDS not equal to original points [is/was] = "
				+ sortedPoints.length + "/" + problem.points.length;
	}

	// add score to each component
	score(pointsObj, sortedPoints, rankingId);

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, rankingAnalysisCollectorObj.nonDistinguishableSets(),
			rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// perf
		// setRankingPerf(rankingPerformanceCollector,
		// rankingAnalysisCollectorObj
		// .nonDistinguishableSets(), sortedPoints, problem.objectives,
		// rankingId, rankingAnalysisCollectorObj.diversityCount());

		//
		setPerformanceMeasures(rankingId, pointsObj, problem,
				rankingAnalysisCollectorObj.nonDistinguishableSets(),
				sortedPoints, rankingPerformanceCollector);
	}
};

// reference point + distance + compute hybrid, non-dominated sorting with
// ranked objectives
rphds = function(selectedMetrics, components, rankingPerformanceCollector) {
	// problem description
	var problem = this.toProblem(selectedMetrics);

	// to points
	var pointsObj = this.toPoints(selectedMetrics, components);

	// collector for set size
	var rankingAnalysisCollectorObj = new rankingAnalysisCollector();

	// compute non-dominated sorting, index = 0, level 1, index = 1 level 2 etc.
	problem.points = pointsObj.points;
	// add pointsObj to problem
	problem.lookupObj = pointsObj;

	// NDS + ranked objectives + RP + distance
	var P = sMoopObj.rphds(problem, rankingAnalysisCollectorObj);

	var rankingId = 'RP_HDS_SMOOP';

	// sorted points, join all non-dominated sets
	var sortedPoints = [];
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		for (var j = 0; j < pi.length; j++) {
			sortedPoints.push(pi[j]);
		}
	}

	// sanity check
	if (sortedPoints.length != problem.points.length) {
		throw "Returned points of RPHDS not equal to original points [is/was] = "
				+ sortedPoints.length + "/" + problem.points.length;
	}

	// add relevance score (RP distance penalty!)
	sMoopObj.setReferencePointDistanceScore(pointsObj, sortedPoints,
			problem.objectives);

	// add penalty factor to subsets
	var newestLevelP = rankingAnalysisCollectorObj.nonDistinguishableSets();
	var sortedPointsPenalty = [];
	for (var i = 0; i < newestLevelP.length; i++) {
		var pi = newestLevelP[i];

		// sort non-distinguishable subset by rpDistance penalty factor (min
		// objective: minimize
		// penalty)
		// sort pi in ascending order by rpDistanceScore
		var sortedPi = pi.sort(function(a, b) {
			// c1, first candidate (all duplicates with equal measures)
			var component1 = problem.lookupObj.lookup[a][0];
			// c2, first candidate (all duplicates with equal measures)
			var component2 = problem.lookupObj.lookup[b][0];

			// min ascending
			return component1.ranking['rpDistanceScore']
					- component2.ranking['rpDistanceScore'];
		});

		for (var j = 0; j < sortedPi.length; j++) {
			sortedPointsPenalty.push(sortedPi[j]);
		}
	}

	// add score to each component
	score(pointsObj, sortedPointsPenalty, rankingId);

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, newestLevelP, rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector,
		// rankingAnalysisCollectorObj
		// .nonDistinguishableSets(), sortedPointsPenalty,
		// problem.objectives, rankingId, rankingAnalysisCollectorObj
		// .diversityCount());

		//
		setPerformanceMeasures(rankingId, pointsObj, problem,
				rankingAnalysisCollectorObj.nonDistinguishableSets(),
				sortedPointsPenalty, rankingPerformanceCollector);
	}
};

// non-dominated sorting (plain)
nds = function(selectedMetrics, components, rankingPerformanceCollector) {
	// problem description
	var problem = this.toProblem(selectedMetrics);

	// to points
	var pointsObj = this.toPoints(selectedMetrics, components);

	// compute non-dominated sorting, index = 0, level 1, index = 1 level 2 etc.
	problem.points = pointsObj.points;
	// add pointsObj to problem
	problem.lookupObj = pointsObj;

	// NDS
	var P = sMoopObj.nonDominatedSorting(problem);

	var rankingId = 'NDS_SMOOP';

	// sorted points, join all non-dominated sets
	var sortedPoints = [];
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		for (var j = 0; j < pi.length; j++) {
			sortedPoints.push(pi[j]);
		}
	}

	// sanity check
	if (sortedPoints.length != problem.points.length) {
		throw "Returned points of NDS not equal to original points [is/was] = "
				+ sortedPoints.length + "/" + problem.points.length;
	}

	// add score to each component
	score(pointsObj, sortedPoints, rankingId);

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, P, rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector, P, sortedPoints,
		// problem.objectives, rankingId);

		//
		setPerformanceMeasures(rankingId, pointsObj, problem, P, sortedPoints,
				rankingPerformanceCollector);
	}
};

rpnds = function(selectedMetrics, components, rankingPerformanceCollector) {
	// problem description
	var problem = this.toProblem(selectedMetrics);

	// to points
	var pointsObj = this.toPoints(selectedMetrics, components);

	// compute non-dominated sorting, index = 0, level 1, index = 1 level 2 etc.
	problem.points = pointsObj.points;
	// add pointsObj to problem
	problem.lookupObj = pointsObj;

	// NDS + RP + distance
	var P = sMoopObj.nonDominatedSorting(problem);

	var rankingId = 'RP_NDS_SMOOP';

	// sorted points, join all non-dominated sets
	var sortedPoints = [];
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		for (var j = 0; j < pi.length; j++) {
			sortedPoints.push(pi[j]);
		}
	}

	// sanity check
	if (sortedPoints.length != problem.points.length) {
		throw "Returned points of RPNDS not equal to original points [is/was] = "
				+ sortedPoints.length + "/" + problem.points.length;
	}

	// add relevance score (RP distance penalty!)
	sMoopObj.setReferencePointDistanceScore(pointsObj, sortedPoints,
			problem.objectives);

	var sortedPointsPenalty = [];
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		// sort non-distinguishable subset by rpDistance penalty factor (min
		// objective: minimize
		// penalty)
		// sort pi in ascending order by rpDistanceScore
		var sortedPi = pi.sort(function(a, b) {
			// c1, first candidate (all duplicates with equal measures)
			var component1 = problem.lookupObj.lookup[a][0];
			// c2, first candidate (all duplicates with equal measures)
			var component2 = problem.lookupObj.lookup[b][0];

			// min ascending
			return component1.ranking['rpDistanceScore']
					- component2.ranking['rpDistanceScore'];
		});

		for (var j = 0; j < sortedPi.length; j++) {
			sortedPointsPenalty.push(sortedPi[j]);
		}
	}

	// add score to each component
	score(pointsObj, sortedPointsPenalty, rankingId);

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, P, rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector, P, sortedPointsPenalty,
		// problem.objectives, rankingId);

		//
		setPerformanceMeasures(rankingId, pointsObj, problem, P,
				sortedPointsPenalty, rankingPerformanceCollector);
	}
};

// norm weight distance to reference point
normWeightDistance = function(selectedMetrics, components,
		rankingPerformanceCollector) {
	// problem description
	var problem = this.toProblem(selectedMetrics);

	// to points
	var pointsObj = this.toPoints(selectedMetrics, components);

	// collector for set size
	var rankingAnalysisCollectorObj = new rankingAnalysisCollector();

	// compute non-dominated sorting, index = 0, level 1, index = 1 level 2 etc.
	problem.points = pointsObj.points;
	// add pointsObj to problem
	problem.lookupObj = pointsObj;

	var rankingId = 'NORM_WEIGHT_DISTANCE_SMOOP';

	// add relevance score (RP distance penalty!)
	sMoopObj.setReferencePointDistanceScore(pointsObj, problem.points,
			problem.objectives);

	// sort non-distinguishable subset by rpDistance penalty factor (min
	// objective: minimize
	// penalty)
	// sort pi in ascending order by rpDistanceScore
	var sortedPoints = problem.points.sort(function(a, b) {
		// c1, first candidate (all duplicates with equal measures)
		var component1 = problem.lookupObj.lookup[a][0];
		// c2, first candidate (all duplicates with equal measures)
		var component2 = problem.lookupObj.lookup[b][0];

		// min ascending
		return component1.ranking['rpDistanceScore']
				- component2.ranking['rpDistanceScore'];
	});

	// sanity check
	if (sortedPoints.length != problem.points.length) {
		throw "Returned points of normWeightDistance not equal to original points [is/was] = "
				+ sortedPoints.length + "/" + problem.points.length;
	}

	// add score to each component
	score(pointsObj, sortedPoints, rankingId);

	var P = [];
	for (var i = 0; i < sortedPoints.length; i++) {
		var pi = [];
		pi.push(sortedPoints[i]);
		P.push(pi);
	}

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, P, rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector, P, sortedPoints,
		// problem.objectives, rankingId);

		//
		setPerformanceMeasures(rankingId, pointsObj, problem, P, sortedPoints,
				rankingPerformanceCollector);
	}
};

// compute weighted metric (list of measures + distance measure)
weightedSum = function(selectedMetrics, components, weightedSumMethod,
		rankingPerformanceCollector) {
	// problem description
	var problem = this.toProblem(selectedMetrics);

	// to points
	var pointsObj = this.toPoints(selectedMetrics, components);

	// ranking id
	var rankingId = weightedSumMethod === sMoop.WS_DEFAULT ? 'WEIGHTEDSUM_SMOOP'
			: 'WEIGHTEDEUCLIDEAN_SMOOP';

	// set points
	problem.points = pointsObj.points;

	// compute weighted sum
	var wsResult = sMoopObj.weightedSum(problem, weightedSumMethod);

	var sortedPoints = wsResult.sortedPoints;

	// add score to each component
	score(pointsObj, sortedPoints, rankingId);

	var P = sMoopObj.toP(sortedPoints, 0); // first objective

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, P, rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector, P, sortedPoints,
		// problem.objectives, rankingId);

		//
		setPerformanceMeasures(rankingId, pointsObj, problem, P, sortedPoints,
				rankingPerformanceCollector);
	}
};

// single criteria ranking
single = function(selectedMetrics, components, rankingPerformanceCollector) {
	// problem description
	var problem = toProblem(selectedMetrics);

	// to points
	var pointsObj = toPoints(selectedMetrics, components);

	// ranking id
	var rankingId = 'SINGLE_SMOOP';

	// compute weighted sum
	var sortedPoints = sMoopObj.single(problem.objectives, pointsObj.points);
	// add score to each component
	score(pointsObj, sortedPoints, rankingId);

	// to P
	var P = sMoopObj.toP(sortedPoints, 0); // first objective

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, P, rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector, P, sortedPoints,
		// problem.objectives, rankingId);

		//
		setPerformanceMeasures(rankingId, pointsObj, problem, P, sortedPoints,
				rankingPerformanceCollector);
	}
};

// compute naive dominated sorting with ranked objectives
lrr = function(selectedMetrics, components, rankingPerformanceCollector) {
	// problem description
	var problem = toProblem(selectedMetrics);

	// to points
	var pointsObj = toPoints(selectedMetrics, components);

	// collector for set size
	var rankingAnalysisCollectorObj = new rankingAnalysisCollector();

	// compute non-dominated sorting, index = 0, level 1, index = 1 level 2 etc.
	problem.points = pointsObj.points;
	// NDS + ranked objectives
	var P = sMoopObj
			.naiveDominatedSorting(problem, rankingAnalysisCollectorObj);

	var rankingId = 'LRR_SMOOP';

	// sorted points, join all non-dominated sets
	var sortedPoints = [];
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		for (var j = 0; j < pi.length; j++) {
			sortedPoints.push(pi[j]);
		}
	}

	// sanity check
	if (sortedPoints.length != problem.points.length) {
		throw "Returned points of LRR not equal to original points [is/was] = "
				+ sortedPoints.length + "/" + problem.points.length;
	}

	// add score to each component
	score(pointsObj, sortedPoints, rankingId);

	// score non-distinguishable sets in P (po = partial ordering)
	scoreP(pointsObj, rankingAnalysisCollectorObj.nonDistinguishableSets(),
			rankingId + '_po');

	// perf
	if (rankingPerformanceCollector) {
		// // perf
		// setRankingPerf(rankingPerformanceCollector,
		// rankingAnalysisCollectorObj
		// .nonDistinguishableSets(), sortedPoints, problem.objectives,
		// rankingId, rankingAnalysisCollectorObj.diversityCount());

		//
		setPerformanceMeasures(rankingId, pointsObj, problem,
				rankingAnalysisCollectorObj.nonDistinguishableSets(),
				sortedPoints, rankingPerformanceCollector);
	}
};

// set ranking to component
setRankingScore = function(rankingId, score, component) {
	var ranking = component.ranking;
	if (!ranking) {
		ranking = {};

		component.ranking = ranking;
	}
	// set score
	ranking[rankingId] = score;
};

// score each component
score = function(pointsObj, sortedPoints, rankingId) {
	// add score to each component
	var score = 0;

	var lastVal;
	var visitedPoints = {};
	for (var i = 0; i < sortedPoints.length; i++) {

		if (visitedPoints[sortedPoints[i].toString()]) {
			continue;
		}

		// already visited
		visitedPoints[sortedPoints[i].toString()] = true;

		var cArr = pointsObj.lookup[sortedPoints[i]];

		if (!cArr) {
			// continue
			continue;
		}

		for (var k = 0; k < cArr.length; k++) {
			// inc score
			score++;

			// sanity check
			if (score > sortedPoints.length) {
				throw "Ranking score cannot be larger than amount of candidates (by definition) [is/max] = "
						+ score + "/" + sortedPoints.length;
			}

			// set ranking score
			setRankingScore(rankingId, score, cArr[k]);
		}
	}

	// append incompleteComponents to list (components for which at least one
	// measure is missing)
	for (var i = 0; i < pointsObj.incomplete.length; i++) {
		// inc score
		score++;

		// set ranking score
		setRankingScore(rankingId, score, pointsObj.incomplete[i]);
	}
};

// set perf measures for given ranking
// @Deprecated, TBR
setRankingPerf = function(rankingPerformanceCollector, P, sortedPoints,
		objectives, rankingId, setCount) {
	console.log(new Date() + " set count for perf " + setCount);
	// diversity
	var noOfSets = isNaN(setCount) ? P.length : setCount;
	var diversity = sMoopObj.diversity(noOfSets, sortedPoints.length);

	// topN relevance
	var relevanceTopN = sMoopObj.relevance(sortedPoints, objectives);
	// Top 10 relevance
	var relevanceTop10 = sMoopObj.relevance(sortedPoints, objectives, 10);

	// partial relevance (accounting for Reference Point)
	var partialRelevanceTopN = sMoopObj.partialRelevanceForReferencePoint(
			sortedPoints, P, objectives);
	var partialRelevanceTop10 = sMoopObj.partialRelevanceForReferencePoint(
			sortedPoints, P, objectives, 10);

	// sum relevance
	// topN relevance
	var sumRelevanceTopN = sMoopObj.sumRelevance(sortedPoints, objectives);
	// Top 10 relevance
	var sumRelevanceTop10 = sMoopObj.sumRelevance(sortedPoints, objectives, 10);

	// // partial relevance
	// var partialRelevanceTopN = sMoopObj.nestedPartialRelevance(P,
	// objectives);
	// var partialRelevanceTop10 = sMoopObj.nestedPartialRelevance(P,
	// objectives, 10);

	// performance metrics map
	rankingPerformanceCollector.metrics['diversity'] = diversity;
	rankingPerformanceCollector.metrics['relevanceTopN'] = relevanceTopN;
	rankingPerformanceCollector.metrics['relevanceTop10'] = relevanceTop10;
	rankingPerformanceCollector.metrics['sumRelevanceTopN'] = sumRelevanceTopN;
	rankingPerformanceCollector.metrics['sumRelevanceTop10'] = sumRelevanceTop10;
	rankingPerformanceCollector.metrics['partialRelevanceTopN'] = partialRelevanceTopN;
	rankingPerformanceCollector.metrics['partialRelevanceTop10'] = partialRelevanceTop10;
	rankingPerformanceCollector.metrics['noOfNonDistinguishableSets'] = noOfSets;
	// rankingPerformanceCollector.metrics['nonDistinguishableSets'] = P;

	for (var i = 0; i < P.length; i++) {
		rankingPerformanceCollector.metrics['pi_' + i] = P[i].length;
	}
};

// score non-distinguishable sets
scoreP = function(pointsObj, P, rankingId) {
	// add score to each component
	var score = 0;

	var lastVal;
	var visitedPoints = {};
	for (var i = 0; i < P.length; i++) {
		var pi = P[i];

		if (pi.length > 0) {
			// inc score
			score++;
		}

		for (var j = 0; j < pi.length; j++) {
			if (visitedPoints[pi[j].toString()]) {
				continue;
			}

			// already visited
			visitedPoints[pi[j].toString()] = true;

			var cArr = pointsObj.lookup[pi[j]];

			if (!cArr) {
				// continue
				continue;
			}

			for (var k = 0; k < cArr.length; k++) {
				// sanity check
				if (score > P.length) {
					throw "Ranking score cannot be larger than amount of non-distinguishable sets (by definition) [is/max] = "
							+ score + "/" + P.length;
				}

				// set ranking score
				setRankingScore(rankingId, score, cArr[k]);
			}
		}
	}

	// append incompleteComponents to list (components for which at least one
	// measure is missing)
	for (var i = 0; i < pointsObj.incomplete.length; i++) {
		// inc score
		score++;

		// set ranking score
		setRankingScore(rankingId, score, pointsObj.incomplete[i]);
	}
};

/**
 * Compute diversity based on sets size and points size. (1 ideal, range (0, 1],
 * setSize / pointsSize).
 * 
 * @param {number}
 *            setSize - Sets size
 * @param {number}
 *            pointsSize - Points size (length of all points)
 * @return {number} diversity - setSize / pointsSize
 */
measureDistinctiveness = function(setSize, pointsSize) {
	// diversity (#sets divided by #points)
	var diversity = setSize / pointsSize;

	if (this.verbose) {
		console.log("set size " + setSize + " divided by points size "
				+ pointsSize + " = " + diversity);
	}

	return diversity;
};

// measure relevance: sum[rel = (1 / rank) * distance * scaleFactor]
measureRelevance = function(rankingId, pointsObj, sortedPoints, topN) {
	var rankingLength = isNaN(topN) ? sortedPoints.length : topN;

	// adjust to the smaller value
	if (rankingLength > sortedPoints.length) {
		rankingLength = sortedPoints.length;
	}
	// invalid
	if (rankingLength < 1) {
		throw "Illegal topN value passed, must be greater than 0: " + topN;
	}

	var scaleFactor = 1;
	var relevance = 0;
	for (var i = 0; i < rankingLength; i++) {

		// add relevance score to candidate(s)
		var cArr = pointsObj.lookup[sortedPoints[i]];
		for (var k = 0; k < cArr.length; k++) {
			var component = cArr[k];

			var distance = component.ranking['rpDistanceScore'];
			var rank = component.ranking[rankingId];

			//console.log("D R" + distance + " " + rank);

			// sum up
			var rel = (1 / rank) * distance * scaleFactor;

			relevance += rel;
		}
	}

	return relevance;
};

setPerformanceMeasures = function(rankingId, pointsObj, problem, P,
		sortedPoints, rankingPerformanceCollector) {
	// compute distance penalty
	// add relevance score (RP distance penalty!)
	sMoopObj.setReferencePointDistanceScore(pointsObj, sortedPoints,
			problem.objectives);

	// distinctiveness
	rankingPerformanceCollector.metrics['distinctiveness'] = measureDistinctiveness(
			P.length, sortedPoints.length);

	// relevance strict ordering
	rankingPerformanceCollector.metrics['relevance_so'] = measureRelevance(
			rankingId, pointsObj, sortedPoints);
	rankingPerformanceCollector.metrics['relevance_top10_so'] = measureRelevance(
			rankingId, pointsObj, sortedPoints, 10);

	// relevance partial ordering
	rankingPerformanceCollector.metrics['relevance_po'] = measureRelevance(
			rankingId + '_po', pointsObj, sortedPoints);
	rankingPerformanceCollector.metrics['relevance_top10_po'] = measureRelevance(
			rankingId + '_po', pointsObj, sortedPoints, 10);

	// no of non-distinguishable sets
	rankingPerformanceCollector.metrics['nds_size'] = P.length;

	for (var i = 0; i < P.length; i++) {
		rankingPerformanceCollector.metrics['pi_' + i] = P[i].length;
	}

	// incomplete
	rankingPerformanceCollector.metrics['incomplete_size'] = pointsObj.incomplete ? pointsObj.incomplete.length
			: 0;
	
	// add normalized
	setNormalizedValues(rankingId, pointsObj, problem, sortedPoints, rankingPerformanceCollector);
	
	// add stats for measures (normalized)
	//var normalizedPoints = sMoopObj.normalize(sortedPoints);
	var statsPerDim = sMoopObj.stats(sortedPoints);
	for(var y = 0; y < problem.objectives.length; y++) {
		var dimStats = statsPerDim[y];
		rankingPerformanceCollector.metrics[problem.ids[y] + '_min'] = dimStats.min;
		rankingPerformanceCollector.metrics[problem.ids[y] + '_max'] = dimStats.max;
		rankingPerformanceCollector.metrics[problem.ids[y] + '_mean'] = dimStats.mean;
		rankingPerformanceCollector.metrics[problem.ids[y] + '_sum'] = dimStats.sum;
		rankingPerformanceCollector.metrics[problem.ids[y] + '_stdev'] = dimStats.stdev;
	}
};

// set normalized values, range [0, 100]
setNormalizedValues = function(rankingId, pointsObj, problem, sortedPoints, rankingPerformanceCollector) {
	  // normalization of points in [0, 1] range
	  var normalizedPoints = sMoopObj.normalize(sortedPoints);

	  // apply duality principle if necessary (min --> max)
	  for(var i = 0; i < normalizedPoints.length; i++) {
	    var point = normalizedPoints[i];

	    for(var y = 0; y < problem.objectives.length; y++) {
	      // make min to max
	      if(problem.objectives[y] == 0) {
	        point[y] = 1 - point[y];
	      }
	    }
	  }
	  
	  for (var i = 0; i < sortedPoints.length; i++) {

		// add normalized vector to candidate(s)
		var cArr = pointsObj.lookup[sortedPoints[i]];
		for (var k = 0; k < cArr.length; k++) {
			var component = cArr[k];
			
			for(var n = 0; n < normalizedPoints[i].length; n++) {
				// range [0, 1]
				component.ranking[problem.ids[n] + "_norm"] = normalizedPoints[i][n];
			}
		}
	  }
};