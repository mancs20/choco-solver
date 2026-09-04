/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.loop.monitors.IMonitorInitialize;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for the PAUGMECON Pareto-front algorithm.
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public class PaugmeconParetoFrontFinderTest {

    @Test(groups = "1s", timeOut = 60000)
    public void testTwoObjectives() {
        Model model = new Model();
        IntVar a = model.intVar("a", 0, 2);
        IntVar b = model.intVar("b", 0, 2);
        model.arithm(a, "+", b, "<=", 2).post();

        model.getSolver().setSearch(Search.inputOrderLBSearch(a,b));

        List<Solution> front = model.getSolver().findParetoFront(
                new IntVar[]{a, b}, true, ParetoFrontAlgorithm.PAUGMECON
        );

        assertFrontEquals(front, Set.of("0,2", "2,0", "1,1"), a, b);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testMinimization() {
        Model model = new Model();
        IntVar a = model.intVar("a", 0, 2);
        IntVar b = model.intVar("b", 0, 2);
        model.arithm(a, "+", b, ">=", 2).post();

        List<Solution> front = model.getSolver().findParetoFront(
                new IntVar[]{a, b}, false, ParetoFrontAlgorithm.PAUGMECON
        );

        assertFrontEquals(front, Set.of("0,2", "1,1", "2,0"), a, b);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testThreeObjectives() {
        Model model = new Model();
        IntVar a = model.intVar("a", 0, 2);
        IntVar b = model.intVar("b", 0, 2);
        IntVar c = model.intVar("c", 0, 2);
        model.sum(new IntVar[]{a, b, c}, "=", 2).post();

        List<Solution> front = model.getSolver().findParetoFront(
                new IntVar[]{a, b, c}, true, ParetoFrontAlgorithm.PAUGMECON
        );

        assertFrontEquals(front,
                Set.of("0,0,2", "0,1,1", "0,2,0", "1,0,1", "1,1,0", "2,0,0"),
                a, b, c);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testStopWhileFindingIndividualOptima() {
        Model model = new Model();
        IntVar a = model.intVar("a", 0, 2);
        IntVar b = model.intVar("b", 0, 2);
        IntVar c = model.intVar("c", 0, 2);
        model.sum(new IntVar[]{a, b, c}, "=", 2).post();

        SolutionCounter stop = new SolutionCounter(model.getSolver(), 1);
        List<Solution> front = model.getSolver().findParetoFront(
                new IntVar[]{a, b, c}, true, ParetoFrontAlgorithm.PAUGMECON, stop
        );

        assertFrontEquals(front, Set.of("0,2,0"), a, b, c);
        Assert.assertEquals(model.getSolver().getSearchState(), SearchState.STOPPED);
        Assert.assertFalse(front.isEmpty(), "Solutions found before the stop should be returned");
        Assert.assertFalse(front.contains(null));
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testStopWhileFindingParetoOptimalSolution() {
        Model model = new Model();
        IntVar a = model.intVar("a", 0, 2);
        IntVar b = model.intVar("b", 0, 2);
        IntVar c = model.intVar("c", 0, 2);
        model.sum(new IntVar[]{a, b, c}, "=", 2).post();

        SolutionCounter stop = new SolutionCounter(model.getSolver(), 4);
        List<Solution> front = model.getSolver().findParetoFront(
                new IntVar[]{a, b, c}, true, ParetoFrontAlgorithm.PAUGMECON, stop
        );

        assertFrontEquals(front, Set.of("0,2,0", "0,0,2", "0,1,1", "2,0,0"), a, b, c);
        Assert.assertEquals(model.getSolver().getSearchState(), SearchState.STOPPED);
        Assert.assertFalse(front.isEmpty(), "Solutions found before the stop should be returned");
        Assert.assertFalse(front.contains(null));
    }

    // todo here check the number of infeasible searches or something like that in the stats, maybe debug because now we cannot access to the accumulative stats, ask that to Charles,also the stats after reset, and knapsack.
    /**
     * Uses the objective vectors from the SAUGMECON example. The example includes grid points that can be skipped
     * because they are infeasible or already covered by a solution obtained for a previous epsilon vector.
     */
    @Test(groups = "1s", timeOut = 60000)
    public void testSaugmeconExample() {
        Model model = new Model();
        IntVar f1 = model.intVar("f1", 1, 5);
        IntVar f2 = model.intVar("f2", 1, 5);
        IntVar f3 = model.intVar("f3", 1, 5);

        Tuples feasibleObjectiveVectors = createSimpleExampleFeasibleObjectiveVectors();
        model.table(new IntVar[]{f1, f2, f3}, feasibleObjectiveVectors).post();

        // The figure displays (f2, f3, f1): the main objective f1 is the last coordinate.
        IntVar[] displayedObjectives = new IntVar[]{f2, f3, f1};
        model.getSolver().setSearch(Search.inputOrderLBSearch(displayedObjectives));
        PaugmeconParetoFrontFinder finder = new PaugmeconParetoFrontFinder();
        Assert.assertNull(finder.getCurrentEpsilon());

        List<String> solutionsFoundBySearch = new ArrayList<>();
        List<String> solutionsFoundDuringGridSearch = new ArrayList<>();
        List<String> epsilonAtSearchInitialization = new ArrayList<>();
        model.getSolver().plugMonitor(new IMonitorInitialize() {
            @Override
            public void beforeInitialize() {
                int[] epsilon = finder.getCurrentEpsilon();
                if (epsilon != null) {
                    epsilonAtSearchInitialization.add(Arrays.toString(epsilon));
                }
            }
        });
        model.getSolver().plugMonitor((IMonitorSolution) () -> {
            String objectiveVector = currentPoint(displayedObjectives);
            solutionsFoundBySearch.add(objectiveVector);
            int[] epsilon = finder.getCurrentEpsilon();
            if (epsilon != null) {
                Assert.assertTrue(f2.getValue() >= epsilon[0],
                        "The solution does not satisfy the current epsilon bound on f2");
                Assert.assertTrue(f3.getValue() >= epsilon[1],
                        "The solution does not satisfy the current epsilon bound on f3");
                solutionsFoundDuringGridSearch.add(objectiveVector);
            }
        });

        List<Solution> front = finder.findParetoFront(
                model.getSolver(), new IntVar[]{f1, f2, f3}, true
        );

        assertFrontEquals(front, Set.of("3,3,5", "5,2,5", "5,3,4", "2,4,3", "4,5,2"),
                displayedObjectives);

        Set<String> finalFront = points(front, displayedObjectives);
        List<String> paretoPointsInDiscoveryOrder = solutionsFoundBySearch.stream()
                .filter(finalFront::contains)
                .toList();
        Assert.assertEquals(paretoPointsInDiscoveryOrder,
                List.of("2,4,3", "3,3,5", "5,2,5", "4,5,2", "5,3,4"),
                "Pareto points were not discovered in the expected order");

        Assert.assertTrue(solutionsFoundBySearch.contains("5,1,1"),
                "The first dominated preprocessing solution was not found: " + solutionsFoundBySearch);
        Assert.assertTrue(solutionsFoundBySearch.contains("1,5,1"),
                "The second dominated preprocessing solution was not found: " + solutionsFoundBySearch);
        Assert.assertFalse(finalFront.contains("5,1,1"),
                "The dominated preprocessing solution was not removed from the archive");
        Assert.assertFalse(finalFront.contains("1,5,1"),
                "The dominated preprocessing solution was not removed from the archive");

        Assert.assertEquals(solutionsFoundDuringGridSearch,
                List.of("2,4,2", "2,4,3", "3,3,4", "3,3,5", "5,2,5", "4,5,2", "5,3,4"),
                "For inputOrderLBSearch the grid-search mechanism should find the solutions in that order");
        Assert.assertFalse(finalFront.contains("2,4,2"),
                "A dominated grid-search intermediate solution remained in the final archive");
        Assert.assertFalse(finalFront.contains("3,3,4"),
                "A dominated grid-search intermediate solution remained in the final archive");
        Assert.assertTrue(solutionsFoundBySearch.size() > front.size(),
                "The example should visit intermediate/dominated solutions in addition to the final front");
        // The feasible epsilon points [4,1], [4,3], [3,4] and [1,5] reuse previously found Pareto points.
        // The infeasible points [5,4] and [5,5] are also handled without initializing another search.
        Assert.assertEquals(epsilonAtSearchInitialization, List.of("[1, 1]", "[1, 3]", "[1, 4]"),
                "Unexpected epsilon values at the start of the grid searches");
        Assert.assertEquals(finder.getCurrentEpsilon(), new int[]{1, 6},
                "The final epsilon should violate the loop condition");
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testUpdateEpsilonAfterSolutions() {
        int[] epsilon = {1, 1};
        int[] ideal = {5, 5};
        int[] nadir = {1, 1};
        int[] relativeWorstValue = ideal.clone();

        PaugmeconParetoFrontFinder.updateEpsilon(
                new int[]{5, 3, 3}, epsilon, ideal, nadir, relativeWorstValue
        );
        Assert.assertEquals(epsilon, new int[]{4, 1});
        Assert.assertEquals(relativeWorstValue, new int[]{5, 3});

        PaugmeconParetoFrontFinder.updateEpsilon(
                new int[]{5, 5, 2}, epsilon, ideal, nadir, relativeWorstValue
        );
        Assert.assertEquals(epsilon, new int[]{1, 3});
        Assert.assertEquals(relativeWorstValue, new int[]{5, 5});
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testUpdateEpsilonAfterInfeasibleGridPoint() {
        int[] epsilon = {5, 4};
        int[] ideal = {5, 5};
        int[] nadir = {1, 1};
        int[] relativeWorstValue = {5, 4};

        PaugmeconParetoFrontFinder.updateEpsilon(
                null, epsilon, ideal, nadir, relativeWorstValue
        );

        Assert.assertEquals(epsilon, new int[]{1, 5});
        Assert.assertEquals(relativeWorstValue, new int[]{5, 5});
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testUpdateEpsilonExitsEnumeration() {
        int[] epsilon = {5, 5};
        int[] ideal = {5, 5};
        int[] nadir = {1, 1};
        int[] relativeWorstValue = ideal.clone();

        PaugmeconParetoFrontFinder.updateEpsilon(
                new int[]{5, 5, 5}, epsilon, ideal, nadir, relativeWorstValue
        );

        Assert.assertEquals(epsilon, new int[]{1, 6});
        Assert.assertTrue(epsilon[epsilon.length - 1] > ideal[ideal.length - 1]);
    }

    private static Tuples createSimpleExampleFeasibleObjectiveVectors() {
        Tuples feasibleObjectiveVectors = new Tuples(true);
        // Dominated vectors deliberately encountered during individual optimization.
        feasibleObjectiveVectors.add(1, 5, 1);
        feasibleObjectiveVectors.add(1, 1, 5);
        feasibleObjectiveVectors.add(1, 2, 1);
        feasibleObjectiveVectors.add(1, 3, 1);
        feasibleObjectiveVectors.add(1, 4, 1);
        feasibleObjectiveVectors.add(1, 2, 4);
        feasibleObjectiveVectors.add(1, 2, 5);
        // Dominated intermediate vectors deliberately encountered during grid search.
        feasibleObjectiveVectors.add(2, 2, 4);
        feasibleObjectiveVectors.add(4, 3, 3);
        feasibleObjectiveVectors.add(5, 3, 3);
        feasibleObjectiveVectors.add(5, 5, 2);
        feasibleObjectiveVectors.add(4, 5, 3);
        feasibleObjectiveVectors.add(3, 2, 4);
        feasibleObjectiveVectors.add(2, 4, 5);
        return feasibleObjectiveVectors;
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testNegativeObjectiveValues() {
        Model model = new Model();
        IntVar a = model.intVar("a", -2, 2);
        IntVar b = model.intVar("b", -2, 2);
        model.arithm(a, "+", b, "=", 0).post();

        List<Solution> front = model.getSolver().findParetoFront(
                new IntVar[]{a, b}, true, ParetoFrontAlgorithm.PAUGMECON
        );

        assertFrontEquals(front, Set.of("-2,2", "-1,1", "0,0", "1,-1", "2,-2"), a, b);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testTemporaryConstraintsAreRemoved() {
        Model model = new Model();
        IntVar a = model.intVar("a", 0, 2);
        IntVar b = model.intVar("b", 0, 2);
        model.arithm(a, "+", b, "=", 2).post();
        int initialConstraintCount = model.getNbCstrs();

        model.getSolver().findParetoFront(
                new IntVar[]{a, b}, true, ParetoFrontAlgorithm.PAUGMECON
        );

        Assert.assertEquals(model.getNbCstrs(), initialConstraintCount);
        Assert.assertNull(model.getObjective());
    }

    private void assertFrontEquals(List<Solution> front, Set<String> expected, IntVar... objectives) {
        Assert.assertFalse(front.contains(null), "The returned front contains a null solution");

        Set<String> actual = points(front, objectives);

        if (!actual.equals(expected) || front.size() != expected.size()) {
            Assert.fail(
                    "Pareto fronts differ:\n" +
                            "Expected: " + expected + "\n" +
                            "Actual:   " + actual + "\n" +
                            "Returned solutions: " + front.size()
            );
        }
    }

    private Set<String> points(List<Solution> front, IntVar... objectives) {
        Set<String> points = new HashSet<>();

        for (Solution solution : front) {
            StringBuilder point = new StringBuilder();
            for (IntVar objective : objectives) {
                if (!point.isEmpty()) {
                    point.append(',');
                }
                point.append(solution.getIntVal(objective));
            }
            points.add(point.toString());
        }

        return points;
    }

    private String currentPoint(IntVar... objectives) {
        StringBuilder point = new StringBuilder();
        for (IntVar objective : objectives) {
            if (!point.isEmpty()) {
                point.append(',');
            }
            point.append(objective.getValue());
        }
        return point.toString();
    }

}
