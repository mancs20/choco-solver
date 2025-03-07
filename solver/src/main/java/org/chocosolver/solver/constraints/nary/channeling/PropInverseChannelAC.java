/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.channeling;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.constraints.Explained;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.UnaryIntProcedure;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

/**
 * X[i] = j+Ox <=> Y[j] = i+Oy
 * <p>
 * AC propagator for enumerated domain variables
 *
 * @author Jean-Guillaume Fages
 * @since Nov 2012
 */
@Explained
public class PropInverseChannelAC extends Propagator<IntVar> {

    private final int minX;
    private final int minY;
    private final int n;
    private final IntVar[] X;
    private final IntVar[] Y;
    private final RemProc rem_proc;
    private final IIntDeltaMonitor[] idms;
    private final ICause cause;

    public PropInverseChannelAC(IntVar[] X, IntVar[] Y, int minX, int minY) {
        super(ArrayUtils.append(X, Y), PropagatorPriority.LINEAR, true);
        for (int i = 0; i < this.vars.length; i++) {
            if (!vars[i].hasEnumeratedDomain()) {
                throw new UnsupportedOperationException("this propagator should be used with enumerated domain variables");
            }
        }
        this.X = Arrays.copyOfRange(this.vars, 0, X.length);
        this.Y = Arrays.copyOfRange(this.vars, X.length, vars.length);
        n = Y.length;
        this.minX = minX;
        this.minY = minY;
        rem_proc = new RemProc();
        this.idms = new IIntDeltaMonitor[this.vars.length];
        for (int i = 0; i < vars.length; i++) {
            idms[i] = this.vars[i].monitorDelta(this);
        }
        this.cause = this;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
            X[i].updateLowerBound(minX, this, Reason.undef());
            X[i].updateUpperBound(n - 1 + minX, this, Reason.undef());
            Y[i].updateLowerBound(minY, this, Reason.undef());
            Y[i].updateUpperBound(n - 1 + minY, this, Reason.undef());
        }
        for (int i = 0; i < n; i++) {
            enumeratedFilteringOfX(i);
            enumeratedFilteringOfY(i);
        }
        for (int i = 0; i < vars.length; i++) {
            idms[i].startMonitoring();
        }
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        idms[varIdx].forEachRemVal(rem_proc.set(varIdx));
    }

    private void enumeratedFilteringOfX(int var) throws ContradictionException {
        // X[i] = j+Ox <=> Y[j] = i+Oy
        int min = X[var].getLB();
        int max = X[var].getUB();
        for (int v = min; v <= max; v = X[var].nextValue(v)) {
            if (!Y[v - minX].contains(var + minY)) {
                X[var].removeValue(v, this,
                        lcg()? Reason.r(Y[v - minX].getLit(var + minY, IntVar.LR_EQ)) : Reason.undef());
            }
        }
    }

    private void enumeratedFilteringOfY(int var) throws ContradictionException {
        // X[i] = j+Ox <=> Y[j] = i+Oy
        int min = Y[var].getLB();
        int max = Y[var].getUB();
        for (int v = min; v <= max; v = Y[var].nextValue(v)) {
            if (!X[v - minY].contains(var + minX)) {
                Y[var].removeValue(v, this,
                        lcg()? Reason.r(X[v - minY].getLit(var + minX, IntVar.LR_EQ)) : Reason.undef());
            }
        }
    }

    private class RemProc implements UnaryIntProcedure<Integer> {
        private int var;

        @Override
        public UnaryIntProcedure<Integer> set(Integer idxVar) {
            this.var = idxVar;
            return this;
        }

        @Override
        public void execute(int val) throws ContradictionException {
            if (var < n) {
                Y[val - minX].removeValue(var + minY, cause,
                        lcg()? Reason.r(X[var].getLit(val, IntVar.LR_EQ)) : Reason.undef());
            } else {
                X[val - minY].removeValue(var - n + minX, cause,
                        lcg()? Reason.r(Y[var - n].getLit(val, IntVar.LR_EQ)) : Reason.undef());
            }
        }
    }

    @Override
    public ESat isEntailed() {
        boolean allInst = true;
        for (int i = 0; i < n; i++) {
            if (!(vars[i].isInstantiated() && vars[i + n].isInstantiated())) {
                allInst = false;
                break;
            }
            if (X[i].isInstantiated()) {
                int x = X[i].getValue() - minX;
                if (x < 0 || x >= n) return ESat.FALSE;
                if (!Y[x].contains(i + minY) && Y[x].isInstantiated()) return ESat.FALSE;
            }
            if (Y[i].isInstantiated()) {
                int y = Y[i].getValue() - minY;
                if (y < 0 || y >= n) return ESat.FALSE;
                if (!X[y].contains(i + minX) && X[i].isInstantiated()) return ESat.FALSE;
            }
        }
        if (allInst) return ESat.TRUE;
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return "Inverse_AC({" + X[0] + "...}{" + Y[0] + "...})";
    }

}
