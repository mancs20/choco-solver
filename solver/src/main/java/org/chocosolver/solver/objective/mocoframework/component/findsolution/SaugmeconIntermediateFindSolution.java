package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SaugmeconIntermediateFindSolution extends AbstractFindSolutionStrategy{

    public SaugmeconIntermediateFindSolution(SolutionFinder solutionFinder) {
        super(solutionFinder);
    }

    @Override
    public Solution find(Model model, ParetoArchive archive, IntVar[] objectives, Region region, StrategyParams params, Criterion... stop) {
        // params
        int[] epsilonArr = params.getEpsilonArray();

        //todo for debugging delete after
        System.out.print("epsilon: " + Arrays.toString(epsilonArr) + ". ");
        //todo for debug

        //solution information
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = params.getPreviousSolutionInfo();
        Set<String> previousSolutions = params.getPreviousSolutions();
        assert previousSolutions != null;

        ParetoArchive localArchive = new ParetoArchive(objectives);

        // todo boolean to try different versions
        boolean allSolutionsInRegion = true;
        ArrayList<Integer> solutionArchiveIndices;
        Constraint lbMainObjectiveConstraint = null;

        if (allSolutionsInRegion) {
            solutionArchiveIndices = findAllArchivePointsForEpsilon(epsilonArr, archive, localArchive, params);
        } else {
            solutionArchiveIndices = findArchivePointsForEpsilon(epsilonArr, archive, localArchive, params);
            if (solutionArchiveIndices.size() > 0) {
                int bestValueMainObjective = archive.getParetoFront().get(solutionArchiveIndices.get(0))[0];
                lbMainObjectiveConstraint = model.arithm(objectives[0], ">=", bestValueMainObjective);
                lbMainObjectiveConstraint.post();
            }
        }

        Solution solution = solutionFinder.find(model, localArchive, objectives, region, params, stop);
        if (solution == null && (allSolutionsInRegion || solutionArchiveIndices.size() == 0)) {
            // no solution in this region
            // todo for debugginng delete after
            System.out.println("Infeasibility" );
            // todo for debugginng delete after
            saveSolutionInformation(epsilonArr, null,  previousSolutionInformation, null);
        } else {
            // there are solutions in this region, check if they are new and update the archive
            if (solution == null) {
                // no new solution in the region, take the lexicographically best solution from the archive for this epsilon
                archive.setCanAddSolution(false);
                solution = getSolutionFromArchiveForEpsilon(solutionArchiveIndices, archive, objectives);
            } else {
                archive.setCanAddSolution(true);
                int[] solutionObjectiveValues = new int[objectives.length];
                for (int i = 0; i < objectives.length; i++) {
                    solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
                }
                String solutionString = Arrays.toString(solutionObjectiveValues);

                // todo for debugginng delete after
                System.out.println("Found solution: " + solutionString);
                // todo for debugginng delete after

                previousSolutions.add(solutionString);
//                if (params.getParetoMaximizer() == null) { //todo I think this is wrong, I should always save the info of Pareto-optimal solutions
                saveSolutionInformation(epsilonArr, solutionObjectiveValues,  previousSolutionInformation, solution);
//                }
                mergeLocalArchiveIntoGlobal(localArchive, archive, solution);
            }
        }

        if (lbMainObjectiveConstraint != null) {
            model.unpost(lbMainObjectiveConstraint);
        }

        return solution;
    }

    private static void saveSolutionInformation(int[] efArrayActual, int[] solutionObjectiveValues, List<SolutionEpsilonArrayInformation> previousSolutionInformation, Solution solverSolution) {
        boolean feasible = solutionObjectiveValues != null;
        SolutionEpsilonArrayInformation solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible, solverSolution);
        previousSolutionInformation.add(solutionEfArrayInformation);
    }

    private ArrayList<Integer> findArchivePointsForEpsilon(int[] epsilonArr, ParetoArchive globalArchive, ParetoArchive localArchive,
                                             StrategyParams params) {
        final int p = 0; // main objective in lexicographic optimization

        int bestP = Integer.MIN_VALUE;
        List<int[]> gf = globalArchive.getParetoFrontValues();

        int[] idx = new int[gf.size()];
        int sz = 0;

        for (int i = 0; i < gf.size(); i++) {
            int[] z = gf.get(i);
            if (!inRegion(z, epsilonArr)) continue;

            int zp = z[p];
            if (zp > bestP) {
                bestP = zp;
                sz = 0;          // clear
                idx[sz++] = i;   // add
            } else if (zp == bestP) {
                idx[sz++] = i;
            }
        }

        for (int k = 0; k < sz; k++) {
            localArchive.getParetoFront().add(gf.get(idx[k]));
            localArchive.getParetoFrontSolutions().add(null);
        }

        // 3) wire ParetoMaximizer to local lists (so addIntermediateSolutions() updates what propagates)
        ParetoMaximizer pareto = params.getParetoMaximizer();
        if (pareto != null) {
            pareto.setSharedFront(localArchive.getParetoFrontSolutions(), localArchive.getParetoFrontValues());
        }

        // unify return types between different versions
        ArrayList<Integer> idxList = new ArrayList<>();
        for (int i = 0; i < sz; i++) {
            if (idx[i] == 0) break;
            idxList.add(idx[i]);
        }

        return idxList;
    }

    private ArrayList<Integer> findAllArchivePointsForEpsilon(int[] epsilonArr, ParetoArchive globalArchive, ParetoArchive localArchive,
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

        return idx;
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

    private static void mergeLocalArchiveIntoGlobal(ParetoArchive localArchive, ParetoArchive globalArchive, Solution optimalSolution) {
        List<Solution> localSols = localArchive.getParetoFrontSolutions();
        List<int[]> localVals = localArchive.getParetoFrontValues();

        for (int i = 0; i < localSols.size(); i++) {
            Solution sLoc = localSols.get(i);

            if (sLoc == null || sLoc == optimalSolution) continue;

            int[] vLoc = localVals.get(i);

            // Option A: do NOT add the final solution here (outer loop will add it)
//            if (lastVals != null && Arrays.equals(vLoc, lastVals)) continue;

            // merge intermediate survivor into the GLOBAL archive
            // (this assumes your SAUGMECON logic guarantees these are not dominated by existing global points)
            globalArchive.add(sLoc, /*checkDominanceWhenAdding=*/true);
        }
    }

    private Solution getSolutionFromArchiveForEpsilon(ArrayList<Integer> solutionArchiveIndices, ParetoArchive archive, IntVar[] objectives) {
        if (solutionArchiveIndices.size() == 0) {
            return null;
        }
        int idInnerObjective = 1;
        int bestValueInnerObjective = archive.getParetoFrontValues().get(solutionArchiveIndices.get(0))[idInnerObjective];
        int idxBest = solutionArchiveIndices.get(0);
        for (int i = 1; i < solutionArchiveIndices.size(); i++) {

            int candidateValue = archive.getParetoFrontValues().get(solutionArchiveIndices.get(i))[idInnerObjective];
//            if (isLexicographicallyBigger(candidateValues, bestValues)) {
            if(bestValueInnerObjective < candidateValue) {
                bestValueInnerObjective = candidateValue;
                idxBest = solutionArchiveIndices.get(i);
            }
        }

        // todo for debugginng delete after
        System.out.println("Solution already found: " + Arrays.toString(archive.getParetoFront().get(idxBest)));
        // todo for debugginng delete after

        return archive.getParetoFrontSolutions().get(idxBest);
    }

    private static boolean isLexicographicallyBigger(int[] candidateValues, int[] baseValues) {
        for (int i = 0; i < candidateValues.length; i++) {
            if (candidateValues[i] > baseValues[i]) {
                return true;
            } else if (candidateValues[i] < baseValues[i]) {
                return false;
            }
        }
        return false; // they are equal
    }
}
