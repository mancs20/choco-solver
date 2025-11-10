package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Solution;

public class SolutionEpsilonArrayInformation {
    private final int[] solution;
    private final int[] epsilonArr;
    private final boolean feasible;
    private final Solution solverSolution;

    public SolutionEpsilonArrayInformation(int[] solution, int[] epsilonArr, boolean feasible, Solution solverSolution) {
        this.solution = solution;
        this.epsilonArr = epsilonArr;
        this.feasible = feasible;
        this.solverSolution = solverSolution;
    }

    public int[] getSolution() {
        return solution;
    }

    public Solution getSolverSolution() {
        return solverSolution;
    }

    public int[] getEfArray() {
        return epsilonArr;
    }

    public boolean isFeasible() {
        return feasible;
    }
}
