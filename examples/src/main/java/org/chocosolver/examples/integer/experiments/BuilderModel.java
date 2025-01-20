package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.integer.experiments.benchmarkreader.Augmecon2;
import org.chocosolver.examples.integer.experiments.benchmarkreader.BenchamarkReader;
import org.chocosolver.examples.integer.experiments.benchmarkreader.ModelObjectivesVariables;
import org.chocosolver.examples.integer.experiments.benchmarkreader.vOptLibUKP;

public class BuilderModel {
    private final BenchamarkReader benchmarkReader;

    public BuilderModel(Config config) {
        this.benchmarkReader = defineBenchmarkReader(config);
    }

    private BenchamarkReader defineBenchmarkReader(Config config) {
        if (config.getBenchmark().contains("augmecon")) {
            return new Augmecon2(config);
        }
        if (config.getBenchmark().contains("vOptLib")) {
            // check for the different problems of vOptLib
            ProblemName problemName;
            try {
                problemName = ProblemName.valueOf(config.getProblem().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid problem name: " + config.getProblem());
            }
            switch (problemName) {
                case UKP:
                    return new vOptLibUKP(config);
                default:
                    throw new IllegalArgumentException("Invalid problem name: " + problemName);
            }
        }
        throw new IllegalArgumentException("Invalid benchmark: " + config.getBenchmark());
    }

    public ModelObjectivesVariables createModel(int index) {
        return benchmarkReader.createModel(index);
    }
}

enum ProblemName {
    UKP, SIMS
}