package org.chocosolver.examples;

import org.chocosolver.examples.integer.ParetoGIA;
import org.chocosolver.solver.objective.GiaConfig;
import org.chocosolver.solver.objective.ParetoMaximizerGIACoverage;
import org.chocosolver.solver.objective.ParetoMaximizerGIAGeneral;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoGIASparsityCriteriaSelection extends ParetoGIA {
    protected List<int[]> objValsToConstraintSearchSpace = new ArrayList<>();

    public ParetoGIASparsityCriteriaSelection(GiaConfig config, int timeout) {
        super(config, timeout);
        numObjectivesAllowed = 2;
    }

    @Override
    protected ParetoMaximizerGIAGeneral setGIAPropagator(IntVar[] objectives, boolean portfolio) {
        return new ParetoMaximizerGIACoverage(objectives, portfolio, choosePropagatorPriority(objectives.length));
    }

    protected void boundSolutionSpaceRegionDominatesPoint() {
        // TODO add constraints considering objValsToConstraintSearchSpace
    }
}
