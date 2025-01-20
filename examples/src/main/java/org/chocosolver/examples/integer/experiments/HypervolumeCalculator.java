package org.chocosolver.examples.integer.experiments;

import java.util.Arrays;
import java.util.Comparator;

public class HypervolumeCalculator {

    public static double calculateHypervolume(int[][] paretoFront, boolean maximize, double[][] bounds) {
        // Convert paretoFront to double[][]
        double[][] doubleParetoFront = convertIntToDouble(paretoFront);

        // Determine reference point
        double[] referencePoint = new double[]{maximize ? bounds[0][0] : bounds[0][1], maximize ? bounds[1][0] : bounds[1][1]};

        return calculateWithProcessedFront(doubleParetoFront, referencePoint, maximize);
    }

    public static double calculateNormalizedHypervolume(int[][] paretoFront, boolean maximize, double[][] bounds) {
        // Normalize Pareto front
        double[][] normalizedFront = normalizeParetoFront(paretoFront, bounds);

        // Determine reference point
        double[] normalizedReferencePoint = new double[]{maximize ? 0.0 : 1.0, maximize ? 0.0 : 1.0};

        return calculateWithProcessedFront(normalizedFront, normalizedReferencePoint, maximize);
    }

    private static double calculateWithProcessedFront(double[][] doubleParetoFront, double[] referencePoint, boolean maximize) {
        double[][] paretoCopy = doubleParetoFront.clone();
        if (maximize) {
            paretoCopy = negateDoubleArray(paretoCopy);
        }

        // Sort normalized Pareto front points by the first objective in descending order
        Arrays.sort(paretoCopy, Comparator.comparingDouble(a -> -a[0]));

        // calculate normal hypervolume
        double previousX = referencePoint[0];
        double hypervolume = 0.0;
        for (int i = 0; i < paretoCopy.length; i++) {
            double[] point = paretoCopy[i];
            if (point[1] < referencePoint[1]) {
                double width = previousX - point[0];
                double height = referencePoint[1] - point[1];
                hypervolume += width * height;
                previousX = point[0];
            }
        }
        return hypervolume;
    }

    private static double[][] normalizeParetoFront(int[][] paretoFront, double[][] bounds) {
        // bounds[i][1] is the max value for objective i
        // bounds[i][0] is the min value for objective i
        double[][] normalizedFront = new double[paretoFront.length][paretoFront[0].length];
        for (int i = 0; i < paretoFront.length; i++) {
            for (int j = 0; j < paretoFront[0].length; j++) {
                // Assuming bounds contains max value for each objective for normalization
                normalizedFront[i][j] = (paretoFront[i][j] - bounds[j][0]) / (bounds[j][1] - bounds[j][0]);
            }
        }
        return normalizedFront;
    }

    private static double[][] convertIntToDouble(int[][] intArray) {
        // Check if the input array is null
        if (intArray == null) {
            return null;
        }

        // Determine the dimensions of the input array
        int rows = intArray.length;
        double[][] doubleArray = new double[rows][];

        // Iterate through the array and convert int to double
        for (int i = 0; i < rows; i++) {
            int columns = intArray[i].length;
            doubleArray[i] = new double[columns];  // Initialize each row's double array
            for (int j = 0; j < columns; j++) {
                doubleArray[i][j] = intArray[i][j];  // Convert int to double
            }
        }

        return doubleArray;
    }

    private static double[][] negateDoubleArray(double[][] doubleArr) {
        // Check if the input array is null
        if (doubleArr == null) {
            return null;
        }

        // Determine the dimensions of the input array
        int rows = doubleArr.length;
        double[][] negatedArray = new double[rows][];

        // Iterate through the array and negate each int
        for (int i = 0; i < rows; i++) {
            int columns = doubleArr[i].length;
            negatedArray[i] = new double[columns];  // Initialize each row's int array
            for (int j = 0; j < columns; j++) {
                negatedArray[i][j] = -doubleArr[i][j];  // Negate and store int
            }
        }

        return negatedArray;
    }
}
