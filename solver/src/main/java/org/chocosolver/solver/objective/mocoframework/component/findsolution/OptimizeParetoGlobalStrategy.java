package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

public class OptimizeParetoGlobalStrategy implements FindNonDominatedSolutionStrategy{
    @Override
    public Solution find(Model model, IntVar[] objectives, Region region) {
        return null;
    }
}
