package org.chocosolver.solver.objective.mocoframework.component.updateregions;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.component.findsolution.SolutionEpsilonArrayInformation;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SaugmeconUpdateModifyUB implements UpdateRegionsStrategy {
    private Constraint mainObjectiveUB;
    private int mainObjectiveLastValue;
    private boolean updateMainObjectiveUB = true;

    @Override
    public void update(Set<Region> regions, ParetoArchive archive, IntVar[] objectives, Solution solution, StrategyParams params) {
        if (regions.size() != 1) {
            throw new IllegalStateException("Expected a single region but found: " + regions.size());
        }
        int[] epsilonArr = params.getEpsilonArray();
        int[] ideal = params.getIdealPoint();
        if (epsilonArr == null || ideal == null) {
            throw new IllegalArgumentException("Missing epsilon or ideal from params.");
        }


        // todo move this to preprocessing create a new IntVar array where the objectives are ordered according to
        //  the params.getObjectivesOrder()
//        IntVar[] orderedObjectives = new IntVar[objectives.length];
//        int[] objOrder = params.getObjectivesOrder();
//        for (int i = 0; i < objOrder.length; i++) {
//            orderedObjectives[i] = objectives[objOrder[i]];
//        }

//        updateEpsilon(epsilonArr, solution, orderedObjectives, params);
//        if (ideal[ideal.length - 1] >= epsilonArr[epsilonArr.length - 1]) {
//            updateRegionConstraints(regions.iterator().next(), orderedObjectives, epsilonArr);
//        } else{
//            regions.clear();
//        }

        if (solution != null) {
            updateMainObjectiveUB = true;
            mainObjectiveLastValue = Integer.MAX_VALUE;
        } else {
            updateMainObjectiveUB = false;
        }
        if (mainObjectiveUB != null) {
            objectives[0].getModel().unpost(mainObjectiveUB);
            mainObjectiveUB = null;
        }
        while (!regions.isEmpty()) {
            updateEpsilon(epsilonArr, solution, objectives, params);
            if (ideal[ideal.length - 1] >= epsilonArr[epsilonArr.length - 1]) {
                SolutionEpsilonArrayInformation previousSolutionInfo = previousSolutionSatisfyCurrentEpsilon(epsilonArr, params);
                if (previousSolutionInfo == null) {
                    // no previous solution process satisfies the current epsilon values
                    updateRegionConstraints(regions.iterator().next(), objectives, epsilonArr);
                    if (updateMainObjectiveUB) {
                        mainObjectiveUB = objectives[0].getModel().arithm(objectives[0], "<=", mainObjectiveLastValue);
                        mainObjectiveUB.post();
                    }
                    break;
                } else if (previousSolutionInfo.isFeasible()) {
                    solution = previousSolutionInfo.getSolverSolution();
                } else {
                    solution = null;
                }
                // if the previous solution satisfy the current epsilon, we do not update the main objective UB
                updateMainObjectiveUB = false;
            } else {
                regions.clear();
            }
        }
    }

    private SolutionEpsilonArrayInformation previousSolutionSatisfyCurrentEpsilon(int[] epsilonArr, StrategyParams params) {
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = params.getPreviousSolutionInfo();
        Set<String> previousSolutions = params.getPreviousSolutions();
        assert previousSolutions != null;

        return searchPreviousSolutionsRelaxation(epsilonArr, previousSolutionInformation);
    }

    private SolutionEpsilonArrayInformation searchPreviousSolutionsRelaxation(int[] efArrayActual, List<SolutionEpsilonArrayInformation> previousSolutionInformation){
        SolutionEpsilonArrayInformation previousSolution;
        int idPreviousCloserRelaxation = getLessConstrainedPreviousSolutions(efArrayActual, previousSolutionInformation);
        if (idPreviousCloserRelaxation != -1) {
            previousSolution = previousSolutionInformation.get(idPreviousCloserRelaxation);
        }else{
            previousSolution = null;
        }
        return previousSolution;
    }

    private int getLessConstrainedPreviousSolutions(int[] efArrayActual, List<SolutionEpsilonArrayInformation> previousSolutionInformation){
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
                    if (!solutionSatisfyEfArr(fSolutionValues, efArrayActual)) {
                        solutionWithMoreRelaxationFound = false;
                        idx -= 1;
                        // next solution have a less or equal value for the main objective, if not it means it dominates the current solution, which is impossible as it is Pareto point
                        if (updateMainObjectiveUB && mainObjectiveLastValue > fSolutionValues[0]) {
                            mainObjectiveLastValue = fSolutionValues[0];
                        }
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
        for (int i = 0; i < efArray.length; i++) {
            if (solutionValues[i+1] < efArray[i]) {
                satisfy = false;
                break;
            }
        }
        return satisfy;
    }

    private void updateRegionConstraints(Region region, IntVar[] objectives, int[] epsilonArr){
        // todo unpost and post, use the same object do not create new constraints each time
        Model model = objectives[0].getModel();

        if (region.hasConstraints()) {
            List<Constraint> regionConstraints = region.getConstraints();
            for (int i = 1; i < objectives.length; i++) {
                regionConstraints.set(i - 1, model.arithm(objectives[i], ">=", epsilonArr[i - 1]));
            }
        } else {
            List<Constraint> constraints = new ArrayList<>();
            for (int i = 1; i < objectives.length; i++) {
                constraints.add(model.arithm(objectives[i], ">=", epsilonArr[i-1]));
            }
            region.setConstraints(constraints);
        }
    }

    private void updateEpsilon(int[] epsilonArr, Solution solution, IntVar[] objectives, StrategyParams params){
        int[] rwv = params.getRwv();
        int[] ideal = params.getIdealPoint();
        int[] nadir = params.getNadirPoint();
        if (rwv == null || nadir == null) {
            throw new IllegalArgumentException("Missing rwv or nadir from params.");
        }

        if (solution != null) {
            updateRelativeWorstValues(solution, objectives, rwv);
        } else {
            earlyExitAfterInfeasibility(epsilonArr, ideal, nadir);
        }
        updateEpsilonValues(epsilonArr, ideal, nadir, rwv);
    }

    private void updateRelativeWorstValues(Solution solution, IntVar[] objectives, int[] rwv) {
        rwv[0] = solution.getIntVal(objectives[1]);
        if (objectives.length > 2) {
            for (int i = 1; i < rwv.length; i++) {
                rwv[i] = Math.min(rwv[i], solution.getIntVal(objectives[i+1]));
            }
        }
    }

    private void earlyExitAfterInfeasibility(int[] epsilonArr, int[] ideal, int[] nadir){
        int j = epsilonArr.length - 1;
        for (int i = 0; i < epsilonArr.length - 1; i++) {
            if (epsilonArr[i] != nadir[i]) {
                j = i;
                break;
            }
        }
        System.arraycopy(ideal, 0, epsilonArr, 0, j + 1);
    }

    private void updateEpsilonValues(int[] epsilonArr, int[] ideal, int[] nadir, int[] rwv){
        for (int i = 0; i < epsilonArr.length; i++) {
            if (epsilonArr[i] < ideal[i] && rwv[i] < ideal[i]) {
                epsilonArr[i] = rwv[i] + 1;
                rwv[i] = ideal[i];
                break;
            } else if (i == epsilonArr.length - 1) {
                epsilonArr[i] = ideal[i] + 1;
            } else {
                epsilonArr[i] = nadir[i];
            }
        }
    }
}

