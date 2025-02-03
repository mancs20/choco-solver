package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class ModelObjectivesVariables {
    private Model model;
    private IntVar[] objectives;
    private Object[] decisionVariables;
    private IntVar[] decisionVariablesSearch;
    private final boolean maximization;

    public ModelObjectivesVariables(Model model, IntVar[] objectives, Object[] decisionVariables, boolean maximization) {
        this.model = model;
        this.objectives = objectives;
        this.decisionVariables = decisionVariables;
        if (decisionVariables instanceof IntVar[])
            this.decisionVariablesSearch = (IntVar[]) decisionVariables;
        else
            this.decisionVariablesSearch = new IntVar[0];
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

    public IntVar[] getDecisionVariablesSearch() {
        return decisionVariablesSearch;
    }

    public boolean isMaximization() {
        return maximization;
    }
}
