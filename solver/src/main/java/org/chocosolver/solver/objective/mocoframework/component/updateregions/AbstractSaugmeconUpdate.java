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

public abstract class AbstractSaugmeconUpdate implements UpdateRegionsStrategy {

    @Override
    public final void update(Set<Region> regions, ParetoArchive archive,
                             IntVar[] objectives, Solution solution, StrategyParams params) {

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
        int[] solutionObjValues = null;
        if (solution != null) {
            solutionObjValues = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                solutionObjValues[i] = solution.getIntVal(objectives[i]);
            }
        }

        onStartUpdate(objectives, solution);

        while (!regions.isEmpty()) {
            updateEpsilon(epsilonArr, solutionObjValues, objectives, params);
            if (ideal[ideal.length - 1] >= epsilonArr[epsilonArr.length - 1]) {
                int[] idsPreviousSolutionInfo = previousSolutionSatisfyCurrentEpsilon(epsilonArr, params);
                if (idsPreviousSolutionInfo[0] == -1) {
                    // no previous solution process satisfies the current epsilon values
                    updateRegionConstraints(regions.iterator().next(), objectives, epsilonArr);
                    onAfterUpdateRegionConstraints(objectives[0]);
                    break;
                } else if (idsPreviousSolutionInfo[1] != -1) {
                    solutionObjValues = params.getPreviousSolutionInfo().get(idsPreviousSolutionInfo[0]).getSolutions().get(idsPreviousSolutionInfo[1]);
                } else {
                    solutionObjValues = null;
                }
                onEndProcessingSolution();
            } else {
                regions.clear();
            }
        }
    }

    // Hooks (default no-op)
    protected void onStartUpdate(IntVar[] objectives, Solution solution) {}
    protected void onPreviousSolutionNotDominating(int[] fSolutionValues) {}
    protected void onAfterUpdateRegionConstraints(IntVar objective) {}
    protected void onEndProcessingSolution() {}

    protected int[] previousSolutionSatisfyCurrentEpsilon(int[] epsilonArr, StrategyParams params) {
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = params.getPreviousSolutionInfo();

        if (previousSolutionInformation.isEmpty()) {
            return new int[]{-1, -1};
        }
        int idx = previousSolutionInformation.size() - 1;
        boolean solutionWithMoreRelaxationFound = false;
        int chosenSolIdx = -1;
        while (!solutionWithMoreRelaxationFound && idx > -1) {
            if (efArray1LessConstraintEfArray2(previousSolutionInformation.get(idx).getEfArray(), epsilonArr)) {
                SolutionEpsilonArrayInformation info = previousSolutionInformation.get(idx);
                solutionWithMoreRelaxationFound = true;
                if (info.isFeasible()) {
                    List<int[]> sols = info.getSolutions();
                    for (int s = 0; s < sols.size(); s++) {
                        int[] fSolutionValues = sols.get(s);
                        if (solutionSatisfyEfArr(fSolutionValues, epsilonArr)) {
                            chosenSolIdx = s;
                            break;
                        }
                    }
                    if (chosenSolIdx == -1) {
                        solutionWithMoreRelaxationFound = false;
                        idx -= 1;
                        onPreviousSolutionNotDominating(info.getSolutions().get(0));
                    }
                }
            } else {
                idx -= 1;
            }
        }
        if (idx == -1) {
            return new int[]{-1, -1};
        }
        return new int[]{idx, chosenSolIdx};
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
    private void updateEpsilon(int[] epsilonArr, int[] solutionObjValues, IntVar[] objectives, StrategyParams params){
        int[] rwv = params.getRwv();
        int[] ideal = params.getIdealPoint();
        int[] nadir = params.getNadirPoint();
        if (rwv == null || nadir == null) {
            throw new IllegalArgumentException("Missing rwv or nadir from params.");
        }

        if (solutionObjValues != null) {
            updateRelativeWorstValues(solutionObjValues, objectives, rwv);
        } else {
            earlyExitAfterInfeasibility(epsilonArr, ideal, nadir);
        }
        updateEpsilonValues(epsilonArr, ideal, nadir, rwv);
    }

    private void updateRelativeWorstValues(int[] solutionObjValues, IntVar[] objectives, int[] rwv) {
        rwv[0] = solutionObjValues[1];
        if (objectives.length > 2) {
            for (int i = 1; i < rwv.length; i++) {
//                rwv[i] = Math.min(rwv[i], solution.getIntVal(objectives[i+1]));
                rwv[i] = Math.min(rwv[i], solutionObjValues[i+1]);
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
