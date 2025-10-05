package org.chocosolver.examples.integer;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoSaugmeconNoRecursive {

    public Object[] run(Model model, IntVar[] objectives, Boolean maximize, Boolean performLexicographicOptimization, int timeoutSec){
        List<Solution> solutions = model.getSolver().findParetoFrontSaugmecon(objectives, maximize,
                performLexicographicOptimization, timeoutSec);
        // stats
        List<String> recorderList = new ArrayList<>();
        recorderList.add(model.getSolver().getMeasures().toString());

        return new Object[]{solutions, recorderList};
    }
}
