package org.chocosolver.examples.integer.experiments;

import java.io.IOException;
import java.io.PrintWriter;

public class WriteTextFile {
    public static void writeToFile(String[] array, String frontGenerator, String modelName) throws IOException {
        String filename = frontGenerator + '_' + modelName + ".txt";
        try (PrintWriter writer = new PrintWriter(filename, "UTF-8")) {
            for (String element : array) {
                // Remove \n and \t
                String cleanedElement = element.replaceAll("[\\n\\t]", " ");
                writer.println(cleanedElement);
            }
        }
    }
}
