package org.chocosolver.examples.integer.experiments;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SummaryToCSV {
    public static void writeToCSV(String[] solverStats, float totalTime, Config config, boolean exhaustive,
                                  double normalizedHypervolume, double hypervolume, int[][] paretoFront,
                                  int[][] solutionsParetoFront) throws IOException {
        // get paretoFront and solutionsParetoFront


        // Initialize sums
        int totalSolutions = 0, totalNodes = 0, totalBacktracks = 0, totalBackjumps = 0, totalFails = 0, totalRestarts = 0;
        float totalBuildingTime = 0, totalResolutionTime = 0, averageNodePerSecond = 0;
        int count = 0; // For calculating averages

        String patternString = "Solutions: (\\d+).*Building time : ([\\d.]+)s.*Resolution time : ([\\d.]+)s.*Nodes: ([\\d,]+) \\(([\\d,.]+) n/s\\).*Backtracks: ([\\d,]+).*Backjumps: ([\\d,]+).*Fails: ([\\d,]+).*Restarts: (\\d+)";
        Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);

        for (String element : solverStats) {
            Matcher matcher = pattern.matcher(element);
            if (matcher.find()) {
                totalSolutions += Integer.parseInt(matcher.group(1).replace(",", ""));
                totalBuildingTime = parseFloatRemoveComa(matcher.group(2));
                totalResolutionTime += parseFloatRemoveComa(matcher.group(3));
                totalNodes += Integer.parseInt(matcher.group(4).replace(",", ""));
                averageNodePerSecond += parseFloatRemoveComa(matcher.group(5));
                totalBacktracks += Integer.parseInt(matcher.group(6).replace(",", ""));
                totalBackjumps += Integer.parseInt(matcher.group(7).replace(",", ""));
                totalFails += Integer.parseInt(matcher.group(8).replace(",", ""));
                totalRestarts += Integer.parseInt(matcher.group(9).replace(",", ""));
                count++;
            }
        }
        if (count > 0) {
            averageNodePerSecond /= count;
        }


        String csvFilename =  "results.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilename, true))) {
            // Simplified header without threads and cores
            if (new File(csvFilename).length() == 0) { // Check if file is new or empty to write header
                writer.println("modelName;frontGenerator;solver_search_strategy;solver_timeout_sec;exhaustive;" +
                        "normalizedHypervolume;hypervolume;datetime;totalTime(s);totalSolutions;totalNodes;averageNodePerSecond;" +
                        "totalBuildingTime(s);totalResolutionTime(s);totalBacktracks;totalBackjumps;totalFails;" +
                        "totalRestarts;numberOfPointsPareto;pareto_front;solutions_pareto_front");
            }

            // Write data using config object
            writer.printf("%s;%s;%s;%d;%b;%f;%f;%s;%f;%d;%d;%f;%f;%f;%d;%d;%d;%d;%d;%s;%s%n",
                    config.getModelName(), config.getFrontGenerator(), config.getSolverSearchStrategy(), config.getSolverTimeoutSec(),
                    exhaustive, normalizedHypervolume, hypervolume, config.getDateTime(), totalTime, totalSolutions, totalNodes, averageNodePerSecond,
                    totalBuildingTime, totalResolutionTime, totalBacktracks, totalBackjumps, totalFails, totalRestarts,
                    paretoFront.length, arrayToString(paretoFront), arrayToString(solutionsParetoFront));
        }
    }

    public static float parseFloatRemoveComa(String s) {
        String cleanNumberStr = s.replace(",", ""); // Remove commas
        return Float.parseFloat(cleanNumberStr); // Convert to float
    }

    private static String arrayToString(int[][] array) {
        return "{" + Arrays.stream(array)
                .map(innerArray -> "[" + Arrays.stream(innerArray)
                        .mapToObj(String::valueOf)  // Converts integers to strings
                        .collect(Collectors.joining(",")) + "]")  // Joins numbers in each inner array with commas
                .collect(Collectors.joining(",")) + "}";  // Joins all inner arrays with commas and encloses in curly braces
    }
}
