package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.variables.IntVar;

import java.util.Map;

public interface PreprocessingStrategy {
    Map<String, Object> apply(Model model, IntVar[] objectives, ParetoArchive archive);
}