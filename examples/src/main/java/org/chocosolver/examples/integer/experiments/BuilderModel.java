package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.integer.experiments.benchmarkreader.*;

public class BuilderModel {
    private final BenchamarkReader benchmarkReader;

    public BuilderModel(Config config) {
        this.benchmarkReader = defineBenchmarkReader(config);
    }

    private BenchamarkReader defineBenchmarkReader(Config config) {
        // check if the benchmark is a flatzinc file, has extension .fzn
        if (config.getInstancePath().endsWith(".fzn")) {
            return new FlatZincReader(config);
        }
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
        if (config.getBenchmark().contains("MOOLibrary")) {
            // check for the different problems of MOOLibrary
            ProblemName problemName;
            try {
                problemName = ProblemName.valueOf(config.getProblem().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid problem name: " + config.getProblem());
            }
            switch (problemName) {
                case UKP:
                    return new MOOLibraryUKP(config);
                default:
                    throw new IllegalArgumentException("Invalid problem name: " + problemName);
            }
        }
        if (config.getBenchmark().contains("taillard") && config.getInstancePath().endsWith(".fzn")) {
            return new TaillardDznFileReader(config);
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