package org.chocosolver.solver.objective;

import org.chocosolver.solver.Solution;

import java.util.ArrayList;
import java.util.List;

public abstract class ParetoAbstract {

    protected List<Solution> solutions = new ArrayList<>();
    protected List<Solution> allSolutions = new ArrayList<>();
    protected final List<String> recorderList = new ArrayList<>();
    protected boolean exhaustive;

    public void setSolutions(List<Solution> solutions) {
        this.solutions = solutions;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    public List<String> getRecorderList() {
        return recorderList;
    }

    public List<Solution> getAllSolutions() {
        return allSolutions;
    }

    public boolean isExhaustive() {
        return exhaustive;
    }
}
