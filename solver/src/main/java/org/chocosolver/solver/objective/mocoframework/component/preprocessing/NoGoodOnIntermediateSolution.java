package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

public class NoGoodOnIntermediateSolution implements PreprocessingStrategy{
    @Override
    public void apply(Model model, IntVar[] objectives, ParetoArchive archive, StrategyParams params, Criterion... stop) {
        params.setNoGoodOnSolution(true);
        IntVar[] allVars;
        if (model.getHook("decisionVariables") != null) {
            IntVar[] decisionVars = (IntVar[]) model.getHook("decisionVariables");
            allVars = decisionVars;
//            allVars = new IntVar[decisionVars.length + objectives.length];
//            System.arraycopy(decisionVars, 0, allVars, 0, decisionVars.length);
//            System.arraycopy(objectives, 0, allVars, decisionVars.length, objectives.length);
        } else {
            allVars = model.retrieveIntVars(true);
        }
        model.getSolver().setNoGoodRecordingFromSolutionsForMOO(allVars);
    }
}
