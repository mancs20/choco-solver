package org.chocosolver.solver.objective.mocoframework.component.objectivefunction;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.variables.IntVar;


public class SumObjectiveFunctionStrategy implements ObjectiveFunctionStrategy{
    @Override
    public IntVar define(Model model, IntVar[] objectives, StrategyParams params) {
        int ub = 0;
        int lb = 0;
        for (IntVar obj: objectives) {
            ub += obj.getUB();
            lb += obj.getLB();
        }
        IntVar sum = model.intVar("sum_objectives", lb, ub);
        Constraint objectiveFunction = model.sum(objectives, "=", sum);
        params.setObjectiveFunction(objectiveFunction);
        return sum;
    }
}
