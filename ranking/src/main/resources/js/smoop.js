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
* Library which implements various ranking algorithms for the SOCORA ranking engine.
* (smoop => SOCORA multi-objective optimization)
*
* @author Marcus Kessel (kessel@informatik.uni-mannheim.de)
* @version 0.0.1
*/

/**
 * sMoop object
 *
 * @constructor
 * @param {boolean} verbose - Toggle verbosity (log output)
 */
function sMoop(verbose) {
  // print debug log
  this.verbose = verbose;
}

// -- constants
sMoop.WS_METRIC_EUCLIDEAN_DISTANCE = 'EUCLIDEAN';
sMoop.WS_DEFAULT = 'DEFAULT';

// -- add properties

// -- add methods

/**
 * Single Criterion Ranking
 *
 * @param {number[]} objectives - Array of objectives with either max (1) or min (0). Index has to correspond to point index
 * @param {number[[number[]]} points - Array of points, where each point is represented as an own number array (index must match to objectives)
 * @param {number} subObjective - Which objective should be applied for sorting? May be undefined (if undefined, first objective == 0 is applied)
 * @return {number[[number[]]} sortedPoints - Array of sorted points according to given objectives and optionally restricted by subObjective indizes
 */
sMoop.prototype.single = function(objectives, points, subObjective) {
  var oIndex = isNaN(subObjective) ? 0 : subObjective;

  // sort in ascending order
  var sortedPoints = points.sort(function(a, b) {
    if(objectives[oIndex] === 0) {
      // min ascending
      return a[oIndex] - b[oIndex];
    } else {
      // max ascending
      return b[oIndex] - a[oIndex];
    }
  });

  return sortedPoints;
};

/**
 * Determine the non-dominated set (could be Pareto frontier or local non-dominated solutions) for a given array of points and objectives,
 * assuming minimize objective by default, i.e. all max objectives are converted to min objectives according to the duality principle.
 *
 * @param {number[]} objectives - Array of objectives with either max (1) or min (0). Index has to correspond to point index
 * @param {number[[number[]]} points - Array of points, where each point is represented as an own number array (index must match to objectives)
 * @param {number[]} subObjectives - Array of indizes which map to the indizes of objectives array (used to restrict to a subset of objectives). May be undefined
 * @return {Object} p,d - p the non-dominated set of points, and d the dominated set of points
 */
sMoop.prototype.nonDominatedSet = function(objectives, points, subObjectives) {
  // non-dominated set
  var p = [];
  // dominated set
  var d = [];

  // check if we are restricted to subobjectives
  var restricted = false;
  // note: subObjectives is an array of INDIZES which map to objectives
  if(subObjectives && subObjectives.length > 0) {
    restricted = true;
  }

  if(this.verbose && restricted) {
    console.log("Restricted non-dominated set " + subObjectives.toString());
  }

  for(var i = 0; i < points.length; i++) {
    var point = points[i];
    // is dominated
    var dominated = false;
    for(var j = 0; j < points.length; j++) {
      // do not compare same element
      if(i === j) {
        continue;
      }

      var otherPoint = points[j];

      // increments for point equals or point "better"
      var nonDomCount = 0;
      var domCount = 0;
      for(var k = 0; k < point.length; k++) {
        // check if restriction
        if(restricted) {
          var allowedObjective = false;
          for(var z = 0; z < subObjectives.length; z++) {
            // note: subObjectives is an array of INDIZES which map to objectives
            if(subObjectives[z] === k) {
              allowedObjective = true;
              break;
            }
          }

          if(!allowedObjective) {
            // omit current objective
            continue;
          }
        }

        // shall we minimize or maximize?
        if(objectives[k] === 0) {
          // min
          if(point[k] < otherPoint[k]) {
            // dominates
            nonDomCount++;
          } else if(point[k] > otherPoint[k]) {
            // is dominated
            domCount++;
          }
        } else {
          // max
          if(point[k] > otherPoint[k]) {
            // dominates
            nonDomCount++;
          } else if(point[k] < otherPoint[k]) {
            // is dominated
            domCount++;
          }
        }
      }

      // --
      // strong dominance := domCount == point.length
      // --

      // does not dominate, but is dominated
      if(nonDomCount < 1 && domCount > 0) {
        dominated = true;

        // continue with next point
        break;
      } // else equal point
    }

    if(!dominated) {
      p.push(point);
    } else {
      d.push(point);
    }
  }

  if(this.verbose) {
    console.log("non-dominated solutions found: " + p.length);
  }

  // p = pareto, non-dominated set, d = dominated set
  return {'p': p, 'd': d};
};

///**
// * Determine the non-dominated set (could be Pareto frontier or local non-dominated solutions) for a given array of points and objectives,
// * assuming minimize objective by default, i.e. all max objectives are converted to min objectives according to the duality principle.
// *
// * @param {number[]} objectives - Array of objectives with either max (1) or min (0). Index has to correspond to point index
// * @param {number[[number[]]} points - Array of points, where each point is represented as an own number array (index must match to objectives)
// * @param {number[]} subObjectives - Array of indizes which map to the indizes of objectives array (used to restrict to a subset of objectives). May be undefined
// * @return {Object} p,d - p the non-dominated set of points, and d the dominated set of points
// */
//sMoop.prototype.nonDominatedSet = function(objectives, points, subObjectives) {
//  // non-dominated set
//  var p = [];
//  // dominated set
//  var d = [];
//
//  // check if we are restricted to subobjectives
//  var restricted = false;
//  // note: subObjectives is an array of INDIZES which map to objectives
//  if(subObjectives && subObjectives.length > 0) {
//    restricted = true;
//  }
//  
//  // objectives length (considering the case of having only a subset)
//  var objectiveLength = restricted ? subObjectives.length : objectives.length;
//
//  if(this.verbose && restricted) {
//    console.log("Restricted non-dominated set " + subObjectives.toString());
//  }
//
//  for(var i = 0; i < points.length; i++) {
//    var point = points[i];
//    // is dominated
//    var dominated = false;
//    for(var j = 0; j < points.length; j++) {
//      // do not compare same element
//      if(i === j) {
//        continue;
//      }
//
//      var otherPoint = points[j];
//
//      // increments for point equals or point "better"
//      var nonDomCount = 0;
//      var noWorseCount = 0;
//      var domCount = 0;
//      for(var k = 0; k < objectives.length; k++) {
//        // check if restriction
//        if(restricted) {
//          var allowedObjective = false;
//          for(var z = 0; z < subObjectives.length; z++) {
//            // note: subObjectives is an array of INDIZES which map to objectives
//            if(subObjectives[z] === k) {
//              allowedObjective = true;
//              break;
//            }
//          }
//
//          if(!allowedObjective) {
//            // omit current objective
//            continue;
//          }
//        }
//
//        // current objective: shall we minimize or maximize?
//        if(objectives[k] === 0) {
//          // min
//          if(point[k] < otherPoint[k]) {
//        	// 2) even better  
//        	  
//            // dominates
//            nonDomCount++;
//          } else if(point[k] > otherPoint[k]) {
//            // is dominated
//            domCount++;
//          } else {
//        	// 1) no worse
//        	noWorseCount++;
//          }
//        } else {
//          // max
//          if(point[k] > otherPoint[k]) {
//        	// 2) even better
//        	  
//            // dominates
//            nonDomCount++;
//          } else if(point[k] < otherPoint[k]) {
//            // is dominated
//            domCount++;
//          } else {
//          	// 1) no worse
//          	noWorseCount++;
//          }
//        }
//      }
//      
//      
//      // otherPoint: 1) no worse or 2) even better
//      // weak
//      var weakDominance = (domCount + noWorseCount) === objectiveLength;
//      // strong
//      var strongDominance = (domCount === objectiveLength);
//      
//      if(this.verbose) {
//          if(strongDominance) {
//        	console.log("STRONG dominance of " + otherPoint + " over " + point); 
//          } else if(weakDominance) {
//        	console.log("WEAK dominance of " + otherPoint + " over " + point); 
//          }
//      }
//
//      // point is dominated by otherPoint? (strongly or weakly dominated)
//      if(strongDominance || weakDominance) {
//        dominated = true;
//        
//        console.log(point + " does not dominate");
//
//        // continue with next point
//        break;
//      } // else equal point
//    }
//
//    if(!dominated) {
//      // all those points that are NOT dominated go into pareto set
//      p.push(point);
//    } else {
//      // all those points that ARE dominated go into remaining set
//      d.push(point);
//    }
//  }
//
//  if(this.verbose) {
//    console.log("non-dominated solutions found: " + p.length);
//  }
//
//  // p = pareto, non-dominated set, d = dominated set
//  return {'p': p, 'd': d};
//};

/**
 * Ranking based on weighted sum (or weighted metric), points are sorted according to score in ascending order (min objective by default).
 * Duality principle is applied for max objectives, i.e. converted to min.
 *
 * Weighted sum/metric: Each point is normilized before weighted sum/metric is applied.
 * Weighted metric using distance function: Euclidean distance is applied, the "ideal" vector corresponds to the best solution(s) found (alternatively could be also a zero vector, i.e. min objective be default).
 *
 * @param {Object} problem - Problem object must contain points, objectives and weights.
 * @param {string} weightedSumMethod - see constants defined
 * @return {Object} sortedPoints,P - sortedPoints the absolute sorting in ascending order, P depicts groups of points which can't be distinguished in ascending order
 */
sMoop.prototype.weightedSum = function(problem, weightedSumMethod) {
  var objectives = problem.objectives;
  var weights = problem.weights;
  var points = problem.points;

  // holds weighted sum corresponding index of points array, used to sort afterwards
  var sumArr = {};

  // weights vector
  var weightsVector = Vector.create(weights);

  // normalization of points in [0, 1] range
  var normalizedPoints = this.normalize(points);
  // sum of weights must be 1
  var weightSum = jStat(weights).sum();
  if(weightSum != 1) {
    throw "Sum of weights must be one, but is " + weightSum;
  }

  // apply duality principle if necessary (min --> max)
  for(var i = 0; i < normalizedPoints.length; i++) {
    var point = normalizedPoints[i];

    for(var y = 0; y < objectives.length; y++) {
      // make to min if max objective (duality principle)
      if(objectives[y] == 1) {
        point[y] = point[y] * -1;
      }
    }
  }

  // get stats for normalized points
  var nStats = this.stats(normalizedPoints);

  // compute weighted sum for each normalized point
  for(var i = 0; i < normalizedPoints.length; i++) {
    var point = normalizedPoints[i];

    // weighted metric?
    if (weightedSumMethod === sMoop.WS_METRIC_EUCLIDEAN_DISTANCE) {
      // ideal vector from min of all objectives
      var idealArr = [];
      for(var x = 0; x < nStats.length; x++) {
        idealArr.push(nStats[x].min);
      }

      // ideal min vector solution
      var idealVector = Vector.create(idealArr);

      // create vector for values and mult weights
      var multArr = [];
      for(var z = 0; z < point.length; z++) {
        multArr.push(weights[z] * point[z]);
      }

      // apply euclidean distance
      sum = idealVector.distanceFrom(Vector.create(multArr));

      if(this.verbose) {
        console.log("Result of eclidean distance for [" + points[i] + "] (normalized [" + point + "]): "
            + sum);
      }
    } else if(weightedSumMethod === sMoop.WS_DEFAULT) {
      // normal weighted sum up (scalar product)
      sum = weightsVector.dot(Vector.create(point));

      if(this.verbose) {
        console.log("Result of default weighted sum for [" + points[i] + "] (normalized [" + point + "]): "
            + sum);
      }
    } else {
      // nothing else currently supported
      sum = 0;

      console.log("No weighted sum method passed");

      throw "No weighted sum method passed";
    }

    // add to map
    // take care of double entries
    var sKey = points[i].toString();
    sumArr[sKey] = sum;
  }

  // sort in ascending order
  var sortedPoints = points.sort(function(a, b) {
    var aKey = a.toString();
    var bKey = b.toString();

    return sumArr[aKey] - sumArr[bKey];
  });

  if(this.verbose) {
    for(var i = 0; i < sortedPoints.length; i++) {
      var sKey = sortedPoints[i].toString();
      console.log(sumArr[sKey]);
    }
  }

  // collected all weighted sums
  var weightedSums = [];
  for(var pKey in sumArr) {
    if(sumArr.hasOwnProperty(pKey)) {
      // push one-dim 'point' arr
      var point = [sumArr[pKey]];
      weightedSums.push(point);
    }
  }

  // toP, objective index is assumed to be zero => weighted sum
  var objectiveIndex = 0;
  var P = this.toP(weightedSums, objectiveIndex);

  // return map
  return {
    'sortedPoints' : sortedPoints,
    'P' : P
  };
};

/**
 * Flat, Non-Dominated Sorting (FDS) Algorithm.
 *
 * Simple algorithm for implementing non-dominated sorting of points given a set of objectives.
 * See K. Deb (Multi-Objective Optimization Using Evolutionary Algorithms) for details.
 *
 * @param {Object} problem - Problem object must contain points, objectives and weights.
 * @param {number[]} subObjectives - Array of indizes which map to the indizes of objectives array (used to restrict to a subset of objectives). May be undefined
 * @return {Object} P - P depicts groups of points which can't be distinguished in ascending order of relevance
 */
sMoop.prototype.nonDominatedSorting = function(problem, subObjectives) {
  var objectives = problem.objectives;
  var weights = problem.weights;
  var points = problem.points;

  // compute non-dominated sets, index = 0, level 1, index = 1 level 2 etc.
  var P = [];
  // start with all points (= candidates)
  var p = points;
  var level = 0;
  while(p && p.length > 0) {
    // compute non-dominated set (pareto frontier)
    var result = this.nonDominatedSet(objectives, p, subObjectives);
    var pi = result.p;

    // add to non-dominated sets
    P.push(pi);

    if(this.verbose) {
      console.log("added non-dominated set for level " + (++level) + " = " + pi + ", dominated set size " + result.d.length);
    }

    // substract current non-dominated set pi from p, set to dominated set
    p = result.d;
  }

  // array of arrays, sorted in ascending order (best to worst non-dominated set)
  return P;
};

/**
 * Non-Dominated Sorting ranking incl. weighted sum method for sub ranking.
 *
 * @param {Object} problem - Problem object must contain points, objectives and weights.
 * @param {string} weightedSumMethod - see constants defined
 * @return {Object} P - P depicts groups of points which can't be distinguished in ascending order of relevance
 */
sMoop.prototype.nonDominatedSortingWS = function(problem, weightedSumMethod) {
  // compute non-dominated sets, index = 0, level 1, index = 1 level 2 etc.
  var P = this.nonDominatedSorting(problem);

  // sort each front using weighted sum (if set, i.e. not undefined)
  if(weightedSumMethod) {
    for(var i = 0; i < P.length; i++) {
      var pi = P[i];

      // only sort if length > 1
      if(pi.length > 1) {
        // sort using weighted sum
        var wsResult = this.weightedSum({
          'objectives' : problem.objectives,
          'weights' : problem.weights,
          'points' : pi
        }, weightedSumMethod);

        P[i] = wsResult.sortedPoints;
      }
    }
  }

  // array of arrays, sorted in ascending order (best to worst non-dominated set)
  return P;
};

/**
 * Non-Dominated Sorting ranking incl. high level objective method for sub ranking.
 *
 * @param {Object} problem - Problem object must contain points, objectives and weights. Last objective is assumed to be the high level objective.
 * @return {Object} P - P depicts groups of points which can't be distinguished in ascending order of relevance
 */
sMoop.prototype.nonDominatedSortingHL = function(problem) {
  // check args
  if(problem.objectives < 3) {
    throw new "Minimum size of objectives for non-dominated sorting + high level objective is 3";
  }

  // assume the last element is the high-level objective
  var ndsProblem = {
    'objectives' : [],
    //'weights' : [], not needed
    'points' : []
  };

  // assign objectives
  // high-level, assumed to be last element
  var hlProblemObjectives = [problem.objectives[problem.objectives.length - 1]];

  for(var dim = 0; dim < problem.objectives.length - 1; dim++) {
    ndsProblem.objectives.push(problem.objectives[dim]);
  }

  // map for bookkeeping both point sets, key = ndsPoint, value [] of hlPoint
  var hlPointLookup = {};

  // assign points
  for(var i = 0; i < problem.points.length; i++) {
    var point = problem.points[i];

    var ndsPoint = [];
    // nds objectives
    for(var dim = 0; dim < ndsProblem.objectives.length; dim++) {
      ndsPoint.push(point[dim]);
    }
    ndsProblem.points.push(ndsPoint);

    // high-level objectives (last element in point array)
    var hlPoint = [point[point.length - 1]];

    // lookup entry and update
    var lKey = ndsPoint.toString();
    var lEntry = hlPointLookup[lKey];
    if(!lEntry) {
      lEntry = [];
      hlPointLookup[lKey] = lEntry;
    }

    lEntry.push(hlPoint);
  }

  // compute non-dominated sets, index = 0, level 1, index = 1 level 2 etc.
  var P = this.nonDominatedSorting(ndsProblem);

  // sort each front using high-level objective
  for(var i = 0; i < P.length; i++) {
    var pi = P[i];

    // only sort if length > 1
    if(pi.length > 1) {
      // we need the subset of points corresponding to pi points
      var hlPoints = [];
      // key: hlPoint, value = []
      var ndsPointLookup = {};
      var seen = {};
      for(var n = 0; n < pi.length; n++) {
        var lKey = pi[n].toString();

        if(seen[lKey]) {
          continue;
        }
        // set to seen to avoid duplicates
        seen[lKey] = true;

        var lEntryArr = hlPointLookup[lKey];

        for(var y = 0; y < lEntryArr.length; y++) {
          hlPoints.push(lEntryArr[y]);

          var nEntry = ndsPointLookup[lEntryArr[y].toString()];
          if(!nEntry) {
            nEntry = [];
            ndsPointLookup[lEntryArr[y].toString()] = nEntry;
          }

          nEntry.push(pi[n]);
        }
      }

      // sort using high-level objective
      var hlPi = this.single(hlProblemObjectives, hlPoints);

      if(this.verbose) {
        console.log("hl pi " + hlPi.toString());
      }

      var newPi = [];
      for(var x = 0; x < hlPi.length; x++) {
        var pointArr = ndsPointLookup[hlPi[x].toString()];

        // add
        for(var y = 0; y < pointArr.length; y++) {
          // reconstruct point incl. HL
          var newPoint = pointArr[y].concat([hlPi[x]]);

          //
          newPi.push(newPoint);
        }
      }

      // set new pi
      P[i] = newPi;

      if(this.verbose) {
        console.log("CUT " + P[i].toString());
      }
    } else {
      // we need to reconstruct single point
      var singlePoint = pi[0];
      // should be only one HL point by assumption
      var hlPoints = hlPointLookup[singlePoint.toString()];
      if(hlPoints.length > 1) {
        throw "Single point has more than one High Level Point " + singlePoint.toString() + " = " + hlPoints.toString();
      }
      var hlPoint = hlPoints[0];

      // reconstruct point incl. HL
      var newPoint = singlePoint.concat([hlPoint]);

      // set new pi
      P[i] = [newPoint];

      if(this.verbose) {
        console.log("CUT " + P[i].toString());
      }
    }
  }

  // array of arrays, sorted in ascending order (best to worst non-dominated set)
  return P;
};

/**
 * Hybrid, Non-Dominated Sorting (HDS) Algorithm.
 *
 * Recursive non-dominated sorting ranking incl. ranked objectives method (i.e. prioritized objectives) for sub ranking.
 *
 * 1. Apply NDS WITHOUT sub ranking
 * 2. Apply priorities recursively using NDS
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {Object} rankingAnalysisCollectorObj - Listener that gets notified about current nesting level and set splits.
 * @param {number} currentPriority - Current priority used for sub ranking. In first call assumed to be undefined.
 * @return {Object} P - P depicts groups of points which can't be distinguished in ascending order of relevance
 */
sMoop.prototype.nonDominatedSortingRO = function(problem, rankingAnalysisCollectorObj, currentPriority) {
  if(this.verbose) {
    console.log("NDS+RO [prev priority/subobjectives] = " + currentPriority);
  }

  // determine subobjectives, note only indizes are returned, note in case NaN, objectives remains undefined
  var objectives;
  if(!isNaN(currentPriority)) {
    objectives = this.subObjectives(problem, currentPriority);
  } else {
    // first run is pure NDS without priorities, objectives => undefined
  }

  // compute non-dominated sets, index = 0, level 1, index = 1 level 2 etc.
  var P = this.nonDominatedSorting(problem, objectives);

  // determine next priority
  var nextPriority = this.nextPriority(problem, currentPriority);

  // no more priorities?
  if(isNaN(nextPriority)) {
    // return P, we are done
    return P;
  }

  // objectives
  console.log("NDS+RO next [next priority] = " + nextPriority);

  // NDS without priorities? if yes, level 0, otherwise priority-dependent level + 1 to account for NDS
  var nestingLevel = isNaN(currentPriority) ? 0 : (sMoopObj.nestingLevel(problem, currentPriority) + 1);
  rankingAnalysisCollectorObj.onSetSplit(problem, nestingLevel, P);

  // for each non-dominated set, apply prioritized sub ranking using NDS
  for(var i = 0; i < P.length; i++) {
    var pi = P[i];

    // only sort if length > 1
    //if(pi.length > 1) {
      if(this.verbose) {
        console.log("Recursive NDS [next priority] = " + nextPriority);
      }

      // construct new sub problem
      var newProblem = {
        'objectives' : problem.objectives,
        'weights' : problem.weights,
        'priorities' : problem.priorities,
        'points' : pi
      };
      // recursive NDS with new sub problem and next priority)
      // in case objectives > 1, equal priorities assigned, otherwise unique priority assigned to exactly one objective
      var newP = this.nonDominatedSortingRO(newProblem, rankingAnalysisCollectorObj, nextPriority);

      var newPi = [];
      // add to new pi
      for(var o = 0; o < newP.length; o++) {
        newPi = newPi.concat(newP[o]);
      }

      // set new pi
      P[i] = newPi;
    //}
  }

  // array of arrays, sorted in ascending order (best to worst non-dominated set)
  return P;
};

/**
 * ReferencePoint + Distance + Hybrid, Non-Dominated Sorting (HDS) Algorithm.
 *
 * Recursive non-dominated sorting ranking incl. ranked objectives method (i.e. prioritized objectives) for sub ranking.
 *
 * 1. Apply NDS WITHOUT sub ranking
 * 2. Apply priorities recursively using NDS
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {Object} rankingAnalysisCollectorObj - Listener that gets notified about current nesting level and set splits.
 * @param {number} currentPriority - Current priority used for sub ranking. In first call assumed to be undefined.
 * @return {Object} P - P depicts groups of points which can't be distinguished in ascending order of relevance
 */
sMoop.prototype.rphds = function(problem, rankingAnalysisCollectorObj, currentPriority) {
  if(this.verbose) {
    console.log("RPNDS+RO [prev priority/subobjectives] = " + currentPriority);
  }

  // determine subobjectives, note only indizes are returned, note in case NaN, objectives remains undefined
  var objectives;
  if(!isNaN(currentPriority)) {
    objectives = this.subObjectives(problem, currentPriority);
  } else {
    // first run is pure NDS without priorities, objectives => undefined
  }

  // compute non-dominated sets, index = 0, level 1, index = 1 level 2 etc.
  var P = this.nonDominatedSorting(problem, objectives);

  // determine next priority
  var nextPriority = this.nextPriority(problem, currentPriority);

  // no more priorities?
  if(isNaN(nextPriority)) {
    // return P, we are done
    return P;
  }

  // objectives
  console.log("RPNDS+RO next [next priority] = " + nextPriority);

  // NDS without priorities? if yes, level 0, otherwise priority-dependent level + 1 to account for NDS
  var nestingLevel = isNaN(currentPriority) ? 0 : (sMoopObj.nestingLevel(problem, currentPriority) + 1);
  rankingAnalysisCollectorObj.onSetSplit(problem, nestingLevel, P);

  // for each non-dominated set, apply prioritized sub ranking using NDS
  for(var i = 0; i < P.length; i++) {
    var pi = P[i];

    // only sort if length > 1
    //if(pi.length > 1) {
      if(this.verbose) {
        console.log("Recursive NDS [next priority] = " + nextPriority);
      }

      // construct new sub problem
      var newProblem = {
        'objectives' : problem.objectives,
        'weights' : problem.weights,
        'priorities' : problem.priorities,
        'points' : pi,
        'lookupObj' : problem.lookupObj
      };
      // recursive NDS with new sub problem and next priority)
      // in case objectives > 1, equal priorities assigned, otherwise unique priority assigned to exactly one objective
      var newP = this.rphds(newProblem, rankingAnalysisCollectorObj, nextPriority);

      var newPi = [];
      // add to new pi
      for(var o = 0; o < newP.length; o++) {
        newPi = newPi.concat(newP[o]);
      }

      // set new pi
      P[i] = newPi;
    //}
  }

  // array of arrays, sorted in ascending order (best to worst non-dominated set)
  return P;
};

/**
 * Linear Recursive Ranking (LRR) Algorithm.
 *
 * Recursive naive dominated sorting (a multi-criteria like interpretation using iterative single-criterion rankings) using ranked priorities for sub ranking.
 *
 * This algorithm is only capable to further sub rank if all values so far were equivalent for at least two points.
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {Object} rankingAnalysisCollectorObj - Listener that gets notified about current nesting level and set splits.
 * @param {number} lastPriority - Last priority used for sub ranking, used to compute next priority. In first call assumed to be undefined.
 * @return {Object} P - P depicts groups of points which can't be distinguished in ascending order of relevance
 */
sMoop.prototype.naiveDominatedSorting = function(problem, rankingAnalysisCollectorObj, lastPriority) {
  if(this.verbose) {
    console.log("NaiveDominatedSorting [last priority] = " + lastPriority);
  }

  // determine priority objectives (unique priority!)
  var nextPriority = this.nextPriority(problem, lastPriority);

  // determine subobjectives, note only objective indizes are returned
  var objectives = this.subObjectives(problem, nextPriority);

  // objectives
  console.log("NaiveDominatedSorting Priority " + nextPriority + " objectives " + objectives.toString());

  // select current single criterion
  // in case of objectives > 1, we assume that the first objective "naturally" ranked first is used as single-criterion
  var objectiveIndex = objectives[0];

  // create single-criterion ranking according to highest ranked objective
  var sortedPoints = this.single(problem.objectives, problem.points, objectiveIndex);

  // our sets (in this case, set larger than 1 denotes candidates with equal measures)
  var P = [];
  // create groups (in this case, set larger than 1 denotes candidates with equal measures), explictely use current objective index (i.e. single criterion)
  P = this.toP(sortedPoints, objectiveIndex);

  // current nesting level dependent on priority
  var nestingLevel = sMoopObj.nestingLevel(problem, nextPriority);
  rankingAnalysisCollectorObj.onSetSplit(problem, nestingLevel, P);

  // resursive naive dominated sort (note: sets contain only candidates with equal measures for current selected single objective)
  for(var i = 0; i < P.length; i++) {
    var pi = P[i];

    // only sort if length > 1 and priority left
    if(/*pi.length > 1 &&*/ !isNaN(this.nextPriority(problem, nextPriority))) {
      if(this.verbose) {
        console.log("Recursive Naive Dominated Sort [prev priority] = " + nextPriority);
      }

      // construct new sub problem
      var newProblem = {
        'objectives' : problem.objectives,
        'weights' : problem.weights,
        'priorities' : problem.priorities,
        'points' : pi
      };
      // recursive naive dominated sort with new sub problem and sub objectives (based on current priority)
      // in case objectives > 1, equal priorities assigned, otherwise unique priority assigned to exactly one objective
      var newP = this.naiveDominatedSorting(newProblem, rankingAnalysisCollectorObj, nextPriority);

      var newPi = [];
      // add to new pi
      for(var o = 0; o < newP.length; o++) {
        newPi = newPi.concat(newP[o]);
      }

      // set new pi
      P[i] = newPi;
    }
  }

  // array of arrays, sorted in ascending order (best to worst set)
  return P;
};

/**
 * Get statistics for points (e.g. min, max, mean etc.)
 *
 * @param {number[number[]]} points - Points
 * @return {Object[]} stats - stats per dimension (indizes map to point indizes)
 */
sMoop.prototype.stats = function(points) {
  // stats per dimension
  var statsPerDim = [];

  // dimensions
  var dims = points[0].length;
  for(var dim = 0; dim < dims; dim++) {
    var values = [];
    for(var j = 0; j < points.length; j++) {
      var point = points[j];

      values.push(point[dim]);
    }

    // collect statistics
    var stats = {};
    // jStat
    stats.mean = jStat(values).mean();
    stats.sum = jStat(values).sum();
    stats.min = jStat(values).min();
    stats.max = jStat(values).max();
    stats.stdev = jStat(values).stdev();

    if(this.verbose) {
      console.log("stats for dim " + dim + " (min/max): " + stats.min + "/" + stats.max);
    }

    statsPerDim.push(stats);
  }

  return statsPerDim;
};

/**
 * Normalize points in range [0, 1] (for each value).
 *
 * @param {number[number[]]} points - Points
 * @param {Object} statsPerDim - Statistics per dimension (optional)
 * @return {number[number[]]} normalizedPoints - in the range [0, 1]
 */
sMoop.prototype.normalize = function(points, statsPerDim) {
  var normalizedPoints = [];

  // get statistics
  var normStatsPerDim = statsPerDim ? statsPerDim : this.stats(points);

  // normalize each point
  for(var j = 0; j < points.length; j++) {
    var point = points[j];

    var normalizedPoint = [];
    for(var dim = 0; dim < point.length; dim++) {
      // stats
      var stats = normStatsPerDim[dim];

      // normalize value in range 0 - 1
      var dev = stats.max - stats.min;
      // division by zero (min == max)
      if(dev === 0) {
        dev = 1;
      }
      var nVal = (point[dim] - stats.min) / dev;
      normalizedPoint.push(nVal);
    }

    normalizedPoints.push(normalizedPoint);
  }

  return normalizedPoints;
};

/**
 * Determine next priority.
 *
 * a) if currentPriority undefined, highest priority
 * b) if currentPriority defined, next priority in descending order
 * c) if currentPriority lowest, return undefined.
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {number} currentPriority - Current priority used for sub ranking. In first call assumed to be undefined.
 * @return {number} nextPriority - next priority
 */
sMoop.prototype.nextPriority = function(problem, currentPriority) {
  // apply filter to get unique priorities
  var uniquePriorities = problem.priorities.filter(function(item, i, ar) {
    return ar.indexOf(item) === i;
  });
  // rank priorities
  var rankedPriorities = uniquePriorities.sort(function(a, b) {
    // descending
    return b - a;
  });

  if(this.verbose) {
    console.log("Ranked priorities " + rankedPriorities.toString());
  }

  var priority;
  if(!isNaN(currentPriority)) {
    // determine next (LESS than currentPriority)
    for(var i = 0; i < rankedPriorities.length; i++) {
      if(rankedPriorities[i] < currentPriority) {
        priority = rankedPriorities[i];
        break;
      }
    }
  } else {
    // start with first max priority
    priority = rankedPriorities[0];
  }

  return priority;
};

/**
 * Determine current nesting level based on current priority.
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {number} currentPriority - Current priority used for sub ranking
 * @return {number} nestingLevel - Nesting level for currentPriority
 */
sMoop.prototype.nestingLevel = function(problem, currentPriority) {
  // apply filter to get unique priorities
  var uniquePriorities = problem.priorities.filter(function(item, i, ar) {
    return ar.indexOf(item) === i;
  });
  // rank priorities
  var rankedPriorities = uniquePriorities.sort(function(a, b) {
    // descending
    return b - a;
  });

  var nestingLevel = rankedPriorities.indexOf(currentPriority);
  return nestingLevel;
};

/**
 * Determine group of (sub) objectives for given priority. Returns indizes of objectives (NOT min/max!)
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {number} priority - Priority
 * @return {number} subObjectives - Sub objectives for given priority
 */
sMoop.prototype.subObjectives = function(problem, priority) {
  // determine subobjectives
  var objectives = [];
  for(var i = 0; i < problem.priorities.length; i++) {
    if(priority === problem.priorities[i]) {
      objectives.push(i);
    }
  }

  if(objectives.length < 1) {
    throw "Illegal state: No objectives found for given priority " + priority;
  }

  return objectives;
};

/**
 * Create groups of non-distinguishable points sorted in ascending order.
 *
 * @param {number[number[]]} sortedPoints - Sorted points.
 * @param {number} objectiveIndex - Group by objectiveIndex. May be undefined, entire point is compared to other points
 * @return {Object} P - groups of non-distinguishable points sorted in ascending order.
 */
sMoop.prototype.toP = function(sortedPoints, objectiveIndex) {
  // compare all values or only specific objective?
  var compareAll = isNaN(objectiveIndex);

  // P holds sets
  var P = [];
  var lastPoint;
  for(var i = 0; i < sortedPoints.length; i++) {
    var point = sortedPoints[i];

    var samePoint;
    if(lastPoint) {
      if(compareAll) {
        samePoint = true;
        for(var k = 0; k < point.length; k++) {
          if(point[k] != lastPoint[k]) {
            samePoint = false;
            break;
          }
        }
      } else {
        samePoint = point[objectiveIndex] === lastPoint[objectiveIndex];
      }
    }

    if(!lastPoint || !samePoint) {
      // add set to P
      P.push([]);
    }

    // add point
    P[P.length - 1].push(point);
    // set last point
    lastPoint = point;
  }

  return P;
};

/**
 * Resolves nested P!
 *
 * @param {Object} P - NESTED groups of non-distinguishable points sorted in ascending order.
 * @return {number[number[]]} sortedPoints - Unwind to sorted points
 */
sMoop.prototype.flattenNestedPoints = function(P) {
  var cArr = [];
  for(var i = 0; i < P.length; i++) {
    var pi = P[i];
    // check if first index contains an array
    if(Array.isArray(pi[0])) {
      // add nested points
      cArr = cArr.concat(this.flattenNestedPoints(pi));
    } else {
      // we discovered point
      cArr.push(pi);
    }
  }

  return cArr;
};

/**
 * Compute diversity based on sets size and points size. (1 ideal, range (0, 1], setSize / pointsSize).
 *
 * @param {number} setSize - Sets size
 * @param {number} pointsSize - Points size (length of all points)
 * @return {number} diversity - setSize / pointsSize
 */
sMoop.prototype.diversity = function(setSize, pointsSize) {
  // diversity (#sets divided by #points)
  var diversity = setSize / pointsSize;

  if(this.verbose) {
    console.log("set size " + setSize + " divided by points size " + pointsSize + " = " + diversity);
  }

  return diversity;
};

/**
 * Compute relevance (the smaller the distance value the better).
 * Duality principle is applied for max (converted to min).
 *
 * @param {number[number[]]} sortedPoints - Sorted points.
 * @param {number[]} objectives - Objectives
 * @param {number} topN - optional (may be undefined), restrict to top N instead of assuming all points for relevance
 * @param {Object} statsPerDim - Statistics per dimension (optional)
 * @param {number} static rank - used to assign same weight to non-distinguishable points (optional)
 * @return {number} relevance - MIN SUM(1/N x distance(R,I,C))
 */
sMoop.prototype.relevance = function(sortedPoints, objectives, topN, statsPerDim, staticRank) {
  // normalization of points in [0, 1] range
  var normalizedPoints = this.normalize(sortedPoints, statsPerDim);

  // apply duality principle if necessary (min --> max)
  for(var i = 0; i < normalizedPoints.length; i++) {
    var point = normalizedPoints[i];

    for(var y = 0; y < objectives.length; y++) {
      // make to min if max objective (duality principle)
      if(objectives[y] == 1) {
        point[y] = point[y] * -1;
      }
    }
  }

  // get stats per objective to determine ideal vector (stats already provided?, if not compute)
  var normStatsPerDim = statsPerDim ? statsPerDim : this.stats(normalizedPoints);

  // create zero array
  var idealVectorArr = [];
  for(var i = 0; i < objectives.length; i++) {
      if(objectives[y] == 1) {
    	  // max value
    	  idealVectorArr.push(normStatsPerDim[i].max);  
      } else {
    	  // min value
    	  idealVectorArr.push(normStatsPerDim[i].min);  
      }
  }
  
  console.log("Constructed reference point: " + JSON.stringify(idealVectorArr));
  
  // compute distance to ideal (utopian) solution (ideal objectives vector, here zero vector)
  var idealVector = Vector.create(idealVectorArr);

  // min (sum(1/N * distance(N, I, C))) where N amount of candidates, I ideal ranking and C current ranking
  // relevance
  var rankingLength = isNaN(topN) ? sortedPoints.length : topN;

  // adjust to the smaller value
  if(rankingLength > sortedPoints.length) {
    rankingLength = sortedPoints.length;
  }
  // invalid
  if(rankingLength < 1) {
    throw "Illegal topN value passed, must be greater than 0: " + topN;
  }

  var relevance = 0;
  for(var i = 0; i < rankingLength; i++) {
    // current point
    var point = normalizedPoints[i];

    //console.log("relevance np " + JSON.stringify(point));

    // compute distance
    var distance = idealVector.distanceFrom(Vector.create(point));

    // current rank, starting from 1
    var rank = !isNaN(staticRank) ? staticRank : i + 1;
    var scaleFactor = 1;
    // sum up
    var rel = (1/rank) * distance * scaleFactor;
    
    relevance += rel;
  }

  return relevance;
};

/**
 * Compute relevance (the smaller the distance value the better).
 * Duality principle is applied for max (converted to min).
 *
 * @param {number[number[]]} sortedPoints - Sorted points.
 * @param {number[]} objectives - Objectives
 * @param {number} topN - optional (may be undefined), restrict to top N instead of assuming all points for relevance
 * @param {Object} statsPerDim - Statistics per dimension (optional)
 * @param {number} static rank - used to assign same weight to non-distinguishable points (optional)
 * @return {number} relevance - MIN SUM(distance(R,I,C)) / N where N denotes the number of candidates
 */
sMoop.prototype.sumRelevance = function(sortedPoints, objectives, topN, statsPerDim, staticRank) {
  // normalization of points in [0, 1] range
  var normalizedPoints = this.normalize(sortedPoints, statsPerDim);

  // apply duality principle if necessary (min --> max)
  for(var i = 0; i < normalizedPoints.length; i++) {
    var point = normalizedPoints[i];

    for(var y = 0; y < objectives.length; y++) {
      // make to min if max objective (duality principle)
      if(objectives[y] == 1) {
        point[y] = point[y] * -1;
      }
    }
  }

  // get stats per objective to determine ideal vector (stats already provided?, if not compute)
  var normStatsPerDim = statsPerDim ? statsPerDim : this.stats(normalizedPoints);

  // create zero array
  var idealVectorArr = [];
  for(var i = 0; i < objectives.length; i++) {
      if(objectives[y] == 1) {
    	  // max value
    	  idealVectorArr.push(normStatsPerDim[i].max);  
      } else {
    	  // min value
    	  idealVectorArr.push(normStatsPerDim[i].min);  
      }
  }
  
  console.log("Constructed reference point: " + JSON.stringify(idealVectorArr));
  
  // compute distance to ideal (utopian) solution (ideal objectives vector, here zero vector)
  var idealVector = Vector.create(idealVectorArr);

  // min (sum(1/N * distance(N, I, C))) where N amount of candidates, I ideal ranking and C current ranking
  // relevance
  var rankingLength = isNaN(topN) ? sortedPoints.length : topN;

  // adjust to the smaller value
  if(rankingLength > sortedPoints.length) {
    rankingLength = sortedPoints.length;
  }
  // invalid
  if(rankingLength < 1) {
    throw "Illegal topN value passed, must be greater than 0: " + topN;
  }

  var relevance = 0;
  for(var i = 0; i < rankingLength; i++) {
    // current point
    var point = normalizedPoints[i];

    //console.log("relevance np " + JSON.stringify(point));

    // compute distance
    var distance = idealVector.distanceFrom(Vector.create(point));
    
    var scaleFactor = 1;
    
    relevance += distance * scaleFactor;
  }

  relevance /= rankingLength;
  
  return relevance;
};

/**
 * Compute reference point distance score (the smaller the distance value the better).
 * Duality principle is applied for max (converted to min).
 *
 * @param pointsObj - Points/Component lookup
 * @param {number[number[]]} sortedPoints - Sorted points.
 * @param {number[]} objectives - Objectives
 * @param {number} topN - optional (may be undefined), restrict to top N instead of assuming all points for relevance
 */
sMoop.prototype.setReferencePointDistanceScore = function(pointsObj, sortedPoints, objectives, topN) {
  // normalization of points in [0, 1] range
  var normalizedPoints = this.normalize(sortedPoints);

  // apply duality principle if necessary (min --> max)
  for(var i = 0; i < normalizedPoints.length; i++) {
    var point = normalizedPoints[i];

    for(var y = 0; y < objectives.length; y++) {
      // make to min if max objective (duality principle)
      if(objectives[y] == 1) {
        point[y] = point[y] * -1;
      }
    }
  }
  
  // get stats per objective to determine ideal vector
  var normStatsPerDim = this.stats(normalizedPoints);

  // create zero array
  var idealVectorArr = [];
  for(var i = 0; i < objectives.length; i++) {
      if(objectives[y] == 1) {
    	  // max value
    	  idealVectorArr.push(normStatsPerDim[i].max);  
      } else {
    	  // min value
    	  idealVectorArr.push(normStatsPerDim[i].min);  
      }
  }
  
  console.log("Constructed reference point: " + JSON.stringify(idealVectorArr));
  
  // compute distance to ideal (utopian) solution (ideal objectives vector, here zero vector)
  var idealVector = Vector.create(idealVectorArr);

  // min (sum(1/N * distance(N, I, C))) where N amount of candidates, I ideal ranking and C current ranking
  // relevance
  var rankingLength = isNaN(topN) ? sortedPoints.length : topN;

  // adjust to the smaller value
  if(rankingLength > sortedPoints.length) {
    rankingLength = sortedPoints.length;
  }
  // invalid
  if(rankingLength < 1) {
    throw "Illegal topN value passed, must be greater than 0: " + topN;
  }

  for(var i = 0; i < rankingLength; i++) {
    // current point
    var point = normalizedPoints[i];

    // compute distance
    var distance = idealVector.distanceFrom(Vector.create(point));
    
    // add relevance score to candidate(s)
    var cArr = pointsObj.lookup[sortedPoints[i]];
	for (var k = 0; k < cArr.length; k++) {
		var component = cArr[k];
		var ranking = component.ranking;
		if (!ranking) {
			ranking = {};

			component.ranking = ranking;
		}
		// set score
		ranking['rpDistanceScore'] = distance;
	}
  }
};

/**
 * Accounting for proper reference point. Compute partial relevance (the smaller the distance value the better). Accounts for sets and treats them as non-distinguishable concerning relevance (i.e. rank).
 * Duality principle is applied for max (converted to min).
 *
 * Only applicable for one nesting level (P contains pis)
 *
 * @param {Object} P - groups of non-distinguishable points sorted in ascending order.
 * @param {number[]} objectives - Objectives
 * @param {number} topN - optional (may be undefined), restrict to top N instead of assuming all points for relevance
 * @return {number} partial relevance - MIN SUM(1/N x distance(R,I,C))
 */
sMoop.prototype.partialRelevanceForReferencePoint = function(sortedPoints, P, objectives, topN) {
  // we need to flatten P to get normalized points for all nested points
  var nestedPoints = this.flattenNestedPoints(P);

  // get statistics to get min max for all point dims (ALL points!, not only P nested points)
  // normalization of points in [0, 1] range
  var normalizedPoints = this.normalize(sortedPoints);
  // apply duality principle if necessary (min --> max)
  for(var i = 0; i < normalizedPoints.length; i++) {
    var point = normalizedPoints[i];

    for(var y = 0; y < objectives.length; y++) {
      // make to min if max objective (duality principle)
      if(objectives[y] == 1) {
        point[y] = point[y] * -1;
      }
    }
  }
  
  var statsPerDim = this.stats(normalizedPoints);

  var partialRelevance = 0;
  // get relevance for each
  for(var i = 0; i < P.length; i++) {
    var pi = P[i];

    var rank = i + 1;
    
    //console.log("rank " + rank + " rel " + partialRelevance);
    
    partialRelevance += this.relevance(pi, objectives, topN, statsPerDim, rank);
  }

  return partialRelevance;
}

/**
 * Ranking analysis collector used to collect measures to compute ranking performance metrics.
 *
 * @constructor
 */
function rankingAnalysisCollector() {
  // size of nesting levels
  this.nestingLevels = undefined;
  // holds tree of current visited Ps
  this.Ptree = {};
}

/**
 * Listener method which gets notified on current nesting level and P. Counts set splits and decrements in case of nesting level > 0.
 *
 * @param {Object} problem - Problem object must contain points, objectives and priorities.
 * @param {number} nestingLevel - Current nesting level (starts at 0).
 * @param {Object} P - groups of non-distinguishable points sorted in ascending order.
 */
rankingAnalysisCollector.prototype.onSetSplit = function(problem, nestingLevel, P) {
  if(!this.nestingLevels) {
    this.nestingLevels = problem.priorities.length;
  }

  if(this.verbose) {
    console.log("adddding " + P.length + " nesting level " + nestingLevel + " max levels " + this.nestingLevels);
  }

  // add to tree
  if(!this.Ptree.hasOwnProperty(nestingLevel)) {
    this.Ptree[nestingLevel] = [];
  }

  this.Ptree[nestingLevel] = this.Ptree[nestingLevel].concat(P);

  if(this.verbose) {
      console.log("adddding " + JSON.stringify(P));
  }
};

/**
 * Creates a one-level set of sets.
 *
 * @return {Array} - non-distinguishable sets of points
 */
rankingAnalysisCollector.prototype.nonDistinguishableSets = function() {
  var levels = [];
  for(var nestingLevel in this.Ptree) {
    if(this.Ptree.hasOwnProperty(nestingLevel)) {
      levels.push(nestingLevel);
    }
  }

  // sort descending
  levels = levels.sort(function(a, b) {
    return b - a;
  });

  // return all Ps from last level
  var lastLevel = levels[0];

  console.log("Last level " + lastLevel);

  var P = this.Ptree[lastLevel].slice();

  console.log("Last level P length " + P.length);

  return P;
};

/**
 * Get diversity count
 *
 * @return {Number} - Number of non-distinguishable sets
 */
rankingAnalysisCollector.prototype.diversityCount = function() {
  var ndSets = this.nonDistinguishableSets();

  return ndSets.length;
};
