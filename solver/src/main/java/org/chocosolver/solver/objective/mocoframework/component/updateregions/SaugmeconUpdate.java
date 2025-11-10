package org.chocosolver.solver.objective.mocoframework.component.updateregions;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;

public class SaugmeconUpdate implements UpdateRegionsStrategy{
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
        updateEpsilon(epsilonArr, solution, objectives, params);
        if (ideal[ideal.length - 1] >= epsilonArr[epsilonArr.length - 1]) {
            updateRegionConstraints(regions.iterator().next(), objectives, epsilonArr);
        } else{
            regions.clear();
        }
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
        // params
        int[] rwv = params.getRwv();
        int[] ideal = params.getIdealPoint();
        int[] nadir = params.getNadirPoint();
        if (rwv == null || nadir == null) {
            throw new IllegalArgumentException("Missing rwv or nadir from params.");
        }

        if (solution != null) {
            int[] solutionObjectiveValues = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
            }
            updateRelativeWorstValues(solutionObjectiveValues, rwv);
        } else {
            earlyExitAfterInfeasibility(epsilonArr, ideal, nadir);
        }
        updateEpsilonValues(epsilonArr, ideal, nadir, rwv);
    }

    private void updateRelativeWorstValues(int[] solutionObjectiveValues, int[] rwv) {
        rwv[0] = solutionObjectiveValues[1];
        if (solutionObjectiveValues.length > 2) {
            for (int i = 1; i < rwv.length; i++) {
                rwv[i] = Math.min(rwv[i], solutionObjectiveValues[i+1]);
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
