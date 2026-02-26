package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.objective.mocoframework.component.findsolution.SolutionEpsilonArrayInformation;
import org.chocosolver.solver.search.loop.monitors.NogoodFromDominanceFails;

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
    private final Set<String> previousSolutions = new HashSet<>();
    private final List<SolutionEpsilonArrayInformation> previousSolutionInfo = new ArrayList<>();

    // ────────────── Gavanelli-Specific Parameters ──────────────
    private ParetoMaximizer paretoMaximizer;
    private boolean disableParetoMaximizerAfterFirstSolution = false;
    public int objectiveValueToDisableParetoMaximizer = Integer.MIN_VALUE;

    // ────────────── No good on solutions and on restart ──────────────
    private boolean useNoGoodOnSolution = false;
    private NogoodFromDominanceFails nogoodFromDominanceFails;
    private boolean noGoodsLearntFromNonObjectiveFails = false;

    // ────────────── Use objective manager for the objectives domain ──────────────
    private boolean useObjectiveManagerForObjectivesDomain = false;

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

    public List<SolutionEpsilonArrayInformation> getPreviousSolutionInfo() {
        return previousSolutionInfo;
    }

    public ParetoMaximizer getParetoMaximizer() {
        return paretoMaximizer;
    }

    public void setParetoMaximizer(ParetoMaximizer paretoMaximizer) {
        this.paretoMaximizer = paretoMaximizer;
    }

    public void setNoGoodOnSolution(Boolean useNoGoodOnSolution){this.useNoGoodOnSolution = useNoGoodOnSolution;}

    public boolean isUseObjectiveManagerForObjectivesDomain() {
        return useObjectiveManagerForObjectivesDomain;
    }

    public void setUseObjectiveManagerForObjectivesDomain(boolean useObjectiveManagerForObjectivesDomain) {
        this.useObjectiveManagerForObjectivesDomain = useObjectiveManagerForObjectivesDomain;
    }

    public NogoodFromDominanceFails getNogoodFromDominanceFails() {
        return nogoodFromDominanceFails;
    }

    public void setNogoodFromDominanceFails(NogoodFromDominanceFails nogoodFromDominanceFails, boolean isNoGoodLearntFromNonObjectiveFails) {
        this.nogoodFromDominanceFails = nogoodFromDominanceFails;
        this.noGoodsLearntFromNonObjectiveFails = isNoGoodLearntFromNonObjectiveFails;
    }

    public boolean isNoGoodsLearntFromNonObjectiveFails() {
        return noGoodsLearntFromNonObjectiveFails;
    }

    public boolean isDisableParetoMaximizerAfterFirstSolution() {
        return disableParetoMaximizerAfterFirstSolution;
    }

    public void setDisableParetoMaximizerAfterFirstSolution(boolean disableParetoMaximizerAfterFirstSolution) {
        this.disableParetoMaximizerAfterFirstSolution = disableParetoMaximizerAfterFirstSolution;
    }
}