package org.chocosolver.examples.integer.experiments;

import org.chocosolver.examples.integer.experiments.benchmarkreader.BenchamarkReader;
import org.chocosolver.examples.integer.experiments.benchmarkreader.ModelObjectivesVariables;
import org.chocosolver.examples.integer.experiments.benchmarkreader.ProblemUKP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MOOLibraryUKP extends BenchamarkReader implements ProblemUKP {

    public MOOLibraryUKP(Config config) {
        super(config);
    }

    @Override
    public ModelObjectivesVariables createModel(int index) {
        // read the instance
        // Path to the document
        String filePath = config.getInstancePath();
        // directory path equal to the first part of the file path and file name equal to the second part, after the last '/'

        int numberOfObjects, numberOfObjectives;
        List<List<Integer>> objectivesList;
        List<List<Integer>> constraintList;
        int rhs;

        Object[] values = readFile(filePath);
        numberOfObjectives = (int) values[0];

        if (numberOfObjectives > 2) {
            config.setProblem(config.getProblem() + "_" + numberOfObjectives);
        }

        numberOfObjects = (int) values[1];
        rhs = (int) values[2];
        objectivesList = (List<List<Integer>>) values[3];
        constraintList = (List<List<Integer>>) values[4];

        //convert to arrays
        int[][] objectivesWeights = new int[numberOfObjectives][numberOfObjects];
        for (int i = 0; i < numberOfObjectives; i++) {
            List<Integer> currentList = objectivesList.get(i);
            for (int j = 0; j < numberOfObjects; j++) {
                objectivesWeights[i][j] = currentList.get(j);
            }
        }
        int[] constraintsWeights = new int[numberOfObjects];
        List<Integer> currentList = constraintList.get(0);
        for (int i = 0; i < numberOfObjects; i++) {
            constraintsWeights[i] = currentList.get(i);
        }

        return implementKnapsackModel(getModelName(index), objectivesWeights, constraintsWeights, rhs);
    }

    public static Object[] readFile(String filePath) {
        int numberOfObjectives;
        int numberOfObjects;
        int rhs;
        List<List<Integer>> objectivesList = new ArrayList<>();
        List<List<Integer>> constraintList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            numberOfObjectives = Integer.parseInt(br.readLine().trim());
            numberOfObjects = Integer.parseInt(br.readLine().trim());
            rhs = Integer.parseInt(br.readLine().trim());

            // Read the profits matrix
            for (int i = 0; i < numberOfObjectives; i++) {
                String line = br.readLine().trim();
                // Remove brackets and split by commas
                line = line.replace("[", "").replace("]", "");
                String[] tokens = line.split(",");
                List<Integer> row = new ArrayList<>();
                for (String token : tokens) {
                    row.add(Integer.parseInt(token.trim()));
                }
                objectivesList.add(row);
            }

            // Read the weights of objects
            String weightsLine = br.readLine().trim();
            weightsLine = weightsLine.replace("[", "").replace("]", "");
            String[] weightTokens = weightsLine.split(",");
            List<Integer> rowConstraint = new ArrayList<>();
            for (String token : weightTokens) {
                rowConstraint.add(Integer.parseInt(token.trim()));
            }

            constraintList.add(rowConstraint);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid instance: " + filePath);
        }

        return new Object[]{numberOfObjectives, numberOfObjects, rhs, objectivesList, constraintList};
    }
}
