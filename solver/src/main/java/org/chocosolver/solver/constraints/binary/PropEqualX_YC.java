/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.binary;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.constraints.Explained;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;

/**
 * X = Y + C
 * <p>
 * <br/>
 *
 * @author Charles Prud'homme, Jean-Guillaume Fages
 * @since 1 oct. 2010
 */
 @Explained(partial = true, comment = "must be tested")
public final class PropEqualX_YC extends Propagator<IntVar> {


    private final IntVar x;
    private final IntVar y;
    private final int cste;
    // incremental filtering of enumerated domains
    private boolean bothEnumerated;
    private IIntDeltaMonitor[] idms;
    private IntProcedure rem_proc;
    private int indexToFilter;
    private int offSet;

    public PropEqualX_YC(IntVar[] vars, int c) {
        super(vars, PropagatorPriority.BINARY, true);
        this.x = vars[0];
        this.y = vars[1];
        this.cste = c;
        if (x.hasEnumeratedDomain() && y.hasEnumeratedDomain()) {
            bothEnumerated = true;
            idms = new IIntDeltaMonitor[2];
            idms[0] = vars[0].monitorDelta(this);
            idms[1] = vars[1].monitorDelta(this);
            rem_proc = i -> vars[indexToFilter].removeValue(i + offSet, this,
                    lcg() ? Reason.r(vars[1 - indexToFilter].getLit(i, IntVar.LR_NE)) : Reason.undef());
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vars[0].hasEnumeratedDomain() && vars[1].hasEnumeratedDomain())
            return IntEventType.all();
        else
            return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        updateBounds();
        // ensure that, in case of enumerated domains, holes are also propagated
        if (bothEnumerated) {
            int ub = x.getUB();
            for (int val = x.getLB(); val <= ub; val = x.nextValue(val)) {
                if (!y.contains(val - cste)) {
                    x.removeValue(val, this, lcg() ? Reason.r(y.getLit(val - cste, IntVar.LR_NE)) : Reason.undef());
                }
            }
            ub = y.getUB();
            for (int val = y.getLB(); val <= ub; val = y.nextValue(val)) {
                if (!x.contains(val + cste)) {
                    y.removeValue(val, this, lcg() ? Reason.r(x.getLit(val + cste, IntVar.LR_NE)) : Reason.undef());
                }
            }
            idms[0].startMonitoring();
            idms[1].startMonitoring();
        }
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        updateBounds();
        if (bothEnumerated && (!x.isInstantiated() || !y.isInstantiated())) {
            if (varIdx == 0) {
                indexToFilter = 1;
                offSet = -cste;
            } else {
                indexToFilter = 0;
                offSet = cste;
            }
            idms[varIdx].forEachRemVal(rem_proc);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void updateBounds() throws ContradictionException {
        while (x.updateLowerBound(y.getLB() + cste, this, lcg() ? Reason.r(y.getMinLit()) : Reason.undef())
                | y.updateLowerBound(x.getLB() - cste, this, lcg() ? Reason.r(x.getMinLit()) : Reason.undef())) ;
        while (x.updateUpperBound(y.getUB() + cste, this, lcg() ? Reason.r(y.getMaxLit()) : Reason.undef())
                | y.updateUpperBound(x.getUB() - cste, this, lcg() ? Reason.r(x.getMaxLit()) : Reason.undef())) ;
    }

    @Override
    public ESat isEntailed() {
        if ((x.getUB() < y.getLB() + cste) ||
                (x.getLB() > y.getUB() + cste) ||
                x.hasEnumeratedDomain() && y.hasEnumeratedDomain() && !match())
            return ESat.FALSE;
        else if (x.isInstantiated() &&
                y.isInstantiated() &&
                (x.getValue() == y.getValue() + cste))
            return ESat.TRUE;
        else
            return ESat.UNDEFINED;
    }


    private boolean match() {
        int lb = x.getLB();
        int ub = x.getUB();
        for (; lb <= ub; lb = x.nextValue(lb)) {
            if (y.contains(lb - cste)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder bf = new StringBuilder();
        bf.append("prop(").append(vars[0].getName()).append(".EQ.").append(vars[1].getName());
        if (cste != 0) {
            bf.append("+").append(cste);
        }
        bf.append(")");
        return bf.toString();
    }
}
