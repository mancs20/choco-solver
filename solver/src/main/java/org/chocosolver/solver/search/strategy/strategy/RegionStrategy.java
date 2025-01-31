package org.chocosolver.solver.search.strategy.strategy;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperator;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;


public class RegionStrategy extends AbstractStrategy<IntVar> implements IMonitorRestart {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int[] bounds;
    private boolean isLowerBound;

    private final IntVar[] objs;
    private final int[] originalBounds;
    private int idxObjsDecision = 0;
    private boolean fullBounded = false;

    private final Model model;
    private DecisionOperator<IntVar> decOperator;

    /**
     *
//     * @param objectives variable
     *
     */
    public RegionStrategy(IntVar[] objectives, int[] bounds) {
        super(objectives);
        this.objs = objectives;

        this.bounds = bounds;
        this.model = objs[0].getModel();
        if (objs.length != bounds.length && 2*objs.length != bounds.length) {
            throw new UnsupportedOperationException("The number of objectives and bounds should be the same or the double");
        }
        if (bounds.length == 2*objs.length) {
            fullBounded = true;
        }
        originalBounds = new int[bounds.length];
        for (int i = 0; i < objs.length; i++) {
            originalBounds[i] = objectives[i].getLB();
            if (fullBounded) {
                originalBounds[i + objs.length] = objectives[i].getUB();
            }
        }
        isLowerBound = true;
    }

     private DecisionOperator<IntVar> getOperator(boolean isLowerBound) {
        return isLowerBound ? DecisionOperatorFactory.makeIntReverseSplit() : DecisionOperatorFactory.makeIntSplit();
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    public boolean init() {
        decOperator = getOperator(true);
        return true;
    }

    @Override
    public void remove() {

    }

    @Override
    public Decision<IntVar> getDecision() {
        isLowerBound = true;
        if (fullBounded) {
            if (idxObjsDecision >= objs.length) {
                if (idxObjsDecision < 2*objs.length) {
                    isLowerBound = false;
                } else {
                    idxObjsDecision = 0;
                }
            }
        } else {
            if (idxObjsDecision >= objs.length) {
                idxObjsDecision = 0;
            }
        }
        decOperator = getOperator(isLowerBound);
        return computeDecision(objs[idxObjsDecision % objs.length]);
    }

    @Override
    public Decision<IntVar> computeDecision(IntVar obj) {
        if (obj.isInstantiated()) {
            idxObjsDecision++;
            return null;
        }

        if ((isLowerBound && obj.getLB() >= bounds[idxObjsDecision]) ||
                (!isLowerBound && obj.getUB() <= bounds[idxObjsDecision])) {
            idxObjsDecision++;
            return null;
        }

        if (obj.getLB() > obj.getUB()) {
            idxObjsDecision++;
            return null;
        }

        int bound = bounds[idxObjsDecision];
        idxObjsDecision++;

        String operator = isLowerBound ? ">=" : "<=";
        if (model.getSettings().warnUser()) {
            model.getSolver().log().bold().println("objective " + obj + " " + operator + " " + bound);
        }

        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, bound);
        if (model.getSettings().warnUser()) {
            model.getSolver().log().bold().println("- trying " + obj + " " + operator + " " + bound);
        }
        dec.setRefutable(false);
        return dec;
    }

    public int[] getBounds() {
        return bounds;
    }

    public void setBounds(int[] bounds) {
        this.bounds = bounds;
        idxObjsDecision = 0;
    }

    @Override
    public void afterRestart() {
        idxObjsDecision = 0;
        bounds = originalBounds.clone();
    }
}
