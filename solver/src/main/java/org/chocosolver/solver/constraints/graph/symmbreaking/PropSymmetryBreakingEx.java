/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.graph.symmbreaking;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

/**
 * @author Моклев Вячеслав
 */
public class PropSymmetryBreakingEx extends Propagator<BoolVar> {

    private final int n;
    private final BoolVar[] t;

    public PropSymmetryBreakingEx(BoolVar[] t) {
        super(t, PropagatorPriority.CUBIC, false);
        n = (int) Math.round(Math.sqrt(t.length));
        this.t = t;
    }

    private enum Cmp {
        LESS, EQUALS, GREATER, UNDEFINED
    }

    private Cmp compare(BoolVar a, BoolVar b) {
        if (a.isInstantiatedTo(0) && b.isInstantiatedTo(1)) {
            return Cmp.LESS;
        }
        if ((a.isInstantiatedTo(0) && b.isInstantiatedTo(0)) || (a.isInstantiatedTo(1) && b.isInstantiatedTo(1))) {
            return Cmp.EQUALS;
        }
        if (a.isInstantiatedTo(1) && b.isInstantiatedTo(0)) {
            return Cmp.GREATER;
        }
        return Cmp.UNDEFINED;
    }

    private boolean lessOrEqualsEx(int j1, int j2) throws ContradictionException {
        for (int i = 0; i < n; i++) {
            if (i == j1 || i == j2) continue;
            if (t[i + j1 * n].isInstantiatedTo(1)) {
                t[i + j2 * n].instantiateTo(1, this);
            }
            Cmp cmp = compare(t[i + j1 * n], t[i + j2 * n]);
            // lexicographically less or undefined
            if (cmp == Cmp.LESS || cmp == Cmp.UNDEFINED) {
                return true;
            }
            // lexicographically greater
            if (cmp == Cmp.GREATER) {
                return false;
            }
        }
        // entirely equals
        return true;
    }

    private boolean sortedEx() throws ContradictionException {
        for (int j = 1; j < n; j++) {
            for (int i = 0; i < j; i++) {
                if (j - i != 2 && !lessOrEqualsEx(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (!sortedEx()) {
            throw new ContradictionException();
        }
    }

    @Override
    public ESat isEntailed() {
        try {
            if (!sortedEx()) {
                return ESat.FALSE;
            }
        } catch (ContradictionException e) {
            return ESat.FALSE;
        }
        for (BoolVar aT : t) {
            if (!aT.isInstantiated()) {
                return ESat.UNDEFINED;
            }
        }
        return ESat.TRUE;
    }
}
