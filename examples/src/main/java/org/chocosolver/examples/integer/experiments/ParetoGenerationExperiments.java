package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.integer.Pareto;
import org.chocosolver.examples.integer.ParetoGIA;
import org.chocosolver.examples.integer.ParetoSaugmecon;
import org.chocosolver.examples.integer.experiments.benchmarkreader.ModelObjectivesVariables;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.GiaConfig;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.function.Supplier;
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
 *     solverSearchStrategy: the name of the solver search strategy to be used, for now it is "default", but it could be "LNS"
 *     solverTimeoutSec: the maximum time in seconds that the solver will run
 *     frontGenerator: the name of the front generator to be used, it could be "GIA" (versions: GIA, GIA_bounded, GIA_boundedLazy, BiObjGIA_sparsity, BiObjGIA_sparsityBounded, BiObjGIA_regionImplementation),
 *     "ParetoGavanelliGlobalConstraint", "Saugmecon", "SaugmeconFrontVerify", "ParetoDisjunctiveProgramming"
 *     threads: the number of threads to be used, the default is 1
 *
 *     Examples:
 *     vOptLib ukp 2KP50-1A /Users/manuel.combarrosimon/choco-solver/bench/2KP50-1A.dat portfolio 14400 BiObjGIA_regionImplementation 6
 *     vOptLib ukp 2KP50-1A ../vOptLib/ukp/2KP50-1A.dat default 14400 BiObjGIA_regionImplementation
 *     augmecon2 ukp 100u /Users/manuel.combarrosimon/choco-solver/bench/2KP50-1A.dat default 3600 BiObjGIA_regionImplementation
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
        boolean cumulativeStats = checkIfStatsAreCumulative(frontGenerator);

        outputProcessResults(results, exhaustive, objectives, originalObjectives, decisionVariables, maximization, cumulativeStats);

        // print ending experiment
        System.out.println("Ending experiment with benchmark:" + benchmark + ", problem:" + config.getProblem() +
                ", instance:" + config.getInstance() + ", frontGenerator:" + frontGenerator + ", solverSearchStrategy:" +
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

    private static Object[] runWithElapsedTime(Supplier<Object[]> action) {
        long startTime = System.nanoTime();
        Object[] result = action.get();
        long endTime = System.nanoTime();
        float elapsedTime = (float) (endTime - startTime) / 1_000_000_000;

        // Ensure elapsedTime is added at index 2
        Object[] finalResult = new Object[result.length + 1];
        System.arraycopy(result, 0, finalResult, 0, 2); // Copy first two elements
        finalResult[2] = elapsedTime;                 // Add elapsedTime at index 2
        System.arraycopy(result, 2, finalResult, 3, result.length - 2); // Copy the rest

        return finalResult;
    }

    private static Object[] runFrontStrategy(Model model, IntVar[] objectives, String frontGenerator, boolean maximize, int timeoutSec) {
        model.getSolver().limitTime(timeoutSec + "s");
        return runWithElapsedTime(() -> {
            if (frontGenerator.contains("GIA")) {
                GiaConfig giaConfig = createGiaConfig(frontGenerator);
                ParetoGIA paretoGIA = ParetoGIAFactory.createSolver(giaConfig, timeoutSec);
                if (frontGenerator.contains("regionImplementation"))
                    return paretoGIA.runOriginal(model, objectives, maximize, timeoutSec);
                return paretoGIA.run(maximize, model, objectives);
            }
            switch (frontGenerator) {
                case "ParetoDisjunctiveProgrammingRegSelImproAll":
                    ParetoDisjunctiveProgramming paretoDisjunctiveProgrammingRegSelImprAll = new ParetoDisjunctiveProgramming();
                    return paretoDisjunctiveProgrammingRegSelImprAll.run(model, objectives, maximize, timeoutSec, false);
                case "ParetoDisjunctiveProgramming":
                    ParetoDisjunctiveProgramming paretoDisjunctiveProgramming = new ParetoDisjunctiveProgramming();
                    return paretoDisjunctiveProgramming.run(model, objectives, maximize, timeoutSec, true);
                case "ParetoGavanelliGlobalConstraintNoEvolutionInfo":
                    Pareto pareto = new Pareto();
                    return pareto.run(model, objectives, maximize);
                case "ParetoGavanelliGlobalConstraint":
                    ParetoGavanelliFrontEvolutionInfo paretoGavanelliFrontEvolutionInfo = new ParetoGavanelliFrontEvolutionInfo();
                    return paretoGavanelliFrontEvolutionInfo.run(model, objectives, maximize);
                case "Saugmecon":
                    ParetoSaugmecon paretoSaugmeconLex = new ParetoSaugmecon(true);
                    return paretoSaugmeconLex.run(model, objectives, maximize, timeoutSec);
                case "SaugmeconFrontVerify":
                    ParetoSaugmecon paretoSaugmecon = new ParetoSaugmecon(false);
                    return paretoSaugmecon.run(model, objectives, maximize, timeoutSec);
                default:
                    throw new IllegalArgumentException("Invalid front generator: " + frontGenerator);
            }
        });
    }

    private static Object[] runFrontStrategyWithPortfolio(ModelObjectivesVariables[] modelObjectivesVariables, String frontGenerator, boolean maximize, int timeoutSec) {
        // todo the implementation of the portfolio is not finished, needs to be tested
        System.out.println("Running portfolio is not implemented yet. Exiting.");
        System.exit(1);

        //        model.getSolver().limitTime(timeoutSec + "s");
        if (frontGenerator.contains("GIA")){
            GiaConfig giaConfig = createGiaConfig(frontGenerator);
            ParetoGIA paretoGIA = ParetoGIAFactory.createSolver(giaConfig, timeoutSec);
            return paretoGIA.runWithPortfolio(modelObjectivesVariables, maximize, timeoutSec);
        }else{
            throw new IllegalArgumentException("Invalid front generator: " + frontGenerator);
        }
    }

    private static GiaConfig createGiaConfig(String frontGenerator){
        GiaConfig giaConfig = new GiaConfig();
        switch (frontGenerator) {
            case "GIA":
                break;
            case "GIA_bounded":
                giaConfig.setBounded(GiaConfig.BoundedType.DOMINATING_DOMINATES);
                break;
            case "GIA_boundedLazy":
                giaConfig.setBounded(GiaConfig.BoundedType.LAZY_DOMINATING_DOMINATES);
                break;
            case "BiObjGIA_sparsity":
                giaConfig.setCriteriaSelection(GiaConfig.CriteriaSelection.SPARSITY);
                break;
            case "BiObjGIA_sparsityBounded":
                giaConfig.setCriteriaSelection(GiaConfig.CriteriaSelection.SPARSITY);
                giaConfig.setBounded(GiaConfig.BoundedType.DOMINATING_DOMINATES);
                break;
            case "BiObjGIA_regionImplementation":
                // todo this name has to be replaced, rigth now it acts as bi_objGIA_sparsity_bounded
                giaConfig.setCriteriaSelection(GiaConfig.CriteriaSelection.SPARSITY);
                giaConfig.setBounded(GiaConfig.BoundedType.DOMINATING_DOMINATES);
                break;
            default:
                throw new UnsupportedOperationException("Front generator not supported: " + frontGenerator);
        }
        return giaConfig;
    }

    private static boolean checkIfStatsAreCumulative(String frontGenerator){
        return frontGenerator.toLowerCase().contains("gavanelli");
    }

    private static void outputProcessResults(Object[] results, boolean exhaustive, IntVar[] modelObjectives,
                                             ParetoObjective[] objectives, Object[] decisionVariables, boolean maximize, boolean cumulativeStats) {
        Map<String, Object> orderedMap = new LinkedHashMap<>();

        List<Solution> solutions = (List<Solution>) results[0];
        List<String> stats = (List<String>) results[1];
        float totalTime = (float) results[2];
        // check if results[3] exists
        List<Solution> allSolutions = null;
        if (results.length == 4) {
            allSolutions = (List<Solution>) results[3];
        }

        String[] solverMessages = new String[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            // Remove \n and \t
            String cleanedElement = stats.get(i).replaceAll("[\\n\\t]", " ");
            solverMessages[i] = "Model solved # " + (i+1) + " " + cleanedElement;
        }
        orderedMap.put("solver_messages", solverMessages);
        try {
            addTotalSolverStatsToJsonObject(orderedMap, stats.toArray(new String[0]), totalTime, exhaustive, cumulativeStats);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error while adding total solver stats to json object");
        }

        // Get the hypervolume, the pareto front and the solutions of the problem
        ParetoFrontProcessor paretoFront = new ParetoFrontProcessor(solutions.toArray(new Solution[0]),
                modelObjectives, objectives, decisionVariables, maximize);

        // Print Pareto stats
        orderedMap.put("pareto_front", paretoFront.getParetoFront());
        try {
            orderedMap.put("solutions_pareto_front", paretoFront.getSolutionsParetoFront());
            if (allSolutions != null) {
                orderedMap.put("all_solutions", ParetoFrontProcessor.getParetoFrontFromSolutions(allSolutions.toArray(new Solution[0]), modelObjectives));
            }else{
                orderedMap.put("all_solutions", paretoFront.getParetoFront());
            }
            double[][] bounds = paretoFront.getBounds();
            double[] referencePoint = new double[bounds.length];
            int referenceBound = maximize ? 0 : 1;
            for (int i = 0; i < referencePoint.length; i++) {
                referencePoint[i] = bounds[i][referenceBound];
            }
            orderedMap.put("reference_point", referencePoint);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error while adding pareto front stats to json object");
        }

        try{
            JSONObject jsonObject = new JSONObject(orderedMap);
            JSONObject type = new JSONObject();
            type.put("type", "solutions-details");
            type.put("solutions-details", jsonObject);

            // Print the JSON object
            System.out.println(type);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error while creating JSON object");
            System.out.print("{\"solutions-details\":{\"solvers_messages\":[");
            for (int i = 0; i < solverMessages.length - 1; i++) {
                System.out.print("\""+solverMessages[i] + "\",");
            }
            System.out.print("\""+solverMessages[solverMessages.length-1]);
            System.out.print("\"],\"pareto_front\":[");
            for (int i = 0; i < paretoFront.getParetoFront().length -1; i++) {
                System.out.print(Arrays.toString(paretoFront.getParetoFront()[i]) + ",");
            }
            System.out.print(Arrays.toString(paretoFront.getParetoFront()[paretoFront.getParetoFront().length -1]) + "]");
            String exhaustiveResult = exhaustive ? "true" : "false";
            System.out.print(",\"exhaustive\":" + exhaustiveResult);
            System.out.println("},\"type\":\"solutions-details\"}");
        }

        if (exhaustive) {
            System.out.println("There are " + solutions.size() + " Pareto-optimal points. Founded in " + totalTime + "s.");
        } else{
            System.out.println("There are " + solutions.size() + " points in the approximation front (the algortihm was stopped before finishing). Founded in " + totalTime + "s.");
        }
    }

    private static void addTotalSolverStatsToJsonObject(Map<String, Object> orderedMap, String[] solverStats, float totalTime, boolean exhaustive, boolean cumulativeStats) {
        // Initialize sums
        long totalSolutions = 0, totalNodes = 0, totalBacktracks = 0, totalBackjumps = 0, totalFails = 0, totalRestarts = 0;
        double totalBuildingTime = 0, totalResolutionTime = 0, averageNodePerSecond = 0;
        long count = 0; // For calculating averages

        if (cumulativeStats) {
            SolverStats stats = SolverStats.parse(solverStats[solverStats.length - 1]);
            if (stats != null) {
                totalSolutions = stats.getSolutions();
                totalBuildingTime = stats.getBuildingTime();
                totalResolutionTime = stats.getResolutionTime();
                totalNodes = stats.getNodes();
                averageNodePerSecond = stats.getNodePerSecond();
                totalBacktracks = stats.getBacktracks();
                totalBackjumps = stats.getBackjumps();
                totalFails = stats.getFails();
                totalRestarts = stats.getRestarts();
            }
        } else{
            for (String element : solverStats) {
                SolverStats stats = SolverStats.parse(element);
                if (stats != null) {
                    totalSolutions += stats.getSolutions();
                    totalBuildingTime = stats.getBuildingTime(); // Assuming building time is overwritten
                    totalResolutionTime += stats.getResolutionTime();
                    totalNodes += stats.getNodes();
                    averageNodePerSecond += stats.getNodePerSecond();
                    totalBacktracks += stats.getBacktracks();
                    totalBackjumps += stats.getBackjumps();
                    totalFails += stats.getFails();
                    totalRestarts += stats.getRestarts();
                    count++;
                }
            }
            if (count > 0) {
                averageNodePerSecond /= count;
            }
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
}

class SolverStats {
    private final long solutions;
    private final double buildingTime;
    private final double resolutionTime;
    private final long nodes;
    private final double nodePerSecond;
    private final long backtracks;
    private final long backjumps;
    private final long fails;
    private final long restarts;

    private static final Pattern PATTERN = Pattern.compile(
            "Solutions: ([\\d,]+)\\s+"
            + "(?:MAXIMIZE .+?|MINIMIZE .+?)?\\s+"
            + "Building time : ([\\d.]+)s\\s+"
            + "Resolution time : ([\\d.,]+)s\\s+"
            + "(?:Time to best solution : [\\d.,]+s\\s+)?"
            + "Nodes: ([\\d,]+) \\(([\\d.,]+) n/s\\)\\s+"
            + "Backtracks: ([\\d,]+)\\s+"
            + "Backjumps: ([\\d,]+)\\s+"
            + "Fails: ([\\d,]+)\\s+"
            + "Restarts: (\\d+)", Pattern.DOTALL);

    // Constructor
    private SolverStats(long solutions, double buildingTime, double resolutionTime, long nodes, double nodePerSecond,
                        long backtracks, long backjumps, long fails, long restarts) {
        this.solutions = solutions;
        this.buildingTime = buildingTime;
        this.resolutionTime = resolutionTime;
        this.nodes = nodes;
        this.nodePerSecond = nodePerSecond;
        this.backtracks = backtracks;
        this.backjumps = backjumps;
        this.fails = fails;
        this.restarts = restarts;
    }

    // Factory method for parsing
    public static SolverStats parse(String statString) {
        Matcher matcher = PATTERN.matcher(statString);
        if (matcher.find()) {
            return new SolverStats(
                    Long.parseLong(matcher.group(1).replace(",", "")),
                    parseFloatRemoveComma(matcher.group(2)),
                    parseFloatRemoveComma(matcher.group(3)),
                    Long.parseLong(matcher.group(4).replace(",", "")),
                    parseFloatRemoveComma(matcher.group(5)),
                    Long.parseLong(matcher.group(6).replace(",", "")),
                    Long.parseLong(matcher.group(7).replace(",", "")),
                    Long.parseLong(matcher.group(8).replace(",", "")),
                    Long.parseLong(matcher.group(9))
            );
        }
        return null;
    }

    private static double parseFloatRemoveComma(String value) {
        return Double.parseDouble(value.replace(",", ""));
    }

    // Getters
    public long getSolutions() { return solutions; }
    public double getBuildingTime() { return buildingTime; }
    public double getResolutionTime() { return resolutionTime; }
    public long getNodes() { return nodes; }
    public double getNodePerSecond() { return nodePerSecond; }
    public long getBacktracks() { return backtracks; }
    public long getBackjumps() { return backjumps; }
    public long getFails() { return fails; }
    public long getRestarts() { return restarts; }
}



