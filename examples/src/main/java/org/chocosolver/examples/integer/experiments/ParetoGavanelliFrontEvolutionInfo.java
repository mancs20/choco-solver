package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoGavanelliFrontEvolutionInfo {

    public Object[] run(Model model, IntVar[] objectives, boolean maximize) {
        // Optimise independently two variables using the Pareto optimizer
        Object[] solutionsAndStats;
        solutionsAndStats = model.getSolver().findParetoFrontWithFrontEvolutionInfo(objectives, maximize);
        List<Solution> solutions = (List<Solution>) solutionsAndStats[0];
        List<String> stats = (List<String>) solutionsAndStats[1];
        List<Solution> allSolutions = (List<Solution>) solutionsAndStats[2];

        return new Object[]{solutions, stats, allSolutions};
    }
}
