package org.chocosolver.util.moexperiments;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class TimeBasedSolutionPrinter {
    private final long startMillis;          // Experiment start time
    private int pendingSecond = -1;          // The second we're currently tracking
    private List<int[]> pendingFront = null; // The last front seen in that second
    private final List<int[]> paretoObjectiveValues = new ArrayList<>();
    private boolean firstSolution = true;

    // Constructor: sets experiment start time
    public TimeBasedSolutionPrinter() {
        this.startMillis = System.currentTimeMillis();
    }

    /**
     * Called whenever a new Pareto front is found (as a list of int[])
     */
    public void onNewSolution(List<int[]> currentParetoFront) {
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        int currentCeilSecond = (int) Math.ceil(elapsedMillis / 1000.0);

        List<int[]> copiedFront = deepCopyFront(currentParetoFront);

        if (pendingSecond == -1) {
            // First solution ever
            pendingSecond = currentCeilSecond;
            pendingFront = copiedFront;
        } else if (currentCeilSecond == pendingSecond) {
            // Same second — update to latest front
            pendingFront = copiedFront;
        } else {
            // New second — print previous and store new one
            print(pendingSecond, pendingFront);
            pendingSecond = currentCeilSecond;
            pendingFront = copiedFront;
        }
    }

    public void onNewSolution(Solution solution, IntVar[] objectives) {
        int[] solutionObjectives = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            solutionObjectives[i] = solution.getIntVal(objectives[i]);
        }

        if (firstSolution) {
            firstSolution = false;
            paretoObjectiveValues.add(solutionObjectives);
        } else {
            paretoObjectiveValues.set(paretoObjectiveValues.size() - 1, solutionObjectives);
        }

        onNewSolution(paretoObjectiveValues);
    }


    private List<int[]> deepCopyFront(List<int[]> front) {
        List<int[]> copy = new ArrayList<>(front.size());
        for (int[] arr : front) {
            copy.add(arr.clone());
        }
        return copy;
    }

    /**
     * Called at the end of the search to flush the last front (if any)
     */
    public void onEnd() {
        if (pendingFront != null) {
            print(pendingSecond, pendingFront);
            pendingFront = null;
        }
    }

    /**
     * Prints the front in a Python-friendly format like:
     * HYPERVOLUME: [3, [[721, 626], [638, 740]]]
     */
    private void print(int second, List<int[]> front) {
        StringBuilder sb = new StringBuilder();
        sb.append("HYPERVOLUME: [").append(second).append(", [");
        for (int i = 0; i < front.size(); i++) {
            sb.append(Arrays.toString(front.get(i)));
            if (i < front.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]]");
        System.out.println(sb);
    }

    public void setFirstSolution(boolean firstSolution) {
        this.firstSolution = firstSolution;
    }
}
