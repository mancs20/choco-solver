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

        //solution information
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = params.getPreviousSolutionInfo();
        Set<String> previousSolutions = params.getPreviousSolutions();
        assert previousSolutions != null;

        Solution solution = solutionFinder.find(model, archive, objectives, region, params, stop);
        if (solution == null) {
            saveSolutionInformation(epsilonArr, null,  previousSolutionInformation, null);
        } else {
            int[] solutionObjectiveValues = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
            }
            String solutionString = Arrays.toString(solutionObjectiveValues);
            if (previousSolutions.contains(solutionString)) {
                archive.setCanAddSolution(false);
            } else {
                previousSolutions.add(solutionString);
                archive.setCanAddSolution(true);
            }
            saveSolutionInformation(epsilonArr, solutionObjectiveValues,  previousSolutionInformation, solution);
        }

        return solution;
    }

    private static void saveSolutionInformation(int[] efArrayActual, int[] solutionObjectiveValues, List<SolutionEpsilonArrayInformation> previousSolutionInformation, Solution solverSolution) {
        boolean feasible = solutionObjectiveValues != null;
        SolutionEpsilonArrayInformation solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible, solverSolution);
        previousSolutionInformation.add(solutionEfArrayInformation);
    }
}