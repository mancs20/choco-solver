package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

public class ParetoFrontProcessor {
    private final Solution[] solutions;
    private final int[][] paretoFront;
    private final IntVar[] objectives;
    private final double[][] bounds;
    private final Object[] decisionVariables;
    private final boolean maximize;

    public ParetoFrontProcessor(Solution[] solutions, IntVar[] modelObjectives, ParetoObjective[] paretoObjectives, Object[] decisionVariables, boolean maximize) {
        this.solutions = solutions;
        this.objectives = modelObjectives;
        bounds = new double[objectives.length][objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            bounds[i][0] = paretoObjectives[i].getLB();
            bounds[i][1] = paretoObjectives[i].getUB();
        }
        this.decisionVariables = decisionVariables;
        paretoFront = getParetoFrontFromSolutions(this.solutions, this.objectives);
        this.maximize = maximize;
    }

    public static int[][] getParetoFrontFromSolutions(Solution[] solutions, IntVar[] objectives) {
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
        if (decisionVariables !=null && decisionVariables.length != 0) {
            for (int i = 0; i < solutions.length; i++) {
                int[] newParetoPointSolution = new int[decisionVariables.length];
                if (decisionVariables.getClass() == IntVar[].class || decisionVariables.getClass() == BoolVar[].class) {
                    for (int j = 0; j < decisionVariables.length; j++) {
                        newParetoPointSolution[j] = solutions[i].getIntVal((IntVar) decisionVariables[j]);
                    }
                }else{
                    System.out.println("Unsupported decision variable type, it should be either IntVar[] or BoolVar[].");
                    System.exit(1);
                }
                solutionsParetoFront[i] = newParetoPointSolution;
            }
        }
        return solutionsParetoFront;
    }

    public double[][] getBounds() {
        return bounds;
    }
}
