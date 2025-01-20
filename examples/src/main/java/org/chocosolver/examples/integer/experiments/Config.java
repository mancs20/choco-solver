package org.chocosolver.examples.integer.experiments;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Config {
    private final String benchmark;
    private final String problem;
    private final String instance;
    private final String instancePath;
    private final String modelName;
    private final String frontGenerator;
    private final String solverSearchStrategy;
    private final int solverTimeoutSec;
    private final int threads;
    private final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public Config(String benchmark, String problem, String instance, String instancePath, String frontGenerator,
                  String solverSearchStrategy, int solverTimeoutSec, int threads) {
        this.benchmark = benchmark;
        this.problem = problem;
        this.instance = instance;
        this.modelName = benchmark + "--" + problem + "--" + instance;
        this.instancePath = instancePath;
        this.frontGenerator = frontGenerator;
        this.solverSearchStrategy = solverSearchStrategy;
        this.solverTimeoutSec = solverTimeoutSec;
        this.threads = threads;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public String getProblem() {
        return problem;
    }

    public String getInstance() {
        return instance;
    }

    public String getModelName() {
        return modelName;
    }

    public String getInstancePath() {
        return instancePath;
    }

    public String getFrontGenerator() {
        return frontGenerator;
    }

    public String getSolverSearchStrategy() {
        return solverSearchStrategy;
    }

    public int getSolverTimeoutSec() {
        return solverTimeoutSec;
    }

    public String getDateTime() {
        return dateTime;
    }

    public int getThreads() {
        return threads;
    }
}
