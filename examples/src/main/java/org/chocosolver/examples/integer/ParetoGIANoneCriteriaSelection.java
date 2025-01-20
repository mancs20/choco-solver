package org.chocosolver.examples.integer;

import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.objective.GiaConfig;
import org.chocosolver.solver.objective.ParetoMaximizerGIAClassic;
import org.chocosolver.solver.objective.ParetoMaximizerGIAGeneral;
import org.chocosolver.solver.variables.IntVar;

public class ParetoGIANoneCriteriaSelection extends ParetoGIA {
    public ParetoGIANoneCriteriaSelection(GiaConfig config, int timeout) {
        super(config, timeout);
    }

    @Override
    protected ParetoMaximizerGIAGeneral setGIAPropagator(IntVar[] objectives, boolean portfolio) {
        return new ParetoMaximizerGIAClassic(objectives, portfolio, choosePropagatorPriority(objectives.length));
    }
}
