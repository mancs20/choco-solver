package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.List;

public class ParetoDisjunctiveProgramming {
    public Object[] run(Model model, IntVar[] objectives, boolean maximize, int timeout) {
        // Optimise independently two variables using the Pareto optimizer
        long startTime = System.nanoTime();
        Object[] solutionsAndStats = new Object[0];
        try {
            solutionsAndStats = model.getSolver().findParetoFrontByDisjunctiveProgramming(objectives, maximize, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        float elapsedTime = (float) (endTime - startTime) / 1_000_000_000;
        List<Solution> solutions = (List<Solution>) solutionsAndStats[0];
        List<String> stats = (List<String>) solutionsAndStats[1];

        return new Object[]{solutions, stats, elapsedTime};
    }
}
