package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import java.util.Collections;
import java.util.List;

public class SolutionEpsilonArrayInformation {
    private final List<int[]> solutions;
    private final int[] epsilonArr;
    private final boolean feasible;

    public SolutionEpsilonArrayInformation(int[] solution, int[] epsilonArr, boolean feasible) {
        this.solutions = Collections.singletonList(solution);
        this.epsilonArr = epsilonArr;
        this.feasible = feasible;
    }

    public SolutionEpsilonArrayInformation(List<int[]> solutions, int[] epsilonArr, boolean feasible) {
        this.solutions = solutions;
        this.epsilonArr = epsilonArr;
        this.feasible = feasible;
    }

    public int[] getSolution() {
        return solutions.get(0);
    }

    public List<int[]> getSolutions() {
        return solutions;
    }

    public int[] getEfArray() {
        return epsilonArr;
    }

    public boolean isFeasible() {
        return feasible;
    }
}
