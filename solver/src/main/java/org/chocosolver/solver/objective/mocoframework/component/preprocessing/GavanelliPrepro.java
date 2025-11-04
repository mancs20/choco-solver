package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.constraints.Constraint;

import java.util.Collections;
import java.util.Map;

/**
 * Preprocessing step for Gavanelli's strategy: it posts the Pareto global constraint.
 */
public class GavanelliPrepro implements PreprocessingStrategy {

    @Override
    public Map<String, Object> apply(Model model, IntVar[] objectives, ParetoArchive paretoArchive) {
        // Post the Pareto Global constraint to filter dominated regions
        ParetoMaximizer pareto = new ParetoMaximizer(objectives);
        pareto.setSharedFront(paretoArchive.getParetoFrontSolutions(), paretoArchive.getParetoFrontValues());
        Constraint paretoGlobal = new Constraint("ParetoGlobal", pareto);
        paretoGlobal.post();

        return Collections.emptyMap(); // No specific parameters needed by this strategy
    }
}
