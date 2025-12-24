package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.DisjunctiveAlgorithm;
import org.chocosolver.solver.variables.IntVar;

public class ParetoDisjunctiveNoLabel {

    public Object[] run(Model model, IntVar[] objectives, Boolean maximize, int timeoutSec){
        DisjunctiveAlgorithm disjunctive = model.getSolver().findParetoFrontDisjunctive(objectives, maximize, timeoutSec);
        return new Object[]{disjunctive.getSolutions(), disjunctive.getRecorderList(), disjunctive.getAllSolutions(),
                disjunctive.isExhaustive()};
    }
}
