package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

public class ParetoFrontProcessor {
    private final Solution[] solutions;
    private final int[][] paretoFront;
    private final IntVar[] objectives;
    private final double[][] bounds;
    private final IntVar[] decisionVariables;
    private final boolean maximize;

    public ParetoFrontProcessor(Solution[] solutions, IntVar[] modelObjectives, ParetoObjective[] paretoObjectives, IntVar[] decisionVariables, boolean maximize) {
        this.solutions = solutions;
        this.objectives = modelObjectives;
        bounds = new double[objectives.length][objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            bounds[i][0] = paretoObjectives[i].getLB();
            bounds[i][1] = paretoObjectives[i].getUB();
        }
        this.decisionVariables = decisionVariables;
        paretoFront = getParetoFrontFromSolutions();
        this.maximize = maximize;
    }

    private int[][] getParetoFrontFromSolutions() {
        int[][] paretoFront = new int[solutions.length][];
        for (int i = 0; i < solutions.length; i++) {
            int[] newParetoPoint = new int[objectives.length];
            for (int j = 0; j < objectives.length; j++) {
                newParetoPoint[j] = solutions[i].getIntVal(objectives[j]);
            }
            paretoFront[i] = newParetoPoint;
        }
        return paretoFront;
    }

    public double getHypervolume() {
        return HypervolumeCalculator.calculateHypervolume(paretoFront, maximize, bounds);
    }

    public double getNormalizedHypervolume() {
        return HypervolumeCalculator.calculateNormalizedHypervolume(paretoFront, maximize, bounds);
    }

    public int[][] getParetoFront() {
        return paretoFront;
    }

    public int[][] getSolutionsParetoFront() {
        int[][] solutionsParetoFront = new int[solutions.length][];
        for (int i = 0; i < solutions.length; i++) {
            int[] newParetoPointSolution = new int[decisionVariables.length];
            for (int j = 0; j < decisionVariables.length; j++) {
                newParetoPointSolution[j] = solutions[i].getIntVal(decisionVariables[j]);
            }
            solutionsParetoFront[i] = newParetoPointSolution;
        }
        return solutionsParetoFront;
    }

    public double[][] getBounds() {
        return bounds;
    }
}
