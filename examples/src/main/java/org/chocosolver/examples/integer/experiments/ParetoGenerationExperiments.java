package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.integer.Pareto;
import org.chocosolver.examples.integer.ParetoImproveAllObjs;
import org.chocosolver.examples.integer.ParetoSaugmecon;
import org.chocosolver.examples.integer.experiments.benchmarkreader.ModelObjectivesVariables;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * This class is used to run experiments to generate Pareto fronts for a given model
 * The parameters are passed as command-line arguments
 * @version choco-solver-4.0.4
 * Example usage: java ParetoGenerationExperiments <benchmark> <problem> <instancename> <instancePath> <solverSearchStrategy> <solverTimeoutSec> <frontGenerator> <threads>
 *     benchmark: the name of the benchmark to be used. (e.g. "augmecon2", "vOptLib")
 *     problem: the name of the problem to be used. (e.g. "ukp" (multiobjective unidimensional knapsack), "sims")
 *     instance: the name of the instance to be used. (e.g. "100u1" for augmecon2, "2KP50-1A" for vOptLib)
 *     instancePath: the path to the instance file
 *     solverSearchStrategy: the name of the solver search strategy to be used, for now it is "default", but it could "LNS"
 *     solverTimeoutSec: the maximum time in seconds that the solver will run
 *     frontGenerator: the name of the front generator to be used, it could be "ParetoImproveAllObjs", "ParetoGavanelliGlobalConstraint", "Saugmecon", "SaugmeconFrontVerify", "ParetoDisjunctiveProgramming"
 *     threads: the number of threads to be used, the default is 1
 *
 *     Examples:
 *     vOptLib ukp 2KP50-1A /Users/manuel.combarrosimon/choco-solver/bench/2KP50-1A.dat portfolio 14400 ParetoImproveAllObjs 6
 *     vOptLib ukp 2KP50-1A ../vOptLib/ukp/2KP50-1A.dat default 14400 ParetoImproveAllObjs
 *     augmecon2 ukp 100u /Users/manuel.combarrosimon/choco-solver/bench/2KP50-1A.dat default 3600 ParetoImproveAllObjs
 *
 */

public class ParetoGenerationExperiments {
    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Usage: java <benchmark> <problem> <instanceName> <instancePath> <solverSearchStrategy>" +
                            " <solverTimeoutSec> <frontGenerator> <threads>, with threads being optional");
            System.exit(1);
        }

        // Parsing command-line arguments
        String benchmark = args[0];
        String problem = args[1];
        String instance = args[2];
        String instancePath = args[3];
        String solverSearchStrategy = args[4];
        int solverTimeoutSec = Integer.parseInt(args[5]);
        String frontGenerator = args[6];
        int threads = 1;
        if (args.length >= 8 && !Objects.equals(args[7], "")) {
            threads = Integer.parseInt(args[7]);
        }

        // print starting experiment
        System.out.println("Starting experiment with benchmark:" + benchmark + ", problem:" + problem +
                ", instance:" + instance + ", frontGenerator:" + frontGenerator + ", solverSearchStrategy:" +
                solverSearchStrategy + ", solverTimeoutSec:" + solverTimeoutSec + ", threads:" + threads);

        // Create the Config object
        Config config = new Config(benchmark, problem, instance, instancePath, frontGenerator, solverSearchStrategy,
                solverTimeoutSec, threads);

        int portfolioSize = 1;
        if (config.getSolverSearchStrategy().equalsIgnoreCase("portfolio") && config.getThreads() > 1) {
            portfolioSize = config.getThreads();
        }

        boolean test = false;
        ModelObjectivesVariables[] modelAndObjectivesArray = new ModelObjectivesVariables[portfolioSize];
        ModelObjectivesVariables modelAndObjectives;
        if (test) {
            modelAndObjectives = createExampleModel(config);
        }else{
            BuilderModel builderModel = new BuilderModel(config);
            for (int i = 0; i < portfolioSize; i++) {
                modelAndObjectivesArray[i] = builderModel.createModel(i);
            }
            modelAndObjectives = modelAndObjectivesArray[0];
        }
        IntVar[] objectives = modelAndObjectives.getObjectives();
        Object[] decisionVariables = modelAndObjectives.getDecisionVariables();
        boolean maximization = modelAndObjectives.isMaximization();

        ParetoObjective[] originalObjectives = new ParetoObjective[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            originalObjectives[i] = new ParetoObjective(objectives[i].getLB(), objectives[i].getUB());
        }

        Object[] results;
        // todo find a way to know if it was exhaustive or not when portfolio is used
        boolean exhaustive = true;
        if (portfolioSize == 1) {
            Model model = modelAndObjectives.getModel();
            results = runFrontStrategy(model, objectives, frontGenerator, maximization, config.getSolverTimeoutSec());
            if (model.getSolver().isStopCriterionMet()){
                System.out.println("Solver time limit reached");
                exhaustive = false;
            }
        } else {
            results = runFrontStrategyWithPortfolio(modelAndObjectivesArray, frontGenerator, maximization, config.getSolverTimeoutSec());
        }

        outputProcessResults(results, exhaustive, objectives, originalObjectives, decisionVariables, maximization);

        // print ending experiment
        System.out.println("Ending experiment with benchmark:" + benchmark + ", problem:" + problem +
                ", instance:" + instance + ", frontGenerator:" + frontGenerator + ", solverSearchStrategy:" +
                solverSearchStrategy + ", solverTimeoutSec:" + solverTimeoutSec);
    }
    private static ModelObjectivesVariables createExampleModel(Config config) {
        Model model = new Model(config.getModelName());
        // We handle 4 types of food, described below
        int[] nbItems = new int[]{5, 2, 6, 7}; // number of items for each type
        int[] weights = new int[]{5, 8, 7, 8}; // weight of an item for each type
        int[] lipids = new int[]{5, 9, 8, 1}; // quantity of lipids of the item
        int[] glucose = new int[]{9, 5, 7, 32}; // quantity of glucose of the item

        // For each type, we create a variable for the number of occurrences
        IntVar[] occurrences = new IntVar[nbItems.length];
        for (int i = 0; i < nbItems.length; i++) {
            occurrences[i] = model.intVar(0, nbItems[i]);
        }

        // Total weight of the solution.
        IntVar weight = model.intVar(0, 80);
        // Total of lipids
        IntVar totalLipids = model.intVar(0, 200);
        // Total of glucose
        IntVar totalGlucose = model.intVar(0, 200);

        // We add two knapsack constraints to the solver
        // Beware : call the post() method to save it
        model.knapsack(occurrences, weight, totalLipids, weights, lipids).post();
        model.knapsack(occurrences, weight, totalGlucose, weights, glucose).post();

        return new ModelObjectivesVariables(model, new IntVar[]{totalLipids, totalGlucose}, occurrences, true);
    }

    private static Object[] runFrontStrategy(Model model, IntVar[] objectives, String frontGenerator, boolean maximize, int timeoutSec) {
        model.getSolver().limitTime(timeoutSec + "s");
        switch (frontGenerator) {
            case "ParetoImproveAllObjs":
                ParetoImproveAllObjs paretoImproveAllObjs = new ParetoImproveAllObjs();
                return paretoImproveAllObjs.run(model, objectives, maximize, timeoutSec);
            case "ParetoDisjunctiveProgramming":
                ParetoDisjunctiveProgramming paretoDisjunctiveProgramming = new ParetoDisjunctiveProgramming();
                return paretoDisjunctiveProgramming.run(model, objectives, maximize, timeoutSec);
            case "ParetoGavanelliGlobalConstraint":
                Pareto pareto = new Pareto();
                return pareto.run(model, objectives, maximize); // todo add timeout
            case "Saugmecon":
                ParetoSaugmecon paretoSaugmeconLex = new ParetoSaugmecon(true);
                return paretoSaugmeconLex.run(model, objectives, maximize, timeoutSec);
            case "SaugmeconFrontVerify":
                ParetoSaugmecon paretoSaugmecon = new ParetoSaugmecon(false);
                return paretoSaugmecon.run(model, objectives, maximize, timeoutSec);
            default:
                throw new IllegalArgumentException("Invalid front generator: " + frontGenerator);
        }
    }

    private static Object[] runFrontStrategyWithPortfolio(ModelObjectivesVariables[] modelObjectivesVariables, String frontGenerator, boolean maximize, int timeoutSec) {
        // todo the implementation of the portfolio is not finished, needs to be tested
        System.out.println("Running portfolio is not implemented yet. Exiting.");
        System.exit(1);

        //        model.getSolver().limitTime(timeoutSec + "s");
        switch (frontGenerator) {
            case "ParetoImproveAllObjs":
                ParetoImproveAllObjs paretoImproveAllObjs = new ParetoImproveAllObjs();
                return paretoImproveAllObjs.runWithPortfolio(modelObjectivesVariables, maximize, timeoutSec);
//            case "ParetoGavanelliGlobalConstraint":
//                Pareto pareto = new Pareto();
//                return pareto.run(model, objectives, maximize);
//            case "Saugmecon":
//                ParetoSaugmecon paretoSaugmecon = new ParetoSaugmecon();
//                return paretoSaugmecon.run(model, objectives, maximize, timeoutSec);
            default:
                throw new IllegalArgumentException("Invalid front generator: " + frontGenerator);
        }
    }

    private static void outputProcessResults(Object[] results, boolean exhaustive, IntVar[] modelObjectives,
                                             ParetoObjective[] objectives, Object[] decisionVariables, boolean maximize) {
        Map<String, Object> orderedMap = new LinkedHashMap<>();

        List<Solution> solutions = (List<Solution>) results[0];
        List<String> stats = (List<String>) results[1];
        float totalTime = (float) results[2];

        String[] solverMessages = new String[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            // Remove \n and \t
            String cleanedElement = stats.get(i).replaceAll("[\\n\\t]", " ");
            solverMessages[i] = "Model solved # " + (i+1) + " " + cleanedElement;
        }
        orderedMap.put("solver_messages", solverMessages);
        addTotalSolverStatsToJsonObject(orderedMap, stats.toArray(new String[0]), totalTime, exhaustive);

        // Get the hypervolume, the pareto front and the solutions of the problem
        ParetoFrontProcessor paretoFront = new ParetoFrontProcessor(solutions.toArray(new Solution[0]),
                modelObjectives, objectives, decisionVariables, maximize);

        // Print Pareto stats
        orderedMap.put("pareto_front", paretoFront.getParetoFront());
        orderedMap.put("solutions_pareto_front", paretoFront.getSolutionsParetoFront());
        double[][] bounds = paretoFront.getBounds();
        double[] referencePoint = new double[bounds.length];
        int referenceBound = maximize ? 0 : 1;
        for (int i = 0; i < referencePoint.length; i++) {
            referencePoint[i] = bounds[i][referenceBound];
        }
        orderedMap.put("reference_point", referencePoint);
        JSONObject jsonObject = new JSONObject(orderedMap);
        JSONObject type = new JSONObject();
        type.put("type", "solutions-details");
        type.put("solutions-details", jsonObject);

        // Print the JSON object
        System.out.println(type);
        System.out.println("There are "+solutions.size()+" Pareto-optimal solutions. Founded in " + totalTime + "s.");
    }

    private static void addTotalSolverStatsToJsonObject(Map<String, Object> orderedMap, String[] solverStats, float totalTime, boolean exhaustive) {
        // Initialize sums
        int totalSolutions = 0, totalNodes = 0, totalBacktracks = 0, totalBackjumps = 0, totalFails = 0, totalRestarts = 0;
        float totalBuildingTime = 0, totalResolutionTime = 0, averageNodePerSecond = 0;
        int count = 0; // For calculating averages

        String patternString = "Solutions: ([\\d,]+)\\s+"
                + "(?:MAXIMIZE .+?|MINIMIZE .+?)?\\s+"
                + "Building time : ([\\d.]+)s\\s+"
                + "Resolution time : ([\\d.,]+)s\\s+"
                + "(?:Time to best solution : [\\d.,]+s\\s+)?"
                + "Nodes: ([\\d,]+) \\(([\\d.,]+) n/s\\)\\s+"
                + "Backtracks: ([\\d,]+)\\s+"
                + "Backjumps: ([\\d,]+)\\s+"
                + "Fails: ([\\d,]+)\\s+"
                + "Restarts: (\\d+)";
        Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);

        for (String element : solverStats) {
            Matcher matcher = pattern.matcher(element);
            if (matcher.find()) {
                totalSolutions += Integer.parseInt(matcher.group(1).replace(",", ""));
                totalBuildingTime = parseFloatRemoveComma(matcher.group(2));
                totalResolutionTime += parseFloatRemoveComma(matcher.group(3));
                totalNodes += Integer.parseInt(matcher.group(4).replace(",", ""));
                averageNodePerSecond += parseFloatRemoveComma(matcher.group(5));
                totalBacktracks += Integer.parseInt(matcher.group(6).replace(",", ""));
                totalBackjumps += Integer.parseInt(matcher.group(7).replace(",", ""));
                totalFails += Integer.parseInt(matcher.group(8).replace(",", ""));
                totalRestarts += Integer.parseInt(matcher.group(9));
                count++;
            }
        }
        if (count > 0) {
            averageNodePerSecond /= count;
        }

        orderedMap.put("exhaustive", exhaustive);
        orderedMap.put("time(s)", totalTime);
        orderedMap.put("sum_solutions_resolution_time(s)", totalResolutionTime);
        orderedMap.put("sum_solutions_building_time(s)", totalBuildingTime);
        orderedMap.put("sum_number_solutions", totalSolutions);
        orderedMap.put("sum_solutions_nodes", totalNodes);
        orderedMap.put("average_node_per_second", averageNodePerSecond);
        orderedMap.put("sum_solutions_backtracks", totalBacktracks);
        orderedMap.put("sum_solutions_backjumps", totalBackjumps);
        orderedMap.put("sum_solutions_fails", totalFails);
        orderedMap.put("sum_solutions_restarts", totalRestarts);
    }

    private static float parseFloatRemoveComma(String value) {
        return Float.parseFloat(value.replace(",", ""));
    }
}


