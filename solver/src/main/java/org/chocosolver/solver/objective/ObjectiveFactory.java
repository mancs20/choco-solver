/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Factory to create (mono-)objective managers.
 *
 * @author Arnaud Malapert
 */
public final class ObjectiveFactory {


    private ObjectiveFactory() {
        super();
    }

    /**
     * Define a manager for satisfaction problems.
     *
     * @return a singleton object
     */
    public static IObjectiveManager<Variable> SAT() {
        return SATManager.getInstance();
    }

    /**
     * Define a manager for GIA multiobjective optimization.
     *
     * @return a singleton object
     */
    public static IObjectiveManager<Variable> GIA(IntVar[] objectives, boolean tightUpperBound) {
        return GIAManager.getInstance(objectives, tightUpperBound);
    }

    /**
     * Define the variable to optimize (maximize or minimize)
     * By default, the manager uses {@link IObjectiveManager#setStrictDynamicCut()} to avoid exploring worse solutions.
     *
     * @param objective variable to optimize
     * @param policy    {{@link ResolutionPolicy#MINIMIZE}/{@link ResolutionPolicy#MAXIMIZE}
     * @return the objective manager
     * @throws IllegalArgumentException if the policy is {@link ResolutionPolicy#SATISFACTION}.
     */
    public static IObjectiveManager<IntVar> makeObjectiveManager(IntVar objective, ResolutionPolicy policy) {
        IObjectiveManager<IntVar> objman;
        switch (policy) {
            case MINIMIZE:
                objman = new MinIntObjManager(objective);
                break;
            case MAXIMIZE:
                objman = new MaxIntObjManager(objective);
                break;
            default:
                throw new IllegalArgumentException("cant build integer objective manager :" + policy);
        }
        objman.setStrictDynamicCut();
        return objman;
    }

    /**
     * Define the variable to optimize (maximize or minimize)
     * By default, the manager uses {@link IObjectiveManager#setStrictDynamicCut()} to avoid exploring worse solutions.
     *
     * @param objective variable to optimize
     * @param policy    {{@link ResolutionPolicy#MINIMIZE}/{@link ResolutionPolicy#MAXIMIZE}
     * @return the objective manager
     * @throws IllegalArgumentException if the policy is {@link ResolutionPolicy#SATISFACTION}.
     */
    public static IObjectiveManager<RealVar> makeObjectiveManager(RealVar objective, ResolutionPolicy policy, double precision) {
        IObjectiveManager<RealVar> objman;
        switch (policy) {
            case MINIMIZE:
                objman = new MinRealObjManager(objective, precision);
                break;
            case MAXIMIZE:
                objman = new MaxRealObjManager(objective, precision);
                break;
            default:
                throw new IllegalArgumentException("cant build real objective manager :" + policy);
        }
        objman.setStrictDynamicCut();
        return objman;
    }

    /**
     * @param object to copy
     * @return copy built by a copy constructor if one exists, otherwise the parameter.
     */
    @SuppressWarnings("unchecked")
    public static <V> V copy(V object) {
        try {
            Class<?> c = object.getClass();
            // Use the "copy constructor":
            Constructor<?> ct = c.getConstructor(c);
            return (V) ct.newInstance(object);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // fails silently
        }
        return object;
    }
}

/**
 * A class for CSP (in opposition to COP) which matches {@link IObjectiveManager} requisites.
 */
final class SATManager implements IObjectiveManager<Variable> {

    private static final long serialVersionUID = 2115489336441115889L;
    private final static SATManager INSTANCE = new SATManager();

    public static SATManager getInstance() {
        return INSTANCE;
    }

    private SATManager() {}

    /**
     * readResolve method to preserve singleton property
     */
    private Object readResolve() {
        // Return the one true INSTANCE and let the garbage collector
        // take care of the INSTANCE impersonator.
        return INSTANCE;
    }

    @Override
    public ResolutionPolicy getPolicy() {
        return ResolutionPolicy.SATISFACTION;
    }

    @Override
    public boolean isOptimization() {
        return false;
    }

    @Override
    public Number getBestLB() {
        throw new UnsupportedOperationException("There is no objective bounds in satisfaction problems");
    }

    @Override
    public Number getBestUB() {
        throw new UnsupportedOperationException("There is no objective bounds in satisfaction problems");
    }

    @Override
    public Number getBestSolutionValue() {
        throw new UnsupportedOperationException("There is no objective variable in satisfaction problems");
    }

    @Override
    public Variable getObjective() {
        return null;
    }

    @Override
    public boolean updateBestSolution(Number n) {
        throw new UnsupportedOperationException("not a mono-objective optimization problem");
    }

    @Override
    public boolean updateBestSolution() {
        // nothing to do
        return false;
    }

    @Override
    public void setWalkingDynamicCut() {
        // nothing to do
    }

    @Override
    public void setStrictDynamicCut() {
        // nothing to do
    }

    @Override
    public void setCutComputer(Function<Number, Number> cutComputer) {
        // nothing to do
    }

    @Override
    public void postDynamicCut() {
        // nothing to do
    }

    @Override
    public String toString() {
        return "SAT";
    }

}

class GIAManager implements IObjectiveManager<Variable> {

    private static final long serialVersionUID = 2115489336443115889L;
    private static GIAManager INSTANCE = null; // Lazy initialization

    /**
     * The variable to optimize
     **/
    transient protected final IntVar[] objectives;

    /**
     * define the precision to consider a variable as instantiated
     **/
    protected final int precision = 1;

    /**
     * best lower bounds found so far
     **/
    protected int[] bestProvedLB;
    protected int[] initialLB;

    /**
     * best upper bounds found so far
     **/
    protected int[] bestProvedUB;
    protected int[] initialUB;

    /**
     * Tight upper bound considering the obtained front
     **/
    protected boolean tightUpperBound;

    /**
     * Pareto front
     **/
    protected List<int[]> paretoFront = new ArrayList<>();

    /**
     * Define how the cut should be updated when posting the cut
     **/
    transient protected IntUnaryOperator cutComputer = n -> n; // walking cut by default

    private GIAManager(IntVar[] objectives, boolean tightUpperBound) {
        this.objectives = objectives;
        this.bestProvedLB = new int[objectives.length];  // Initialize best bounds
        this.bestProvedUB = new int[objectives.length];
        this.initialLB = new int[objectives.length];
        this.initialUB = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            bestProvedLB[i] = objectives[i].getLB();
            initialLB[i] = objectives[i].getLB();
            bestProvedUB[i] = objectives[i].getUB();
            initialUB[i] = objectives[i].getUB();
        }
        this.tightUpperBound = tightUpperBound;
    }

    /**
     * Get the singleton instance. Throws an exception if not initialized.
     */
    public static GIAManager getInstance(IntVar[] objectives, boolean tightUpperBound) {
        if (INSTANCE == null) {
            INSTANCE = new GIAManager(objectives, tightUpperBound);
        } else {
            System.arraycopy(INSTANCE.initialLB, 0, INSTANCE.bestProvedLB, 0, INSTANCE.objectives.length);
            System.arraycopy(INSTANCE.initialUB, 0, INSTANCE.bestProvedUB, 0, INSTANCE.objectives.length);
        }
        return INSTANCE;
    }

    /**
     * readResolve method to preserve singleton property during deserialization.
     */
    private Object readResolve() {
        return INSTANCE;
    }


    @Override
    public ResolutionPolicy getPolicy() {
        return ResolutionPolicy.SATISFACTION;
    }

    @Override
    public Number getBestLB() {
        throw new UnsupportedOperationException("There is no objective bounds in satisfaction problems");
    }

    @Override
    public Number getBestUB() {
        throw new UnsupportedOperationException("There is no objective bounds in satisfaction problems");
    }

    @Override
    public Number getBestSolutionValue() {
        throw new UnsupportedOperationException("There is no objective variable in satisfaction problems");
    }

    @Override
    public Variable getObjective() {
        return null;
    }

    @Override
    public boolean updateBestSolution(Number n) {
        return false;
    }

    @Override
    public boolean updateBestSolution() {
        boolean improved = true;
        for (int i = 0; i < objectives.length; i++) {
            if (!objectives[i].isInstantiated()) {
                throw new SolverException(
                        "objective variable (" + objectives[i] + ") is not instantiated on solution. Check constraints and/or decision variables.");
            }
            if (bestProvedLB[i] > objectives[i].getValue()) {
                improved = false;
                break;
            }
        }
        if (improved) {
            for (int i = 0; i < objectives.length; i++) {
                bestProvedLB[i] = objectives[i].getValue();
            }
        }
        if (tightUpperBound && improved) {
            if (addNewPointToParetoFront()) {
                updataUpperBounds();
            }
        }
        return improved;
    }

    private boolean addNewPointToParetoFront() {
        if (paretoFront.isEmpty()) {
            paretoFront.add(Arrays.copyOf(bestProvedLB, bestProvedLB.length));
            return false;
        }
        boolean addNewPoint = false;
        // if the bestProvedLB is improved, we can update the pareto front. There are two cases:
        // 1. the new point has at least one objective lower than the last point of the pareto front, it means that a new
        // non-dominated point is found and we are in the improvement phase.
        // 2. the new point does not have any objective lower than the last point of the pareto front, it means that the
        // last point of the pareto front is dominated by the new point and we need to replace it.
        for (int i = 0; i < objectives.length; i++) {
            if (bestProvedLB[i] < paretoFront.get(paretoFront.size() - 1)[i]) {
                addNewPoint = true;
                break;
            }
        }
        int[] newPoint = Arrays.copyOf(bestProvedLB, bestProvedLB.length);
        if (addNewPoint) {
            paretoFront.add(newPoint);
        } else {
            paretoFront.set((paretoFront.size() - 1), newPoint);
        }
        return addNewPoint;
    }

    private void updataUpperBounds() {
        for (int i = 0; i < bestProvedUB.length; i++) {
            int[] dominatingPoint = computeDominatingPointLastObjectiveVal(i);
            bestProvedUB[i] = computeLowestUBToAvoidDomination(dominatingPoint, i);
        }
    }

    private int[] computeDominatingPointLastObjectiveVal(int i) {
        int[] dp = Arrays.copyOf(bestProvedLB, bestProvedLB.length);
        dp[i] = initialUB[i];
        return dp;
    }

    private int computeLowestUBToAvoidDomination(int[] dominatingPoint, int i) {
        int highestPossibleUpperBound = initialUB[i];
        for (int j = 0; j < paretoFront.size() - 1; j++) {
            if (dominates(dominatingPoint, paretoFront.get(j))) {
                int currentPoint = paretoFront.get(j)[i] - 1;
                if (highestPossibleUpperBound > currentPoint) {
                    highestPossibleUpperBound = currentPoint;
                }
            }
        }
        return highestPossibleUpperBound;
    }

    private boolean dominates(int[] a, int[] b) {
        for (int j = 0; j < objectives.length; j++) {
            if (a[j] < b[j]) return false;
        }
        return true;
    }

    @Override
    public void setCutComputer(Function<Number, Number> cutComputer) {

    }

    @Override
    public void setStrictDynamicCut() {
        cutComputer = n -> n + precision;
    }

    @Override
    public void setWalkingDynamicCut() {
        cutComputer = n -> n;
    }

    @Override
    public void postDynamicCut() throws ContradictionException {
        // todo deal with the case where objectives are instantiated and equal to the bestProvedLB
        boolean sameSolution = true;
        for (int i = 0; i < objectives.length; i++) {
            objectives[i].updateLowerBound(cutComputer.applyAsInt(bestProvedLB[i]), this);
            objectives[i].updateUpperBound(bestProvedUB[i], this);
            if (!objectives[i].isInstantiated() || objectives[i].getValue() > bestProvedLB[i]) {
                sameSolution = false;
            }
        }
        if (sameSolution) {
            throw new ContradictionException();
        }
    }
}