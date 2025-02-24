package org.chocosolver.examples.integer;/*
 * Copyright (C) 2017 COSLING S.A.S.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.chocosolver.examples.integer.experiments.benchmarkreader.ModelObjectivesVariables;
import org.chocosolver.examples.integer.experiments.frontgenerators.TimeoutHolder;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.ParallelPortfolio;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.objective.GiaConfig;
import org.chocosolver.solver.objective.IMultiObjectiveManager;
import org.chocosolver.solver.objective.ParetoMaximizerGIACoverage;
import org.chocosolver.solver.objective.ParetoMaximizerGIAGeneral;
import org.chocosolver.solver.search.ParetoFeasibleRegion;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Simple Choco Solver example involving multi-objective optimization
 * @author Jean-Guillaume FAGES (cosling)
 * @version choco-solver-4.0.4
 */


abstract public class ParetoGIA implements TimeoutHolder, IMultiObjectiveManager {
    protected GiaConfig config;
    protected int numObjectivesAllowed;
    protected Model model;
    protected Solver solver;
    protected IntVar[] objectives;
    protected List<Solution> paretoSolutions = new ArrayList<>();
    protected List<String> recorderList = new ArrayList<>();
    protected ParetoMaximizerGIAGeneral paretoPoint;
    protected float timeout;
    protected int[] lastObjectiveValues;
    protected boolean stopCondition;
    protected long startTimeNano;

    public ParetoGIA(GiaConfig config, int timeout) {
        this.config = config;
        this.timeout = timeout;
        stopCondition = false;
        numObjectivesAllowed = 0; // indefinite number of objectives
    }

    public Object[] run(boolean maximize, Model model, IntVar[] objectives) {
        // Optimise independently two variables using the Pareto optimizer
        startTimeNano = System.nanoTime();
        preparation(maximize, model, objectives);
        Object[] solutionsAndStats = new Object[0];
        try {
            solutionsAndStats = findFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Solution> solutions = (List<Solution>) solutionsAndStats[0];
        List<String> stats = (List<String>) solutionsAndStats[1];

        return new Object[]{solutions, stats};
    }

    private void preparation(boolean maximize, Model model, IntVar[] objectives){
        // prepare model and solver
        this.model = model;
        this.solver = model.getSolver();
        //convert to maximization problem
        this.objectives = Stream.of(objectives).map(o -> maximize ? o : model.neg(o)).toArray(IntVar[]::new);
        this.lastObjectiveValues = new int[this.objectives.length];
        try {
            if (numObjectivesAllowed > 0 && objectives.length > numObjectivesAllowed){
                throw new Exception("Finding the pareto front by improving all objectives is only implemented for " + numObjectivesAllowed + " objectives");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected Object[] findFront() {
        // add pareto constraint
        paretoPoint = setGIAPropagator(objectives, false);
        paretoPoint.setBoundedType(config.getBounded());

        Constraint c = new Constraint("paretoGIA", paretoPoint);
        c.post();
        paretoPoint.prepareGIAMaximizerFirstSolution();
        boolean keepExploring = true;
        long solutionCount = 0;
        while (!stopCondition && keepExploring){
            keepExploring = getFrontPoint();
            solutionCount++;
            solver.getMeasures().setRestartCount(solutionCount);
        }
        return new Object[]{paretoSolutions, recorderList};
    }

    protected abstract ParetoMaximizerGIAGeneral setGIAPropagator(IntVar[] objectives, boolean portfolio);

    protected boolean getFrontPoint(){
        timeout = updateSolverTimeoutCurrentTime(solver, timeout, startTimeNano);
        boolean foundSolution = false;
        try {
            while(solver.solve()){
                paretoPoint.onSolution();
                foundSolution = true;
            }
        } catch (Exception e) {
            System.err.println("Exception during solving: " + e.getMessage());
            e.printStackTrace();
            foundSolution = false;
        }
        // Get statistics
        try {
            recorderList.add(solver.getMeasures().toString());
        } catch (Exception e) {
            System.err.println("Exception recording stats: " + e.getMessage());
            e.printStackTrace();
        }
        if (foundSolution) {
            Solution solution = paretoPoint.getLastFeasibleSolution();
            if (solution != null) {
                lastObjectiveValues = paretoPoint.getLastObjectiveVal();
                paretoSolutions.add(solution);
            }
            // reset to the initial state
            if (solver.isStopCriterionMet() || ((config.getCriteriaSelection() == GiaConfig.CriteriaSelection.NONE) && solution == null)) {
                stopCondition = true;
            } else {
                solver.reset(); // if reset is does not work, use the search strategy regionSearch
            }
            paretoPoint.prepareGIAMaximizerForNextSolution();
        }
        return foundSolution;
    }

    public Object[] runOriginal(Model model, IntVar[] objectives, boolean maximize, int timeout) {
        // Optimise independently two variables using the Pareto optimizer
        Object[] solutionsAndStats = new Object[0];
        try {
            solutionsAndStats = model.getSolver().BiObjGIA_regionImplementation(objectives, maximize, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Solution> solutions = (List<Solution>) solutionsAndStats[0];
        List<String> stats = (List<String>) solutionsAndStats[1];

        return new Object[]{solutions, stats};
    }

    public Object[] runWithPortfolio(ModelObjectivesVariables[] modelObjectivesVariables, boolean maximize, int timeout) {
        // Optimise independently two variables using the Pareto optimizer

        for (ModelObjectivesVariables mov : modelObjectivesVariables) {
            mov.getModel().getSolver().limitTime(timeout + "s");
        }
        long startTime = System.nanoTime();
        Object[] solutionsAndStats = new Object[0];
        try {
            solutionsAndStats = findParetoFrontByUnsatisfactionWithPortfolio(modelObjectivesVariables, maximize, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        float elapsedTime = (float) (endTime - startTime) / 1_000_000_000;
        List<Solution> solutions = (List<Solution>) solutionsAndStats[0];
        List<String> stats = (List<String>) solutionsAndStats[1];

        return new Object[]{solutions, stats, elapsedTime};
    }

    // todo needs to be reviewed
    private Object[] findParetoFrontByUnsatisfactionWithPortfolio(ModelObjectivesVariables[] modelObjectivesVariables,
                                                     boolean maximize, float timeout) throws Exception {

        List<Solution> paretoSolutions = new ArrayList<>();
        List<String> recorderList = new ArrayList<>();
        modelObjectivesVariables[0].getObjectives();
        if (modelObjectivesVariables[0].getObjectives().length != 2){
            throw new Exception("Finding the pareto front by unsatisfaction is only implemented for 2 objectives");
        }
        int[] lowerCorner = {modelObjectivesVariables[0].getObjectives()[0].getLB(), modelObjectivesVariables[0].getObjectives()[1].getLB()};
        int[] upperCorner = {modelObjectivesVariables[0].getObjectives()[0].getUB(), modelObjectivesVariables[0].getObjectives()[1].getUB()};
        ParetoFeasibleRegion initialRegion = new ParetoFeasibleRegion(lowerCorner, upperCorner);
        ArrayList<ParetoFeasibleRegion> possibleFeasibleHyperrectangles = new ArrayList<>();
        possibleFeasibleHyperrectangles.add(initialRegion);
        // todo check how to add the stop criterion
//        for (ModelObjectivesVariables mov : modelObjectivesVariables) {
//            mov.getModel().getSolver().addStopCriterion(stop);
//        }

        ParetoMaximizerGIACoverage[] paretoPointArray = new ParetoMaximizerGIACoverage[modelObjectivesVariables.length];
        for (int i = 0; i < modelObjectivesVariables.length; i++) {
            int finalI = i;
            paretoPointArray[i] = new ParetoMaximizerGIACoverage(
                    Stream.of(modelObjectivesVariables[i].getObjectives()).map(o -> maximize ? o : modelObjectivesVariables[finalI].getModel().neg(o)).toArray(IntVar[]::new),
                    true, choosePropagatorPriority(objectives.length));
            Constraint c = new Constraint("PARETOUNSATISFACTION", paretoPointArray[i]);
            c.post();
        }
        // portfolio
        ParallelPortfolio portfolio = new ParallelPortfolio();
        for (ModelObjectivesVariables mov : modelObjectivesVariables) {
            portfolio.addModel(mov.getModel());
        }

        boolean timeoutReached = false;
        while (possibleFeasibleHyperrectangles.size() > 0 && !timeoutReached){
            // TODO the part of the feasible region should be done in the ParetoGIASparsity
            ParetoFeasibleRegion feasibleRegion;
            feasibleRegion = getNextPossibleRegion(possibleFeasibleHyperrectangles);
            for (int i = 0; i < modelObjectivesVariables.length; i++) {
                paretoPointArray[i].configureInitialUbLb(feasibleRegion);
                paretoPointArray[i].setLastSolution(null);
            }
            Model finder;
            portfolio.stealNogoodsOnRestarts();
            int[] bestObjectiveValues = new int[modelObjectivesVariables[0].getObjectives().length];
            Solution bestSolution = null;
            while (portfolio.solve()) {
                finder = portfolio.getBestModel();
                //todo delete is for testing
                finder.getSolver().printShortStatistics();
                // identify which models have found a solution, get the last substring after '_'
                int bestModelID = Integer.parseInt(finder.getName().substring(finder.getName().lastIndexOf('_') + 1));
                boolean copyObjVals = false;
                for (int i = 0; i < bestObjectiveValues.length; i++) {
                    if (paretoPointArray[bestModelID].getObjectivesValue()[i] < bestObjectiveValues[i]) {
                        copyObjVals = false;
                        break;
                    }
                    if (paretoPointArray[bestModelID].getObjectivesValue()[i] > bestObjectiveValues[i]) {
                        copyObjVals = true;
                    }
                }
                if (copyObjVals){
                    bestObjectiveValues = Arrays.copyOf(paretoPointArray[bestModelID].getObjectivesValue(),
                            paretoPointArray[bestModelID].getObjectivesValue().length);
                    bestSolution = paretoPointArray[bestModelID].getLastFeasibleSolution();
                }
                Arrays.stream(paretoPointArray[bestModelID].getObjectivesValue()).forEach(element -> System.out.print(element + " "));
                System.out.println();
                paretoPointArray[bestModelID].onSolution();
                for (int i = 0; i < modelObjectivesVariables.length; i++) {
                    if (i != bestModelID){
                        paretoPointArray[i].setLastObjectiveVal(bestObjectiveValues);
                    }
                }
            }
            // Get statistics
            //todo how to combine the statistics, or which statics to show
            portfolio.getModels().forEach(m -> recorderList.add(m.getSolver().getMeasures().toString()));


            if (bestSolution != null){
                paretoSolutions.add(bestSolution);
                // add the 2 new possible feasible regions
                List<ParetoFeasibleRegion> newFeasibleRegions = model.getSolver().getNewParetoGIAFeasible2DRegions(feasibleRegion, bestObjectiveValues);
                possibleFeasibleHyperrectangles.addAll(newFeasibleRegions);
            }

            // reset to the initial state
            boolean stopCriterionMet = false;
            for (Model modelPortfolio: portfolio.getModels()) {
                if (modelPortfolio.getSolver().isStopCriterionMet()){
                    stopCriterionMet = true;
                    break;
                }
            }
            if (stopCriterionMet){
                timeoutReached = true;
            }else{
                // the elapsed time is the maximum time count  of all the models
                // how to get the maximun of portfolio.getModels().stream().map(m -> m.getSolver().getTimeCount())?
                float elapsedTime =  0;
                for (Model modelPortfolio: portfolio.getModels()) {
                    if (modelPortfolio.getSolver().getTimeCount() > elapsedTime){
                        elapsedTime = modelPortfolio.getSolver().getTimeCount();
                    }
                }
                timeout = timeout - elapsedTime;
//                solver.reset();
                portfolio.getModels().forEach(m -> m.getSolver().reset());
//                solver.limitTime(timeout + "s");
                float finalTimeout = timeout;
                portfolio.getModels().forEach(m -> m.getSolver().limitTime(finalTimeout + "s"));
            }
        }
        //todo remove the constraints
//        model.unpost(c);
//        solver.removeStopCriterion(stop);
        return new Object[]{paretoSolutions, recorderList};
    }

    private ParetoFeasibleRegion getNextPossibleRegion(ArrayList<ParetoFeasibleRegion> possibleFeasibleHyperrectangles){
        int idBiggerRegion = getBiggestRegionId(possibleFeasibleHyperrectangles);
        return possibleFeasibleHyperrectangles.remove(idBiggerRegion);
    }

    private int getBiggestRegionId(ArrayList<ParetoFeasibleRegion> possibleFeasibleHyperrectangles){
        int idBiggerRegion = 0;
        double maxVolume = 0;
        for (int i = 0; i < possibleFeasibleHyperrectangles.size(); i++) {
            ParetoFeasibleRegion region = possibleFeasibleHyperrectangles.get(i);
            double volume = region.getRegionVolume();
            if (volume > maxVolume){
                maxVolume = volume;
                idBiggerRegion = i;
            }
        }
        return idBiggerRegion;
    }

    public static PropagatorPriority choosePropagatorPriority(int numObjectives){
        return PropagatorPriority.LINEAR;
    }
}
