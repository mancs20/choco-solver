package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoArchive {
    private final List<int[]> paretoFront;
    private final List<Solution> paretoSolutions;

    private final IntVar[] objectives;

    public ParetoArchive(IntVar[] objectives) {
        this.objectives = objectives.clone();
        this.paretoFront = new ArrayList<>();
        this.paretoSolutions = new ArrayList<>();
    }

    public List<Solution> getParetoFrontSolutions() {
        return paretoSolutions;
    }
    public List<int[]> getParetoFrontValues() {
        return paretoFront;
    }

    /**
     * Add a new solution to the archive. It removes all solutions that are dominated
     * by the new one.
     *
     * @param solution the candidate solution vector (int[] of objective values)
     */
    public void add(Solution solution) {
        int isDominated = -1;
        if (solution != null) {
            // get objective values
            int[] vals = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                vals[i] = objectives[i].getValue();
            }
            for (int i = paretoSolutions.size() - 1; i >= 0; i--) {
                isDominated = isDominated(paretoFront.get(i), vals);
                if (isDominated > 0) {
                    paretoSolutions.remove(i);
                    paretoFront.remove(i);
                } else if (isDominated == 0) {
                    break;
                }
            }
            if (isDominated != 0) {
                paretoSolutions.add(solution);
                paretoFront.add(vals);
            }
        }
    }

    private int isDominated(int[] archiveSolution, int[] vals) {
        int delta = 1;
        for (int i = 0; i < objectives.length; i++) {
            int deltaTmp = vals[i] - archiveSolution[i];
            if (deltaTmp < 0) {
                return -1;
            } else{
                delta += deltaTmp;
            }
        }
        return delta;
    }

    public void clear() {
        paretoSolutions.clear();
    }

    public boolean isEmpty() {
        return paretoSolutions.isEmpty();
    }

    public int size() {
        return paretoSolutions.size();
    }
}
