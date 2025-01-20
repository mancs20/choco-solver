package org.chocosolver.solver.search;

import java.util.ArrayList;
import java.util.List;

public class ParetoFeasibleRegion {
    final private int[] lowerCorner;
    final private int[] upperCorner;
    final private List<int[]> efficientCorners;

    // Constructor
    public ParetoFeasibleRegion(int[] lowerCorner, int[] upperCorner) {
        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
        this.efficientCorners = new ArrayList<>();
    }

    public ParetoFeasibleRegion(int[] lowerCorner, int[] upperCorner, List<int[]> efficientCorners) {
        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
        this.efficientCorners = efficientCorners != null ? efficientCorners : new ArrayList<>();
    }

    // Method to calculate nVolume
    public int nVolume() {
        int nVolume = 1;
        for (int i = 0; i < getLowerCorner().length; i++) {
            nVolume *= (getUpperCorner()[i] - getLowerCorner()[i]);
        }
        return nVolume;
    }

    public int[] getLowerCorner() {
        return lowerCorner;
    }

    public int[] getUpperCorner() {
        return upperCorner;
    }

    public List<int[]> getEfficientCorners() {
        return efficientCorners;
    }

    public double getRegionVolume(){
        double volume = 1;
        for (int i = 0; i < lowerCorner.length; i++) {
            volume = volume * (upperCorner[i] - lowerCorner[i]);
        }
        return volume;
    }
}

