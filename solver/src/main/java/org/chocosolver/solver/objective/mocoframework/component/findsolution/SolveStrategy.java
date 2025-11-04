package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.Set;

public class SolveStrategy implements FindNonDominatedSolutionStrategy{
    @Override
    public Solution find(Model model, IntVar[] objectives, Region region) {
        // add the constraints corresponding to the region
        if (!region.isDummy()) {
            Set<Constraint> regionConstraints =  region.getConstraints();
            for (Constraint c : regionConstraints) {
                c.post();
            }
        }

        if (model.getSolver().solve()) {
            Solution sol = new Solution(model);
            sol.record();
            return sol;
        } else {
            return null;
        }
    }
}
