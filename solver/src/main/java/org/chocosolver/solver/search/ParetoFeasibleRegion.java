package org.chocosolver.solver.search;

public class ParetoFeasibleRegion {
    final private int[] lowerCorner;
    final private int[] upperCorner;

    // Constructor
    public ParetoFeasibleRegion(int[] lowerCorner, int[] upperCorner) {
        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
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

    public double getRegionVolume(){
        double volume = 1;
        for (int i = 0; i < lowerCorner.length; i++) {
            volume = volume * (upperCorner[i] - lowerCorner[i]);
        }
        return volume;
    }
}

