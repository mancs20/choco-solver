package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.examples.integer.experiments.Config;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class NQueenReader extends BenchamarkReader{

    public NQueenReader(Config config) {
        super(config);
    }

    @Override
    public ModelObjectivesVariables createModel(int index) {
        String filePath = config.getInstancePath();

        Object[] values = readFile(filePath);
        int n = (int) values[0];
        int[][][] objectives_score = (int[][][]) values[1];

        return implementNQueenModel(getModelName(index), n, objectives_score);
    }

    public static Object[] readFile(String filePath) {
        int numberOfObjectives;
        int n;
        int[][][] objectives_score;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            numberOfObjectives = Integer.parseInt(br.readLine().trim());
            n = Integer.parseInt(br.readLine().trim());
            objectives_score = new int[numberOfObjectives][n][n];
            br.readLine();  // Skip empty line

            // Read matrices
            for (int i = 0; i < numberOfObjectives; i++) {
                for (int j = 0; j < n; j++) {
                    String line = br.readLine().trim();
                    String[] parts = line.split(" ");
                    for (int k = 0; k < n; k++) {
                        objectives_score[i][j][k] = Integer.parseInt(parts[k]);
                    }
                }
                br.readLine();  // Skip empty line between matrices
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid instance: " + filePath);
        }

        return new Object[]{n, objectives_score};
    }

    public ModelObjectivesVariables implementNQueenModel(String modelName, int n, int[][][] objs){
        model = new Model(modelName);
        IntVar[] vars = new IntVar[n];
        IntVar[] diag1 = new IntVar[n];
        IntVar[] diag2 = new IntVar[n];

        for (int i = 0; i < n; i++) {
            vars[i] = model.intVar("Q_" + i, 0, n-1, false);
            diag1[i] = model.offset(vars[i], i);
            diag2[i] = model.offset(vars[i], -i);
        }

        model.allDifferent(vars, "BC").post();
        model.allDifferent(diag1, "BC").post();
        model.allDifferent(diag2, "BC").post();

        // Add the objectives
        IntVar[] objectives = new IntVar[objs.length];
        for (int i = 0; i < objs.length; i++) {
            int[][] matrix = objs[i];

            // Contribution of each row to the objective
            IntVar[] objRow = new IntVar[n];
            for (int j = 0; j < n; j++) {
                objRow[j] = model.intVar("obj_" + i + "objRow_" + j, matrix[j]); // Can take any value from row matrix[i]
                model.element(objRow[j], matrix[j], vars[j]).post(); // objRow[i] = matrix[i][vars[i]]
            }

            int[] lbUB = calculateMaxMinSum(matrix);
            objectives[i] = model.intVar("obj_" + i, lbUB[0], lbUB[1]);
            model.sum(objRow, "=", objectives[i]).post(); // obj_i = sum(objRow[i])
        }

        return new ModelObjectivesVariables(model, objectives, vars, true);
    }

    private static int[] calculateMaxMinSum(int[][] matrix) {
        int minSum = 0;
        int maxSum = 0;

        for (int[] row : matrix) {
            int rowMin = Integer.MAX_VALUE;
            int rowMax = Integer.MIN_VALUE;

            for (int value : row) {
                if (value < rowMin) rowMin = value;
                if (value > rowMax) rowMax = value;
            }
            minSum += rowMin;
            maxSum += rowMax;
        }
        return new int[]{minSum, maxSum};  // Return both sums
    }
}
