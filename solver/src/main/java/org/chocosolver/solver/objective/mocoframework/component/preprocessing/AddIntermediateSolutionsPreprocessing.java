package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;


public class AddIntermediateSolutionsPreprocessing implements PreprocessingStrategy{
    @Override
    public void apply(Model model, IntVar[] objectives, ParetoArchive archive, StrategyParams params, Criterion... stop) {
        params.setAddIntermediateSolutions(true);
    }
}
