package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.ArrayList;
import java.util.List;

public class SaugmeconIntermediateFindSolution extends AbstractFindSolutionStrategy{

    private int mainObjectiveObtainedValue;

    public SaugmeconIntermediateFindSolution(SolutionFinder solutionFinder) {
        super(solutionFinder);
    }

    @Override
    public Solution find(Model model, ParetoArchive archive, IntVar[] objectives, Region region, StrategyParams params, Criterion... stop) {
        // params
        int[] epsilonArr = params.getEpsilonArray();

        //solution information
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = params.getPreviousSolutionInfo();
        ParetoArchive localArchive = new ParetoArchive(objectives);
        findAllArchivePointsForEpsilon(epsilonArr, archive, localArchive, params);

        // The version below aims to find only solutions that are better than the best solution in the sub-archive
        // (archive solutions affecting the current epsilon) for the main objective, which is the first objective
        // in the array. The problem with this approach is that in several occassion it won't find any solution, and
        // what we want is to maximize the number of non-dominated solutions found while solving one region.
//        ArrayList<Integer> solutionArchiveIndices = findArchivePointsForEpsilon(epsilonArr, archive, localArchive, params);
//        if (solutionArchiveIndices.size() > 0) {
//            int bestValueMainObjective = archive.getParetoFront().get(solutionArchiveIndices.get(0))[0];
//            lbMainObjectiveConstraint = model.arithm(objectives[0], ">=", bestValueMainObjective);
//            lbMainObjectiveConstraint.post();
//        }

        Solution solution = solutionFinder.find(model, localArchive, objectives, region, params, stop);
        if (solution == null) {
            // no solution in this region
            saveSolutionInformation(epsilonArr, null,  previousSolutionInformation);
        } else {
            archive.setCanAddSolution(true);
            int[] solutionObjectiveValues = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
            }
            mainObjectiveObtainedValue = solutionObjectiveValues[0];
            List<int[]> solutionsToAddToEpsilon = new ArrayList<>(List.of(solutionObjectiveValues));
            List<int[]> solutionsThatCouldHaveBeenObtained = mergeLocalArchiveIntoGlobal(localArchive, archive, solution);
            if (!solutionsThatCouldHaveBeenObtained.isEmpty()) {
                solutionsToAddToEpsilon.addAll(solutionsThatCouldHaveBeenObtained);
            }
            saveSolutionInformation(epsilonArr, solutionsToAddToEpsilon,  previousSolutionInformation);
        }

        return solution;
    }

    private static void saveSolutionInformation(int[] efArrayActual, List<int[]> solutionObjectiveValues, List<SolutionEpsilonArrayInformation> previousSolutionInformation) {
        boolean feasible = solutionObjectiveValues != null;
        SolutionEpsilonArrayInformation solutionEfArrayInformation;
        if (feasible && solutionObjectiveValues.size() == 1) {
            solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues.get(0), efArrayActual.clone(), true);
        } else {
            solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible);
        }
        previousSolutionInformation.add(solutionEfArrayInformation);
    }

    private List<int[]> mergeLocalArchiveIntoGlobal(ParetoArchive localArchive, ParetoArchive globalArchive, Solution optimalSolution) {
        List<Solution> localSols = localArchive.getParetoFrontSolutions();
        List<int[]> localVals = localArchive.getParetoFrontValues();
        List<int[]> solutionsToAddToEpsilon = new ArrayList<>();

        for (int i = 0; i < localSols.size(); i++) {
            Solution sLoc = localSols.get(i);

            if (sLoc == null) {
                int[] valsLoc = localVals.get(i);
                if (valsLoc[0] > mainObjectiveObtainedValue) {
                    mainObjectiveObtainedValue = valsLoc[0];
                    solutionsToAddToEpsilon.add(valsLoc);
                }
                continue;
            }

            if (sLoc == optimalSolution) continue;

            globalArchive.add(sLoc, /*checkDominanceWhenAdding=*/true);
        }
        return solutionsToAddToEpsilon;
    }

    private void findAllArchivePointsForEpsilon(int[] epsilonArr, ParetoArchive globalArchive, ParetoArchive localArchive,
                                                              StrategyParams params) {
        final int p = 0; // main objective in lexicographic optimization

        int bestP = Integer.MIN_VALUE;

        List<int[]> gf = globalArchive.getParetoFrontValues();
        ArrayList<Integer> idx = new ArrayList<>();

        for (int i = 0; i < gf.size(); i++) {
            int[] z = gf.get(i);
            if (inRegion(z, epsilonArr)) {
                idx.add(i);
                int zp = z[p];
                if (zp > bestP) {
                    bestP = zp;
                }
            }
        }

        for (int i : idx) {
            localArchive.getParetoFront().add(gf.get(i));
            localArchive.getParetoFrontSolutions().add(null);
        }

        // wire ParetoMaximizer to local lists (so addIntermediateSolutions() updates what propagates)
        ParetoMaximizer pareto = params.getParetoMaximizer();
        if (pareto != null) {
            pareto.setSharedFront(localArchive.getParetoFrontSolutions(), localArchive.getParetoFrontValues());
        }

        params.objectiveValueToDisableParetoMaximizer = bestP;
    }

    // region defined by lower bounds on objectives[1..] using epsilonArr[0..]
    private static boolean inRegion(int[] objVals, int[] epsilonArr) {
        // objVals length == objectives length
        // epsilonArr length == objectives.length - 1
        for (int i = 1; i < objVals.length; i++) {
            if (objVals[i] < epsilonArr[i - 1]) return false;
        }
        return true;
    }
}
