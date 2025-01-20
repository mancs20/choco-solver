package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class ModelObjectivesVariables {
    private Model model;
    private IntVar[] objectives;
    private Object[] decisionVariables;
    private final boolean maximization;

    public ModelObjectivesVariables(Model model, IntVar[] objectives, Object[] decisionVaraibles, boolean maximization) {
        this.model = model;
        this.objectives = objectives;
        this.decisionVariables = decisionVaraibles;
        this.maximization = maximization;
    }

    public Model getModel() {
        return model;
    }

    public IntVar[] getObjectives() {
        return objectives;
    }

    public Object[] getDecisionVariables() {
        return decisionVariables;
    }

    public boolean isMaximization() {
        return maximization;
    }
}
