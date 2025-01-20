package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.examples.integer.experiments.Config;
import org.chocosolver.solver.Model;

public abstract class BenchamarkReader {

    protected Model model;
    protected final Config config;
    public BenchamarkReader(Config config) {
        this.config = config;
    }

    /**
     * Create a model
     * @return an array of objects, where element 0 is the model,
     */
    public abstract ModelObjectivesVariables createModel(int index);

    public String getModelName(int index) {
        return config.getModelName() + "_" + index;
    }
}
