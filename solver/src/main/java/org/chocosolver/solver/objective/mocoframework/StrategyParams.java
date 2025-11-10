package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.component.findsolution.SolutionEpsilonArrayInformation;

import java.util.*;

/**
 * A structured container for type-safe access and grouping of algorithm parameters.
 */
public class StrategyParams {

    // ────────────── Generic Solver Parameters ──────────────
    private boolean addIntermediateSolutions = false;
    private boolean checkIfNewSolutionDominates = true;
    private boolean exhaustive = true;
    private Constraint objectiveFunction;
    private boolean useOptimization = false;
    private int[] lexicographicOptimizationOrder;
    private boolean useLexicographicOptimization = false;
    private final List<String> recorderList = new ArrayList<>();

    // ────────────── Ideal/Nadir Point Estimation ──────────────
    private int[] idealPoint;
    private int[] nadirPoint;
    private Solution[] idealSolutions;

    // ────────────── SAUGMECON-Specific Parameters ──────────────
    private int[] objectivesOrder;
    private int[] epsilonArray;
    private int[] relativeWorstValue;
    private Set<String> previousSolutions = new HashSet<>();
    private List<SolutionEpsilonArrayInformation> previousSolutionInfo = new ArrayList<>();

    // ────────────── Getters and Setters ──────────────

    public boolean isAddIntermediateSolutions() {
        return addIntermediateSolutions;
    }

    public void setAddIntermediateSolutions(boolean value) {
        this.addIntermediateSolutions = value;
    }

    public boolean isCheckIfNewSolutionDominates() {
        return checkIfNewSolutionDominates;
    }

    public void setCheckIfNewSolutionDominates(boolean check) {
        this.checkIfNewSolutionDominates = check;
    }

    public boolean isExhaustive() {
        return exhaustive;
    }

    public void setExhaustive(boolean value) {
        this.exhaustive = value;
    }

    public Constraint getObjectiveFunction() {
        return objectiveFunction;
    }

    public void setObjectiveFunction(Constraint constraint) {
        this.objectiveFunction = constraint;
        this.useOptimization = true;
    }

    public boolean isOptimization() {
        return this.useOptimization;
    }

    public void setUseOptimization(boolean value) {
        this.useOptimization = value;
    }

    public int[] getLexicographicOptimizationOrder() {
        return lexicographicOptimizationOrder;
    }

    public void setLexicographicOptimizationOrder(int[] order) {
        this.lexicographicOptimizationOrder = order;
        this.useLexicographicOptimization = true;
    }

    public boolean isLexicographicOptimization() {
        return useLexicographicOptimization;
    }

    public void setUseLexicographicOptimization(boolean value) {
        this.useLexicographicOptimization = value;
    }

    public List<String> getRecorderList() {
        return recorderList;
    }

    public int[] getIdealPoint() {
        return idealPoint;
    }

    public void setIdealPoint(int[] idealPoint) {
        this.idealPoint = idealPoint;
    }

    public int[] getNadirPoint() {
        return nadirPoint;
    }

    public void setNadirPoint(int[] nadirPoint) {
        this.nadirPoint = nadirPoint;
    }

    public Solution[] getIdealSolutions() {
        return idealSolutions;
    }

    public void setIdealSolutions(Solution[] idealSolutions) {
        this.idealSolutions = idealSolutions;
    }

    // ────── SAUGMECON-specific Getters/Setters ──────

    public int[] getObjectivesOrder() {
        return objectivesOrder;
    }

    public void setObjectivesOrder(int[] order) {
        this.objectivesOrder = order;
    }

    public int[] getEpsilonArray() {
        return epsilonArray;
    }

    public void setEpsilonArray(int[] epsilonArray) {
        this.epsilonArray = epsilonArray;
    }

    public int[] getRwv() {
        return relativeWorstValue;
    }

    public void setRwv(int[] rwv) {
        this.relativeWorstValue = rwv;
    }

    public Set<String> getPreviousSolutions() {
        return previousSolutions;
    }

    public void setPreviousSolutions(Set<String> previousSolutions) {
        this.previousSolutions = previousSolutions;
    }

    public List<SolutionEpsilonArrayInformation> getPreviousSolutionInfo() {
        return previousSolutionInfo;
    }

    public void setPreviousSolutionInfo(List<SolutionEpsilonArrayInformation> previousSolutionInfo) {
        this.previousSolutionInfo = previousSolutionInfo;
    }
}