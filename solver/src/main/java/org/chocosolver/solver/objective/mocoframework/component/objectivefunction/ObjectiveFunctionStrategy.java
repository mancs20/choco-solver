package org.chocosolver.solver.objective.mocoframework.component.objectivefunction;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.variables.IntVar;

public interface ObjectiveFunctionStrategy {
    IntVar define(Model model, IntVar[] objectives, StrategyParams params);

    boolean isNone();
}
