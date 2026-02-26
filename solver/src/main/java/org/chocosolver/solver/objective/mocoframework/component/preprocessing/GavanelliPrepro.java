package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.util.criteria.Criterion;


/**
 * Preprocessing step for Gavanelli's strategy: it posts the Pareto global constraint.
 */
public class GavanelliPrepro implements PreprocessingStrategy {

    final private boolean disableAfterFirstSolution;

    public GavanelliPrepro(boolean disableAfterFirstSolution) {
        this.disableAfterFirstSolution = disableAfterFirstSolution;
    }

    @Override
    public void apply(Model model, IntVar[] objectives, ParetoArchive paretoArchive, StrategyParams params, Criterion... stop) {
        // Post the Pareto Global constraint to filter dominated regions
        ParetoMaximizer pareto = new ParetoMaximizer(objectives);
        pareto.setSharedFront(paretoArchive.getParetoFrontSolutions(), paretoArchive.getParetoFrontValues());
        Constraint paretoGlobal = new Constraint("ParetoGlobal", pareto);
        paretoGlobal.post();
        params.setParetoMaximizer(pareto);
        params.setDisableParetoMaximizerAfterFirstSolution(disableAfterFirstSolution);
    }
}
