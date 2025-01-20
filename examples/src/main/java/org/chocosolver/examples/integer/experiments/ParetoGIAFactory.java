package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.ParetoGIASparsityCriteriaSelection;
import org.chocosolver.examples.integer.ParetoGIA;
import org.chocosolver.examples.integer.ParetoGIANoneCriteriaSelection;
import org.chocosolver.solver.objective.GiaConfig;

public class ParetoGIAFactory {
    public static ParetoGIA createSolver(GiaConfig config, int timeout) {
        switch (config.getCriteriaSelection()) {
            case NONE:
                return new ParetoGIANoneCriteriaSelection(config, timeout);
            case SPARSITY:
                return new ParetoGIASparsityCriteriaSelection(config, timeout);
            default:
                throw new IllegalArgumentException("Unsupported Criteria Selection");
        }
    }
}
