package org.chocosolver.examples.integer;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.SaugmeconNoRecursion;
import org.chocosolver.solver.variables.IntVar;


public class ParetoSaugmeconNoRecursive {

    public Object[] run(Model model, IntVar[] objectives, Boolean maximize, Boolean performLexicographicOptimization, int timeoutSec){
        return this.run(model, objectives, maximize, performLexicographicOptimization, timeoutSec, false);
    }

    public Object[] run(Model model, IntVar[] objectives, Boolean maximize, Boolean performLexicographicOptimization,
                        int timeoutSec, boolean useReals){
        SaugmeconNoRecursion saugmecon = model.getSolver().findParetoFrontSaugmecon(objectives, maximize,
                performLexicographicOptimization, timeoutSec, useReals);
        return new Object[]{saugmecon.getSolutions(), saugmecon.getRecorderList(), saugmecon.getAllSolutions(), saugmecon.isExhaustive()};
    }
}
