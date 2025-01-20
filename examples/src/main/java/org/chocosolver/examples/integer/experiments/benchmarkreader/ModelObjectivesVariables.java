package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class ModelObjectivesVariables {
    private Model model;
    private IntVar[] objectives;
    private IntVar[] decisionVaraibles;
    private final boolean maximization;

    public ModelObjectivesVariables(Model model, IntVar[] objectives, IntVar[] decisionVaraibles, boolean maximization) {
        this.model = model;
        this.objectives = objectives;
        this.decisionVaraibles = decisionVaraibles;
        this.maximization = maximization;
    }

    public Model getModel() {
        return model;
    }

    public IntVar[] getObjectives() {
        return objectives;
    }

    public IntVar[] getDecisionVaraibles() {
        return decisionVaraibles;
    }

    public boolean isMaximization() {
        return maximization;
    }
}
