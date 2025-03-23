package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoGavanelliFrontEvolutionInfo {

    public Object[] run(Model model, IntVar[] objectives, boolean maximize) {
        // Find the Pareto front using the Pareto optimizer
        List<Solution> solutions = model.getSolver().findParetoFrontWithFrontEvolutionInfo(objectives, maximize);
        // stats
        List<String> recorderList = new ArrayList<>();
        recorderList.add(model.getSolver().getMeasures().toString());

        return new Object[]{solutions, recorderList};
    }
}
