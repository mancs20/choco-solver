package org.chocosolver.examples.integer.experiments;

public class ParetoObjective {
    private final int LB;
    private final int UB;

    public ParetoObjective(int LB, int UB) {
        this.LB = LB;
        this.UB = UB;
    }

    public int getLB() {
        return LB;
    }

    public int getUB() {
        return UB;
    }
}
