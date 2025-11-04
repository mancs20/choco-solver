package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.Solution;

public interface FindNonDominatedSolutionStrategy {
    Solution find(Model model, IntVar[] objectives, Region region);
}