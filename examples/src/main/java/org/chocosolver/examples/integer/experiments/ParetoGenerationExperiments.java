package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.integer.experiments.benchmarkreader.ModelObjectivesVariables;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.IMultiObjectiveManager;
import org.chocosolver.solver.objective.mocoframework.MocoStrategy;
import org.chocosolver.solver.objective.mocoframework.StrategyComponents;
import org.chocosolver.solver.objective.mocoframework.StrategyFactory;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.PreprocessingStrategy;
import org.chocosolver.solver.objective.mocoframework.enums.*;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoSolutionDetails;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chocosolver.util.tools.TimeUtils;
import org.json.JSONObject;

/**
 * This class is used to run experiments to generate Pareto fronts for a given model
 * The parameters are passed as command-line arguments
 * @version choco-solver-5.0.0
 * Example usage: java ParetoGenerationExperiments <benchmark> <problem> <instancename> <instancePath> <solverSearchStrategy> <solverTimeoutSec> <frontGenerator> <threads>
 *     benchmark: is the benchmark name, e.g. "MOOLibrary" for "ukp" or "powa" for "NQUEENS" and when the instance files are ".fzn" files ("RCPSP" and "sims").
 *     problem: the name of the problem to be used. (e.g. "ukp" (multiobjective unidimensional knapsack), "NQUEENS", "RCPSP", "sims_cost_clouds", "sims_cost_clouds_angle")
 *     instance: the name of the instance to be used.
 *     instancePath: the path to the instance file
 *     solverSearchStrategy: the name of the solver search strategy to be used, for now it is "default" to represent "domOverWDegSearch"
 *     solverTimeoutSec: the maximum time in seconds that the solver will run
 *     frontGenerator: the name of the front generator to be used, it could be: "MOBAB-CP", "SAUGMECON", "PAUGMECON"
 *
 *     Examples:
 *     MOOLibrary ukp KP_p-4_n-30_ins-2 benchmarks/ukp/KP_p-4_n-30_ins-2.dat default 3600 MOBAB-CP
 *
 */

public class ParetoGenerationExperiments implements IMultiObjectiveManager {
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

        ModelObjectivesVariables[] modelAndObjectivesArray = new ModelObjectivesVariables[portfolioSize];
        ModelObjectivesVariables modelAndObjectives;

        BuilderModel builderModel = new BuilderModel(config);
        for (int i = 0; i < portfolioSize; i++) {
            if (solverSearchStrategy.equalsIgnoreCase("lcg")) {
                modelAndObjectivesArray[i] = builderModel.createModel(i, true);
            } else {
                modelAndObjectivesArray[i] = builderModel.createModel(i, false);
            }
        }
        modelAndObjectives = modelAndObjectivesArray[0];

        IntVar[] objectives = modelAndObjectives.getObjectives();
        Object[] decisionVariables = modelAndObjectives.getDecisionVariables();
        boolean maximization = modelAndObjectives.isMaximization();
        ParetoObjective[] originalObjectives = new ParetoObjective[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            originalObjectives[i] = new ParetoObjective(objectives[i].getLB(), objectives[i].getUB());
        }

        Object[] results;
        boolean exhaustive = true;
        Model model = modelAndObjectives.getModel();

        if (decisionVariables != null && decisionVariables.length > 0) {
            model.addHook("decisionVariables", decisionVariables);
        }
        model.addHook("objectives", objectives);
        IMultiObjectiveManager.setDefaultSearchMultiObjective(model, objectives, modelAndObjectives.getDecisionVariablesSearch(), solverSearchStrategy);
        results = runFrontStrategy(model, objectives, frontGenerator, maximization, config.getSolverTimeoutSec());
        if (model.getSolver().isStopCriterionMet()){
            System.out.println("Solver time limit reached");
            exhaustive = false;
        }

        boolean cumulativeStats = checkIfStatsAreCumulative(frontGenerator);

        outputProcessResults(results, exhaustive, objectives, originalObjectives, decisionVariables, maximization, cumulativeStats);

        // print ending experiment
        System.out.println("Ending experiment with benchmark:" + benchmark + ", problem:" + config.getProblem() +
                ", instance:" + config.getInstance() + ", frontGenerator:" + frontGenerator + ", solverSearchStrategy:" +
                solverSearchStrategy + ", solverTimeoutSec:" + solverTimeoutSec);
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
        return runWithElapsedTime(() -> {
            StrategyComponents components = getStrategyComponentsFromKeyword(frontGenerator);
            MocoStrategy strategy = new MocoStrategy(components);
            TimeCounter tc = new TimeCounter(model, 1000 * timeoutSec * TimeUtils.MILLISECONDS_IN_NANOSECONDS);
            ParetoSolutionDetails paretoSolutionDetails = strategy.execute(model, objectives, maximize, tc);
            return new Object[]{paretoSolutionDetails.getParetoFront(), paretoSolutionDetails.getSolverMeasures(), paretoSolutionDetails.isExhaustive()};
        });
    }

    private static StrategyComponents getStrategyComponentsFromKeyword(String keyword) {
        StrategyFactory strategyFactory = new StrategyFactory();
        ArrayList<PreprocessingStrategy> preprocessingStrategies = new ArrayList<>();
        switch (keyword) {
            case "MOBAB-CP":
                preprocessingStrategies.add(strategyFactory.getPreprocessing(PreprocessingType.GAVANELLI));
                return new StrategyComponents(
                        strategyFactory.getInitialRegion(InitialRegionType.ENTIRE_OBJECTIVE_SPACE),
                        preprocessingStrategies,
                        strategyFactory.getSelectRegion(SelectRegionType.SINGLE),
                        strategyFactory.getFindSolution(FindSolutionType.GENERIC),
                        strategyFactory.getUpdateRegion(UpdateRegionType.GAVANELLI)
                );
            case "SAUGMECON":
                preprocessingStrategies.add(strategyFactory.getPreprocessing(PreprocessingType.SAUGMECON));
                return new StrategyComponents(
                        strategyFactory.getInitialRegion(InitialRegionType.ENTIRE_OBJECTIVE_SPACE),
                        preprocessingStrategies,
                        strategyFactory.getSelectRegion(SelectRegionType.SINGLE),
                        strategyFactory.getFindSolution(FindSolutionType.SAUGMECON),
                        strategyFactory.getUpdateRegion(UpdateRegionType.SAUGMECON)
                );
            case "PAUGMECON":
                preprocessingStrategies.add(strategyFactory.getPreprocessing(PreprocessingType.ADD_INTERMEDIATE_SOLUTIONS));
                preprocessingStrategies.add(strategyFactory.getPreprocessing(PreprocessingType.DISABLE_PARETO_MAXIMIZER_AFTER_FIRST_SOLUTION));
                preprocessingStrategies.add(strategyFactory.getPreprocessing(PreprocessingType.SAUGMECON));
                return new StrategyComponents(
                        strategyFactory.getInitialRegion(InitialRegionType.ENTIRE_OBJECTIVE_SPACE),
                        preprocessingStrategies,
                        strategyFactory.getSelectRegion(SelectRegionType.SINGLE),
                        strategyFactory.getFindSolution(FindSolutionType.SAUGMECON_INTERMEDIATE),
                        strategyFactory.getUpdateRegion(UpdateRegionType.SAUGMECON_CONSTRAIN_MAIN_OBJECTIVE_UB)
                );
            // Add more cases here as needed
            default:
                if (keyword.startsWith("MocoFrameworkStrate-")) {
                    return StrategyParser.parse(keyword);
                }
                throw new IllegalArgumentException("Invalid front generator: " + keyword);
        }
    }

    private static boolean checkIfStatsAreCumulative(String frontGenerator){
        return frontGenerator.toLowerCase().contains("gavanelli");
    }

    @SuppressWarnings("unchecked")
    private static void outputProcessResults(Object[] results, boolean exhaustive, IntVar[] modelObjectives,
                                             ParetoObjective[] objectives, Object[] decisionVariables, boolean maximize, boolean cumulativeStats) {
        Map<String, Object> orderedMap = new LinkedHashMap<>();

        List<Solution> solutions = (List<Solution>) results[0];
        List<String> stats = (List<String>) results[1];
        float totalTime = (float) results[2];
        // check if results[3] exists
        List<Solution> allSolutions = null;
        if (results.length >= 4) {
            // check the type of results[3] if it is a list of solutions or a boolean
            if (results[3] instanceof List) {
                allSolutions = (List<Solution>) results[3];
                if (results.length == 5) {
                    exhaustive = (Boolean) results[4];
                }
            } else if (results[3] instanceof Boolean) {
                exhaustive = (Boolean) results[3];
            }
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
        orderedMap.put("subproblems_solved", solverMessages.length);

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
        long totalSolutions = 0, totalNodes = 0, totalBacktracks = 0, totalBackjumps = 0, totalFails = 0,
                totalRestarts = 0, totalPropagations = 0;
        double totalBuildingTime = 0, totalResolutionTime = 0, averageNodePerSecond = 0, paretoPropTime = 0;
        long count = 0; // For calculating averages

        if (cumulativeStats) {
            SolverStats stats = SolverStats.parse(solverStats[solverStats.length - 1]);
            if (stats != null) {
                totalSolutions = stats.getSolutions();
                totalBuildingTime = stats.getBuildingTime();
                totalResolutionTime = stats.getResolutionTime();
                paretoPropTime = stats.getParetoPropagationTime();
                totalNodes = stats.getNodes();
                averageNodePerSecond = stats.getNodePerSecond();
                totalBacktracks = stats.getBacktracks();
                totalBackjumps = stats.getBackjumps();
                totalFails = stats.getFails();
                totalRestarts = stats.getRestarts();
                totalPropagations = stats.getPropagations();
            }
        } else{
            for (String element : solverStats) {
                SolverStats stats = SolverStats.parse(element);
                if (stats != null) {
                    totalSolutions += stats.getSolutions();
                    totalBuildingTime = stats.getBuildingTime(); // Assuming building time is overwritten
                    totalResolutionTime += stats.getResolutionTime();
                    paretoPropTime += stats.getParetoPropagationTime();
                    totalNodes += stats.getNodes();
                    averageNodePerSecond += stats.getNodePerSecond();
                    totalBacktracks += stats.getBacktracks();
                    totalBackjumps += stats.getBackjumps();
                    totalFails += stats.getFails();
                    totalRestarts += stats.getRestarts();
                    totalPropagations += stats.getPropagations();
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
        orderedMap.put("pareto_propagation_time(s)", paretoPropTime);
        orderedMap.put("sum_solutions_building_time(s)", totalBuildingTime);
        orderedMap.put("sum_number_solutions", totalSolutions);
        orderedMap.put("sum_solutions_nodes", totalNodes);
        orderedMap.put("average_node_per_second", averageNodePerSecond);
        orderedMap.put("sum_solutions_backtracks", totalBacktracks);
        orderedMap.put("sum_solutions_backjumps", totalBackjumps);
        orderedMap.put("sum_solutions_fails", totalFails);
        orderedMap.put("sum_solutions_restarts", totalRestarts);
        orderedMap.put("sum_solutions_propagations", totalPropagations);
    }
}

class SolverStats {
    private final long solutions;
    private final double buildingTime;
    private final double resolutionTime;
    private final double paretoPropTime;
    private final long nodes;
    private final double nodePerSecond;
    private final long backtracks;
    private final long backjumps;
    private final long fails;
    private final long restarts;
    private final long propagations;

    private static final Pattern PATTERN = Pattern.compile(
            "Solutions: ([\\d,]+)\\s+"
            + "(?:MAXIMIZE .+?|MINIMIZE .+?)?\\s+"
            + "(?:.*?\\s+)?"
            + "Building time : ([\\d.]+)s\\s+"
            + "Resolution time : ([\\d.,]+)s\\s+"
            + "(?:Time to best solution : [\\d.,]+s\\s+)?"
            + "Nodes: ([\\d,]+) \\(([\\d.,]+) n/s\\)\\s+"
            + "Backtracks: ([\\d,]+)\\s+"
            + "Backjumps: ([\\d,]+)\\s+"
            + "Fails: ([\\d,]+)\\s+"
            + "Restarts: ([\\d,]+)\\s+"
            + "Propagations: ([\\d,]+)"
            + "(?:\\s+Time Pareto prop\\s*:\\s*([\\d.,]+)s\\s*)?"
            , Pattern.DOTALL);


    // Constructor
    private SolverStats(long solutions, double buildingTime, double resolutionTime, long nodes, double nodePerSecond,
                        long backtracks, long backjumps, long fails, long restarts, long propagations, double paretoPropTime) {
        this.solutions = solutions;
        this.buildingTime = buildingTime;
        this.resolutionTime = resolutionTime;
        this.paretoPropTime = paretoPropTime;
        this.nodes = nodes;
        this.nodePerSecond = nodePerSecond;
        this.backtracks = backtracks;
        this.backjumps = backjumps;
        this.fails = fails;
        this.restarts = restarts;
        this.propagations = propagations;
    }

    // Factory method for parsing
    public static SolverStats parse(String statString) {
        Matcher matcher = PATTERN.matcher(statString);
        if (matcher.find()) {
            String paretoPropStr = matcher.group(11);
            double paretoProp = paretoPropStr != null ? parseFloatRemoveComma(paretoPropStr) : 0.0;
            return new SolverStats(
                    Long.parseLong(matcher.group(1).replace(",", "")),
                    parseFloatRemoveComma(matcher.group(2)),
                    parseFloatRemoveComma(matcher.group(3)),
                    Long.parseLong(matcher.group(4).replace(",", "")),
                    parseFloatRemoveComma(matcher.group(5)),
                    Long.parseLong(matcher.group(6).replace(",", "")),
                    Long.parseLong(matcher.group(7).replace(",", "")),
                    Long.parseLong(matcher.group(8).replace(",", "")),
                    Long.parseLong(matcher.group(9).replace(",", "")),
                    Long.parseLong(matcher.group(10).replace(",", "")),
                    paretoProp // This will be null if the group is absent
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
    public double getParetoPropagationTime() { return paretoPropTime; } // Placeholder if needed
    public long getNodes() { return nodes; }
    public double getNodePerSecond() { return nodePerSecond; }
    public long getBacktracks() { return backtracks; }
    public long getBackjumps() { return backjumps; }
    public long getFails() { return fails; }
    public long getRestarts() { return restarts; }
    public long getPropagations() { return propagations; }
}



