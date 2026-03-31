package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.HashSet;
import java.util.Set;

public class DisjunctivePreprocessing extends BasePreprocessing {
    private int[] idealValues;

    public DisjunctivePreprocessing(SolutionFinder optimizer) {
        super(optimizer);
    }

    @Override
    public void apply(Model model, IntVar[] objectives, ParetoArchive archive, StrategyParams params, Criterion... stop) {
        // custom objectives order
        Set<Integer> excludedObjectives = new HashSet<>();
        idealValues = getIdealValues(objectives, archive, excludedObjectives, params, stop);
        params.setIdealPoint(idealValues);
        int[] nadirValues = getNadirValues(objectives, excludedObjectives, stop);
        params.setNadirPoint(nadirValues);
        params.setCheckIfNewSolutionDominates(false);
        model.clearObjective();
        // constraint objectives
        for (int i = 0; i < idealValues.length; i++) {
            model.arithm(objectives[i], "<=", idealValues[i]).post();
        }
        prepareObjectiveFunction(objectives, model);
        params.setUseOptimization(true);
    }

    private void prepareObjectiveFunction(IntVar[] objectives, Model model) {
        int LBsum = 0;
        int UBsum = 0;
        for (int i = 0; i < objectives.length; i++) {
            LBsum += objectives[i].getLB();
            UBsum += idealValues[i];
        }
        IntVar objectiveSum = model.intVar("objectiveSum", LBsum, UBsum);
        model.sum(objectives, "=", objectiveSum).post();
        model.setObjective(true, objectiveSum);
    }
}
