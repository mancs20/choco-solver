package org.chocosolver.solver.search.strategy.strategy;

import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;

public class GIARegionStrategy extends RegionStrategy implements IMonitorRestart {

    private int sumObjectives;
    private final IntVar sumObjectivesVar;
    private final int idxObjsDecisionLimit;
    private boolean returnDecision;

    public GIARegionStrategy(IntVar[] objectives, int[] bounds, IntVar sumObjectivesVar) {
        super(objectives, bounds);
        this.sumObjectivesVar = sumObjectivesVar;
        idxObjsDecision = -1;
        idxObjsDecisionLimit = (fullBounded) ? 2*objs.length : objs.length;
        returnDecision = false;
    }

    @Override
    public Decision<IntVar> getDecision() {
        if (!returnDecision) {
            return null;
        }

        idxObjsDecision = -1;
        isLowerBound = true;
        decOperator = getOperator(true);
        Decision<IntVar> dec;
        do {
            dec = (idxObjsDecision == -1) ? computeDecision(sumObjectivesVar) : computeDecision(objs[idxObjsDecision % objs.length]);
            if (dec == null){
                idxObjsDecision++;
            }
            if (idxObjsDecision > objs.length) {
                isLowerBound = false;
                decOperator = getOperator(false);
            }
        } while (dec == null && idxObjsDecision < idxObjsDecisionLimit);

        return dec;
    }

    @Override
    public Decision<IntVar> computeDecision(IntVar obj) {
        IntDecision dec;
        if (idxObjsDecision == -1) {
            if (sumObjectivesVar.getLB() >= sumObjectives) {
                return null;
            }
            dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, sumObjectives);
        } else {
            if (obj.isInstantiated()) {
                return null;
            }

            if ((isLowerBound && obj.getLB() >= bounds[idxObjsDecision]) ||
                    (!isLowerBound && obj.getUB() <= bounds[idxObjsDecision])) {
                return null;
            }

            int bound = bounds[idxObjsDecision];
            dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, bound);
        }
        dec.setRefutable(false);
        return dec;
    }

    @Override
    public void afterRestart() {
        idxObjsDecision = -1;
        bounds = originalBounds.clone();
        sumObjectives = bounds[0];
        for (int i = 1; i < objs.length; i++) {
            sumObjectives += bounds[i];
        }
    }

    public void configureSearchToDominatePoint(int[] bounds, int sumObjectives){
        this.bounds = bounds;
        this.sumObjectives = sumObjectives;
        idxObjsDecision = -1;
    }

    public void setReturnDecision(boolean returnDecision) {
        this.returnDecision = returnDecision;
    }
}
