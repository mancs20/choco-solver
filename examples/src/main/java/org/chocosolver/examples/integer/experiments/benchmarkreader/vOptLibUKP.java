package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.examples.integer.experiments.Config;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class vOptLibUKP extends BenchamarkReader implements ProblemUKP {

    public vOptLibUKP(Config config) {
        super(config);
    }

    @Override
    public ModelObjectivesVariables createModel(int index) {
        // read the instance
        // Path to the document
        String filePath = config.getInstancePath();
        // directory path equal to the first part of the file path and file name equal to the second part, after the last '/'
//        String directoryPath = filePath.substring(0, filePath.lastIndexOf('/'));
//        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
//        File file = getFileFromFolder(directoryPath, fileName);

        // Variables for N, P, and K
        int numberOfObjects = 0, numberOfObjectives = 0, numberOfConstraints = 0;

        // Lists to store the values before converting to arrays
        List<List<Integer>> objectivesList = new ArrayList<>();
        List<List<Integer>> constraintList = new ArrayList<>();

        // Variable to store the final value
        int rhs = 0;
//        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<Integer> currentList = null;
            int lineCount = 0; // To track the number of lines read for objectives and constraints

            while ((line = br.readLine()) != null) {
                line = line.trim(); // Trim whitespace

                if (line.startsWith("#")) {
                    if (line.equals("# N")) {
                        numberOfObjects = Integer.parseInt(br.readLine().trim());
                    } else if (line.equals("# P")) {
                        numberOfObjectives = Integer.parseInt(br.readLine().trim());
                    } else if (line.equals("# K")) {
                        numberOfConstraints = Integer.parseInt(br.readLine().trim());
                    } else if (line.contains("# Objectif")) {
                        currentList = new ArrayList<>();
                        objectivesList.add(currentList);
                        lineCount = 0; // Reset line count for the new section
                    } else if (line.contains("# Contrainte")) {
                        currentList = new ArrayList<>();
                        constraintList.add(currentList);
                        lineCount = 0; // Reset line count for the new section
                    } else {
                        currentList = null; // For other lines starting with #, ignore
                    }
                } else if (!line.isEmpty()) {
                    if (currentList != null) {
                        // Add the value to the current list
                        currentList.add(Integer.parseInt(line));
                        lineCount++;
                        if (lineCount == numberOfObjects) {
                            currentList = null; // Stop adding to the list
                        }
                    }else{
                        rhs = Integer.parseInt(line);
                    }
                }
            }
            // verify that the number of objectives and constraints is equal to P and K
            if (objectivesList.size() != numberOfObjectives || constraintList.size() != numberOfConstraints) {
                throw new IllegalArgumentException("Wrong reading of: " + filePath + ". P=" + numberOfObjectives + " and K=" +
                        numberOfConstraints + " but found " + objectivesList.size() + " objectives and " +
                        constraintList.size() + " constraints");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid instance: " + filePath);
        }

        //convert to arrays
        int [][] objectivesWeights = new int[numberOfObjectives][numberOfObjects];
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
}
