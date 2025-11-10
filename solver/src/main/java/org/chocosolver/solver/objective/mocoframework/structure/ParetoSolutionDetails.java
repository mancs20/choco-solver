package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.Solution;

import java.util.List;

public class ParetoSolutionDetails {
    private final List<Solution> paretoFront;
    private final List<String> solverMeasures;
    private final boolean exhaustive;

    public ParetoSolutionDetails(List<Solution> paretoFront, List<String> solverMeasures, boolean exhaustive) {
        this.paretoFront = paretoFront;
        this.solverMeasures = solverMeasures;
        this.exhaustive = exhaustive;
    }

    public List<Solution> getParetoFront() {
        return paretoFront;
    }

    public List<String> getSolverMeasures() {
        return solverMeasures;
    }

    public boolean isExhaustive() {
        return exhaustive;
    }
}
