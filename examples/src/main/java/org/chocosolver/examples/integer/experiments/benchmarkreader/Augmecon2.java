package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.examples.integer.experiments.Config;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Augmecon2 extends BenchamarkReader implements ProblemUKP {
    public Augmecon2(Config config) {
        super(config);
    }

    @Override
    public ModelObjectivesVariables createModel(int index) {
        // todo this model is only considering one constraint dimension, like the classical knapsack where
        //  the weight is the only constraint. In augmenc2 there are two constraints, but choco only supports one. Fix
        //  this later

        // get the last string after '-' from the modelNameResourcePath
        String instance = config.getInstance();
        // if the last letter is not a number remove it
        if (!Character.isDigit(instance.charAt(instance.length() - 1))) {
            instance = instance.substring(0, instance.length() - 1);
        }

        String lhsDataName = "z2_" + instance + "att.txt";
        String rhsDataName = "z2_" + instance + "btt.txt";
        String objectivesDataName = "z2_" + instance + "ctt.txt";
        // Read the data from the resources
        List<int[]> lhsData = readFromResources(lhsDataName);
        lhsData.remove(0); // remove the first line (header)
        List<int[]> rhsData = readFromResources(rhsDataName);
        List<int[]> objectivesData = readFromResources(objectivesDataName);
        objectivesData.remove(0); // remove the first line (header)

        // todo when the implementation for multidimensional knapsack is done, the following code for the weight
        //  constraint should be changed
        int augmeconDimension = 1;
        int weightConstraintUB = rhsData.get(augmeconDimension - 1)[1];
        int[] weightsLHS = new int[lhsData.size()];
        for (int i = 0; i < lhsData.size(); i++) {
            weightsLHS[i] = lhsData.get(i)[augmeconDimension];
        }
        int[][] objectivesDataArray = new int[objectivesData.get(0).length - 1][objectivesData.size()];
        for (int i = 0; i < objectivesData.size(); i++) {
            for (int j = 1; j < objectivesData.get(0).length; j++) {
                objectivesDataArray[j - 1][i] = objectivesData.get(i)[j];
            }
        }

        return implementKnapsackModel(getModelName(index), objectivesDataArray, weightsLHS, weightConstraintUB);
    }

    private List<int[]> readFromResources(String fileName) {
        // Include the subdirectory in the resource path
        String filePath = config.getInstancePath() + "/" + fileName;

        List<int[]> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Process each line into an int array and add it to the list
                int[] numbers = processLineToIntArray(line);
                data.add(numbers);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return data;
    }

    private int[] processLineToIntArray(String line) {
        // Split the line on spaces
        String[] parts = line.trim().split("\\s+");
        int[] intArray = new int[parts.length];

        // Convert each part to an integer, truncating decimals
        for (int i = 0; i < parts.length; i++) {
            try {
                double value = Double.parseDouble(parts[i]);
                intArray[i] = (int) value;
            } catch (NumberFormatException e) {
                System.out.println("Error parsing number: " + parts[i] + " in line: " + line);
                intArray[i] = 0; // Provide a default value or handle it as needed
            }
        }
        return intArray;
    }
}
