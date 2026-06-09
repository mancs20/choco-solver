package org.chocosolver.solver.objective.mocoframework.component.updateregions;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.Set;

public class LocalPareto implements UpdateRegionsStrategy {

    private IntVar[] objectives;
    private Model model;

    @Override
    public void update(Set<Region> regions, ParetoArchive archive, IntVar[] objectives, Solution solution, StrategyParams params) {

        if (this.objectives == null) {
            this.objectives = objectives;
            model = objectives[0].getModel();
        }

        if (solution == null) {
            regions.clear();
        } else {
            int[] objectivesValues = solutionToObjectivesValues(solution);
            // post constraints
            Constraint[] constraints = new Constraint[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                constraints[i] = model.arithm(objectives[i], ">", objectivesValues[i]);
            }
            model.or(constraints).post();
        }
    }

    private int[] solutionToObjectivesValues(Solution solution) {
        int[] objectivesValues = new int[objectives.length];
        if (solution == null) {
            return objectivesValues;
        }
        for (int i = 0; i < objectives.length; i++) {
            objectivesValues[i] = solution.getIntVal(objectives[i]);
        }
        return objectivesValues;
    }
}
