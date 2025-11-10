package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.*;

public class SaugmeconFindSolution extends AbstractFindSolutionStrategy{

    public SaugmeconFindSolution(SolutionFinder solutionFinder) {
        super(solutionFinder);
    }

    @Override
    public Solution find(Model model, ParetoArchive archive, IntVar[] objectives, Region region, StrategyParams params, Criterion... stop) {
        // params
        int[] epsilonArr = params.getEpsilonArray();

        // solution information
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = params.getPreviousSolutionInfo();
        Set<String> previousSolutions = params.getPreviousSolutions();
        assert previousSolutions != null;

        int[] solutionObjectiveValues;
        Solution solution = null;
        SolutionEpsilonArrayInformation previousSolutionSatisfyCurrentConstraint = searchPreviousSolutionsRelaxation(epsilonArr, previousSolutionInformation);
        if (previousSolutionSatisfyCurrentConstraint != null) {
            // uncomment for debugging
//            System.out.print(" is satisfied by a previous solution: ");
            if (previousSolutionSatisfyCurrentConstraint.isFeasible()) {
                solution = previousSolutionSatisfyCurrentConstraint.getSolverSolution();
                int[] solutionValues = new int[objectives.length];
                for (int i = 0; i < objectives.length; i++) {
                    solutionValues[i] = solution.getIntVal(objectives[i]);
                }
                archive.setLastAddedSolutionObjValues(solutionValues);
                // uncomment for debugging
//                System.out.println(Arrays.toString(solutionObjectiveValues) + " efArrayPrevious: " +
//                        Arrays.toString(previousSolutionSatisfyCurrentConstraint.getEfArray()));
            }
        } else {
            solution = solutionFinder.find(model, archive, objectives, region, params, stop);
            if (solution == null) {
                // save solution information
                saveSolutionInformation(epsilonArr, null,  previousSolutionInformation, null);
                // uncomment for debugging
//                System.out.println(" after solved is infeasible");
            } else {
                solutionObjectiveValues = new int[objectives.length];
                for (int i = 0; i < objectives.length; i++) {
                    solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
                }
                // uncomment for debugging
//                System.out.println(" after solved is feasible: " + Arrays.toString(solutionObjectiveValues));
                String solutionString = Arrays.toString(solutionObjectiveValues);
                // uncomment for debugging
//                if (previousSolutions.contains(solutionString)){
//                    System.out.println("Above solution already in the front");
//                }
                if (!previousSolutions.contains(solutionString)) {
                    previousSolutions.add(solutionString);
                    archive.add(solution, false);
                } else {
                    archive.setLastAddedSolutionObjValues(solutionObjectiveValues);
                }

                saveSolutionInformation(epsilonArr, solutionObjectiveValues,  previousSolutionInformation, solution);
            }

        }
        return solution;
    }

    public static SolutionEpsilonArrayInformation searchPreviousSolutionsRelaxation(int[] efArrayActual, List<SolutionEpsilonArrayInformation> previousSolutionInformation){
        SolutionEpsilonArrayInformation previousSolution;
        int idPreviousCloserRelaxation = getLessConstrainedPreviousSolutions(efArrayActual, previousSolutionInformation);
        if (idPreviousCloserRelaxation != -1) {
            previousSolution = previousSolutionInformation.get(idPreviousCloserRelaxation);
        }else{
            previousSolution = null;
        }
        return previousSolution;
    }

    private static int getLessConstrainedPreviousSolutions(int[] efArrayActual, List<SolutionEpsilonArrayInformation> previousSolutionInformation){
        if (previousSolutionInformation.isEmpty()) {
            return -1;
        }
        int idx = previousSolutionInformation.size() - 1;
        boolean solutionWithMoreRelaxationFound = false;
        while (!solutionWithMoreRelaxationFound && idx > -1) {
            if (efArray1LessConstraintEfArray2(previousSolutionInformation.get(idx).getEfArray(), efArrayActual)) {
                int[] fSolutionValues = previousSolutionInformation.get(idx).getSolution();
                solutionWithMoreRelaxationFound = true;
                if (previousSolutionInformation.get(idx).isFeasible()) {
                    int[] fSolutionValuesForConstraint = Arrays.copyOfRange(fSolutionValues, 1, fSolutionValues.length);
                    if (!solutionSatisfyEfArr(fSolutionValuesForConstraint, efArrayActual)) {
                        solutionWithMoreRelaxationFound = false;
                        idx -= 1;
                    }
                }
            } else {
                idx -= 1;
            }
        }
        return idx;
    }

    private static boolean efArray1LessConstraintEfArray2(int[] efArray1, int[] efArray2) {
        boolean lessConstrained = true;
        for (int i = 0; i < efArray1.length; i++) {
            if (efArray1[i] > efArray2[i]) {
                lessConstrained = false;
                break;
            }
        }
        return lessConstrained;
    }

    private static boolean solutionSatisfyEfArr(int[] solutionValues, int[] efArray) {
        boolean satisfy = true;
        for (int i = 0; i < solutionValues.length; i++) {
            if (solutionValues[i] < efArray[i]) {
                satisfy = false;
                break;
            }
        }
        return satisfy;
    }

    private static void saveSolutionInformation(int[] efArrayActual, int[] solutionObjectiveValues, List<SolutionEpsilonArrayInformation> previousSolutionInformation, Solution solverSolution) {
        boolean feasible = solutionObjectiveValues != null;
        SolutionEpsilonArrayInformation solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible, solverSolution);
        previousSolutionInformation.add(solutionEfArrayInformation);
    }
}