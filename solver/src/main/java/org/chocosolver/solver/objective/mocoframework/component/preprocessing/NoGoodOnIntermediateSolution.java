package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.search.loop.monitors.NogoodFromDominanceFails;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

public class NoGoodOnIntermediateSolution implements PreprocessingStrategy{
    private final boolean learnFromNonObjectiveFails;

    public NoGoodOnIntermediateSolution(boolean learnFromNonObjectiveFails) {
        this.learnFromNonObjectiveFails = learnFromNonObjectiveFails;
    }

    @Override
    public void apply(Model model, IntVar[] objectives, ParetoArchive archive, StrategyParams params, Criterion... stop) {
        params.setNoGoodOnSolution(true);
        IntVar[] allVars;
        if (model.getHook("decisionVariables") != null) {
            allVars = (IntVar[]) model.getHook("decisionVariables");
//            allVars = new IntVar[decisionVars.length + objectives.length];
//            System.arraycopy(decisionVars, 0, allVars, 0, decisionVars.length);
//            System.arraycopy(objectives, 0, allVars, decisionVars.length, objectives.length);
        } else {
            allVars = model.retrieveIntVars(true);
        }
        model.getSolver().setNoGoodRecordingFromSolutionsForMOO(allVars);
        NogoodFromDominanceFails nogoodFromDominanceFails = model.getSolver().setNoGoodFromDominanceFails(objectives, learnFromNonObjectiveFails);
        params.setNogoodFromDominanceFails(nogoodFromDominanceFails);
    }
}
