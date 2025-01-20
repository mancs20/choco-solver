package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.List;

public class ParetoDisjunctiveProgramming {
    public Object[] run(Model model, IntVar[] objectives, boolean maximize, int timeout, boolean regionSelectionLikePaper) {
        // Optimise independently two variables using the Pareto optimizer
        Object[] solutionsAndStats = new Object[0];
        try {
            if (regionSelectionLikePaper)
                solutionsAndStats = model.getSolver().findParetoFrontByDisjunctiveProgramming(objectives, maximize, timeout);
            else
                solutionsAndStats = model.getSolver().findParetoFrontByDisjunctiveProgrammingSelRegionLikeImproveAll(objectives, maximize, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Solution> solutions = (List<Solution>) solutionsAndStats[0];
        List<String> stats = (List<String>) solutionsAndStats[1];

        return new Object[]{solutions, stats};
    }
}
