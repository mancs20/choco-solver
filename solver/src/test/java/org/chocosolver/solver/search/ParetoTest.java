/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2024, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.objective.ParetoMaximizerGIACoverage;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.RegionStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;

/**
 * Created by cprudhom on 18/02/15.
 * Project: choco.
 */
public class ParetoTest {

    int bestProfit1 = 0;

    //******************************************************************
    // MAIN
    //******************************************************************

    @Test(groups = "1s", timeOut = 60000)
    public void testPareto() {
        runKnapsackPareto(30, "10;1;2;5", "5;3;7;4", "2;5;11;3");
        Assert.assertTrue(bestProfit1 > 60);
    }

    private void runKnapsackPareto(final int capacity, final String... items) {
        int[] nbItems = new int[items.length];
        int[] weights = new int[items.length];
        int[] profits_1 = new int[items.length];
        int[] profits_2 = new int[items.length];

        int maxProfit_1 = 0;
        int maxProfit_2 = 0;
        for (int it = 0; it < items.length; it++) {
            String item = items[it];
            item = item.trim();
            final String[] itemData = item.split(";");
            nbItems[it] = parseInt(itemData[0]);
            weights[it] = parseInt(itemData[1]);
            profits_1[it] = parseInt(itemData[2]);
            profits_2[it] = parseInt(itemData[3]);
            maxProfit_1 += nbItems[it] * profits_1[it];
            maxProfit_2 += nbItems[it] * profits_2[it];
        }

        Model s = new Model("Knapsack");
        // --- Creates decision variables
        IntVar[] occurrences = new IntVar[nbItems.length];
        for (int i = 0; i < nbItems.length; i++) {
            occurrences[i] = s.intVar("occurrences_" + i, 0, nbItems[i], true);
        }
        IntVar totalWeight = s.intVar("totalWeight", 0, capacity, true);
        IntVar totalProfit_1 = s.intVar("totalProfit_1", 0, maxProfit_1, true);
        IntVar totalProfit_2 = s.intVar("totalProfit_2", 0, maxProfit_2, true);

        // --- Posts constraints
        s.knapsack(occurrences, totalWeight, totalProfit_1, weights, profits_1).post();
        s.knapsack(occurrences, totalWeight, totalProfit_2, weights, profits_2).post();
        // --- Monitor
        s.getSolver().plugMonitor((IMonitorSolution) () -> bestProfit1 = max(bestProfit1, totalProfit_1.getValue()));
        // --- Search
        s.getSolver().setSearch(Search.domOverWDegSearch(occurrences), Search.inputOrderLBSearch(totalProfit_1, totalProfit_2));
        // --- solve
        //ParetoMaximizer pareto = new ParetoMaximizer(Model.MAXIMIZE,new IntVar[]{totalProfit_1,totalProfit_2});
        //s.getSolver().plugMonitor(pareto);
        s.getSolver().showShortStatistics();
        List<Solution> front = s.getSolver().findParetoFront(new IntVar[]{totalProfit_1, totalProfit_2}, Model.MAXIMIZE);
        System.out.println("Pareto Front:");
        for (Solution sol : front) {
            System.out.println(sol.getIntVal(totalProfit_1) + " // " + sol.getIntVal(totalProfit_2));
        }
    }

    @Test(groups = "10s")
    public void testMOQAP() {
        runMOQAP();
    }

    private void runMOQAP() {
        Model m = new Model();
        int n = 10;
        int[][] w1 = {
                {0, 0, 132, 0, 2558, 667, 0, 572, 1, 200},
                {0, 0, 990, 140, 9445, 1397, 7, 0, 100, 0},
                {132, 990, 0, 7, 40, 2213, 0, 1, 0, 0},
                {0, 140, 7, 0, 58, 0, 1, 0, 4, 70},
                {2558, 9445, 40, 58, 0, 3, 0, 0, 0, 0},
                {667, 1397, 2213, 0, 3, 0, 139, 3, 5169, 101},
                {0, 7, 0, 1, 0, 139, 0, 0, 5659, 0},
                {572, 0, 1, 0, 0, 3, 0, 0, 3388, 1982},
                {1, 100, 0, 4, 0, 5169, 5659, 3388, 0, 1023},
                {200, 0, 0, 70, 0, 101, 0, 1982, 1023, 0}
        };
        int[][] w2 = {
                {0, 1, 0, 5379, 0, 0, 1, 0, 329, 856},
                {1, 0, 2029, 531, 15, 80, 197, 17, 274, 241},
                {0, 2029, 0, 1605, 0, 194, 0, 2, 4723, 0},
                {5379, 531, 1605, 0, 24, 68, 0, 0, 4847, 2205},
                {0, 15, 0, 24, 0, 1355, 5124, 1610, 0, 0},
                {0, 80, 194, 68, 1355, 0, 549, 0, 151, 2},
                {1, 197, 0, 0, 5124, 549, 0, 5955, 0, 0},
                {0, 17, 2, 0, 1610, 0, 5955, 0, 553, 710},
                {329, 274, 4723, 4847, 0, 151, 0, 553, 0, 758},
                {856, 241, 0, 2205, 0, 2, 0, 710, 758, 0},
        };
        int[][] d = new int[][]{
                {0, 3, 60, 62, 155, 29, 47, 78, 83, 102},
                {3, 0, 57, 61, 152, 27, 44, 74, 80, 99},
                {60, 57, 0, 58, 95, 47, 32, 19, 25, 48},
                {62, 61, 58, 0, 141, 33, 27, 61, 61, 65},
                {155, 152, 95, 141, 0, 142, 123, 82, 80, 80},
                {29, 27, 47, 33, 142, 0, 21, 60, 63, 78},
                {47, 44, 32, 27, 123, 21, 0, 41, 43, 57},
                {78, 74, 19, 61, 82, 60, 41, 0, 7, 31},
                {83, 80, 25, 61, 80, 63, 43, 7, 0, 23},
                {102, 99, 48, 65, 80, 78, 57, 31, 23, 0}
        };
        IntVar[] x = m.intVarArray("X", n, 1, n);
        // limitations for test purpose
        x[0].eq(1).post();
        IntVar[][] dist = m.intVarMatrix("dist", n, n, 0, 200);
        IntVar obj1 = m.intVar("O1", 0, 9_999_999);
        IntVar obj2 = m.intVar("O2", 0, 9_999_999);

        m.allDifferent(x).post();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m.element(dist[i][j], d, x[i], 1, x[j], 1);
            }
        }
        m.scalar(ArrayUtils.flatten(dist), ArrayUtils.flatten(w1), "=", obj1).post();
        m.scalar(ArrayUtils.flatten(dist), ArrayUtils.flatten(w2), "=", obj2).post();

        m.getSolver().setSearch(Search.inputOrderLBSearch(x));
        List<Solution> front = m.getSolver().findParetoFront(new IntVar[]{m.neg(obj1), m.neg(obj2)}, Model.MAXIMIZE);
        Assert.assertEquals(26, front.size());
        Assert.assertEquals(233, m.getSolver().getSolutionCount());
        Assert.assertEquals(95208, m.getSolver().getNodeCount());
    }

    @Test(groups = "10s")
    public void testParetoStrategyByRegionWithoutHardReset(){
        Object[] modelAndObjectives = createConflinctingModelKnapSackExample();
        Model model = (Model) modelAndObjectives[0];
        IntVar[] objectives = (IntVar[]) modelAndObjectives[1];

        // define the regions to search
        int[][] boundsObj1 = new int[][]{
                {15901,18278},
                {18278,23349},
                {0,15901}};
        int[][] boundsObj2 = new int[][]{
                {14103,17621},
                {0,14103},
                {17621,21741}};

        // variable for the optimization version
        int obj1UB = objectives[0].getUB();
        int obj2UB = objectives[1].getUB();

        ParetoMaximizerGIACoverage paretoPoint = new ParetoMaximizerGIACoverage(objectives, false, PropagatorPriority.BINARY);
        Constraint c = new Constraint("PARETOOPTALLOBJ", paretoPoint);
        c.post();

        int idBounds = 0;

        //search strategy
        IntVar[] tempModelVars = model.retrieveIntVars(true);
        IntVar[] notObjectivesVars = new IntVar[tempModelVars.length - objectives.length];
        int index = 0;
        for (int i = 0; i < tempModelVars.length; i++) {
            if (tempModelVars[i] != objectives[0] && tempModelVars[i] != objectives[1]) {
                notObjectivesVars[index] = tempModelVars[i];
                index++;
            }
        }

        int[] initialBounds = new int[objectives.length * 2];
        // IMPORTANT add tge region strategy as the first strategy in the search
        RegionStrategy regionStrategy = new RegionStrategy(objectives, initialBounds);
        model.getSolver().setSearch(regionStrategy,
                Search.domOverWDegRefSearch(notObjectivesVars), Search.domOverWDegRefSearch(objectives));
        // if restart is used
        model.getSolver().plugMonitor(regionStrategy);

        while (idBounds < 3){
            // select the region
            int[] boundObj1Current = boundsObj1[idBounds];
            int[] boundObj2Current = boundsObj2[idBounds];
            int[] lowerBounds = new int[]{boundObj1Current[0] + 1, boundObj2Current[0] +1};
            int[] upperBounds = new int[]{boundObj1Current[1], boundObj2Current[1]};
            idBounds++;

            int[] lowerCorner = {boundObj1Current[0] + 1, boundObj2Current[0] +1};

            int[] bounds = new int[objectives.length * 2];
            for (int i = 0; i < objectives.length; i++){
                bounds[i] = lowerCorner[i];
                bounds[i + objectives.length] = upperBounds[i];
            }
            int[] boundsLow = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++){
                boundsLow[i] = lowerCorner[i];
            }

            // IMPORTANT add the new bounds in every loop like this to avoid adding the search again.
            AbstractStrategy usedSearch = model.getSolver().getSearch();
            if (usedSearch instanceof StrategiesSequencer) {
                AbstractStrategy firstSearch = ((StrategiesSequencer) usedSearch).getStrategies()[0];
                if (firstSearch instanceof RegionStrategy) {
                    ((RegionStrategy) firstSearch).setBounds(bounds);
                }
            }

            ParetoFeasibleRegion feasibleRegion = new ParetoFeasibleRegion(new int[]{0,0}, new int[]{obj1UB, obj2UB});
            paretoPoint.configureInitialUbLb(feasibleRegion);

            boolean solutionFound = false;
            paretoPoint.setLastSolution(null);
            Solution solution = new Solution(model);

            while (model.getSolver().solve()) {
                paretoPoint.onSolution();
                solution.record();
                solutionFound = true;
            }
            // Get statistics
            System.out.println(model.getSolver().getMeasures().toString());
            if (solutionFound){
                System.out.println("Solution found in region: " + Arrays.toString(boundObj1Current) + " " +
                        Arrays.toString(boundObj2Current));
                System.out.println("Objectives values: " + solution.getIntVal(objectives[0]) + " - " +
                        solution.getIntVal(objectives[1]));
            }else{
                System.out.println("No solution found in region: " + Arrays.toString(boundObj1Current) + " " + Arrays.toString(boundObj2Current));
                Assert.assertEquals(lowerCorner, new int[]{18279, 1}); // Here is in the only region that we should not find a solution
            }

            model.getSolver().reset();
        }
    }

    @Test(groups = "10s")
    public void testParetoDifferentRegionsConstraintWithoutHardReset(){
        Object[] modelAndObjectives = createConflinctingModelKnapSackExample();
        Model model = (Model) modelAndObjectives[0];
        IntVar[] objectives = (IntVar[]) modelAndObjectives[1];

        // define the regions to search
        int[][] boundsObj1 = new int[][]{
                {15901,18278},
                {18278,23349},
                {0,15901}};
        int[][] boundsObj2 = new int[][]{
                {14103,17621},
                {0,14103},
                {17621,21741}};

        // variable for the optimization version
        int obj1UB = objectives[0].getUB();
        int obj2UB = objectives[1].getUB();

        ParetoMaximizerGIACoverage paretoPoint = new ParetoMaximizerGIACoverage(objectives, false, PropagatorPriority.BINARY);
        Constraint c = new Constraint("PARETOOPTALLOBJ", paretoPoint);
        c.post();

        int idBounds = 0;

        //search strategy
        IntVar[] tempModelVars = model.retrieveIntVars(true);
        IntVar[] notObjectivesVars = new IntVar[tempModelVars.length - objectives.length];
        int index = 0;
        for (int i = 0; i < tempModelVars.length; i++) {
            if (tempModelVars[i] != objectives[0] && tempModelVars[i] != objectives[1]) {
                notObjectivesVars[index] = tempModelVars[i];
                index++;
            }
        }

        int[] initialBounds = new int[objectives.length * 2];
        // IMPORTANT add tge region strategy as the first strategy in the search
//        RegionStrategy regionStrategy = new RegionStrategy(objectives, initialBounds);
//        model.getSolver().setSearch(regionStrategy,
//                Search.domOverWDegRefSearch(notObjectivesVars), Search.domOverWDegRefSearch(objectives));
//        // if restart is used
//        model.getSolver().plugMonitor(regionStrategy);

        long solutionCounter = 0;
        while (idBounds < 3){
            // select the region
            int[] boundObj1Current = boundsObj1[idBounds];
            int[] boundObj2Current = boundsObj2[idBounds];
            int[] lowerBounds = new int[]{boundObj1Current[0] + 1, boundObj2Current[0] +1};
            int[] upperBounds = new int[]{boundObj1Current[1], boundObj2Current[1]};
            idBounds++;

            int[] lowerCorner = {boundObj1Current[0] + 1, boundObj2Current[0] +1};

            int[] bounds = new int[objectives.length * 2];
            for (int i = 0; i < objectives.length; i++){
                bounds[i] = lowerCorner[i];
                bounds[i + objectives.length] = upperBounds[i];
            }
            int[] boundsLow = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++){
                boundsLow[i] = lowerCorner[i];
            }

            // IMPORTANT add the new bounds in every loop like this to avoid adding the search again.
//            AbstractStrategy usedSearch = model.getSolver().getSearch();
//            if (usedSearch instanceof StrategiesSequencer) {
//                AbstractStrategy firstSearch = ((StrategiesSequencer) usedSearch).getStrategies()[0];
//                if (firstSearch instanceof RegionStrategy) {
//                    ((RegionStrategy) firstSearch).setBounds(bounds);
//                }
//            }

            ParetoFeasibleRegion feasibleRegion = new ParetoFeasibleRegion(lowerBounds, upperBounds);
//            ParetoFeasibleRegion feasibleRegion = new ParetoFeasibleRegion(new int[]{0,0}, new int[]{obj1UB, obj2UB});
            paretoPoint.configureInitialUbLb(feasibleRegion);

            boolean solutionFound = false;
            paretoPoint.setLastSolution(null);
            Solution solution = new Solution(model);

            while (model.getSolver().solve()) {
                paretoPoint.onSolution();
                solution.record();
                solutionFound = true;
            }

            // Get statistics
            System.out.println(model.getSolver().getMeasures().toString());
            if (solutionFound){
                System.out.println("Solution found in region: " + Arrays.toString(boundObj1Current) + " " +
                        Arrays.toString(boundObj2Current));
                System.out.println("Objectives values: " + solution.getIntVal(objectives[0]) + " - " +
                        solution.getIntVal(objectives[1]));
            }else{
                System.out.println("No solution found in region: " + Arrays.toString(boundObj1Current) + " " + Arrays.toString(boundObj2Current));
                Assert.assertEquals(lowerCorner, new int[]{18279, 1}); // Here is in the only region that we should not find a solution
            }

            model.getSolver().reset();
            // simulate a restart
            solutionCounter++;
            model.getSolver().getMeasures().setRestartCount(solutionCounter);
        }
    }

    private Object[] createConflinctingModelKnapSackExample() {
        int[] nbItems; // number of items for each type
        nbItems = new int[50];
        for (int i = 0; i < 50; i++) {
            nbItems[i] = 1;
        }
        int[] weights = new int[]{62, 300, 670, 372, 535, 162, 985, 952, 562, 417, 2, 817, 554, 9, 128, 619, 934, 805, 100, 334, 149, 668,
                647, 164, 709, 421, 838, 659, 152, 563, 134, 60, 374, 317, 478, 419, 807, 220, 511, 986, 928, 82, 841, 608, 874, 72, 458, 969, 912, 314};
        int[] obj1Coeff = new int[]{684, 45, 678, 805, 478, 549, 302, 485, 788, 623, 652, 583, 682, 641, 320, 258, 49, 402, 79, 123, 759, 563, 328,
                531, 400, 985, 889, 121, 130, 329, 618, 113, 224, 451, 512, 761, 577, 273, 753, 217, 881, 714, 889, 114, 391, 100, 471, 44, 751, 234};
        int[] obj2Coeff = new int[]{168, 547, 584, 513, 978, 417, 914, 225, 226, 65, 58, 519, 708, 429, 117, 213, 37, 69, 693, 734, 507, 279, 163,
                8, 321, 177, 978, 473, 712, 274, 665, 392, 778, 336, 18, 673, 58, 495, 460, 725, 285, 902, 859, 143, 171, 167, 732, 581, 926, 269};

        int knapsackCapacity = 11674;

        Model model = new Model("MultiObjKnapsackVoptLibK5050W06");

        // For each type, we create a variable for the number of occurrences
        IntVar[] occurrences = new IntVar[nbItems.length];
        for (int i = 0; i < nbItems.length; i++) {
            occurrences[i] = model.intVar(0, 1);
        }

        // model variables
        IntVar weight = model.intVar(0, knapsackCapacity);
        int obj1UB = Arrays.stream(obj1Coeff).sum();
        int obj2UB = Arrays.stream(obj2Coeff).sum();
        IntVar obj1 = model.intVar(0, obj1UB);
        IntVar obj2 = model.intVar(0, obj2UB);
        IntVar[] objectives = new IntVar[]{obj1, obj2};

        // We add two knapsack constraints to the solver
        // Beware : call the post() method to save it
        model.knapsack(occurrences, weight, obj1, weights, obj1Coeff).post();
        model.knapsack(occurrences, weight, obj2, weights, obj2Coeff).post();
        return new Object[]{model, objectives};
    }
}
