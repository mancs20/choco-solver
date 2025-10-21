package org.chocosolver.examples.integer;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.ParetoOptGavanelliConstraint;
import org.chocosolver.solver.variables.IntVar;

public class ParetoOptimizeSumGavanelliConstraint {

    public Object[] run(Model model, IntVar[] objectives, Boolean maximize, int timeoutSec){
         ParetoOptGavanelliConstraint pareto = model.getSolver().findParetoFrontOptimizing(objectives, maximize,
                timeoutSec);
        return new Object[]{pareto.getSolutions(), pareto.getRecorderList(), pareto.isExhaustive()};
    }
}
