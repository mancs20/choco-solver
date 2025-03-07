/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.unary;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.constraints.Explained;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.constraints.UpdatablePropagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableRangeSet;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableSetUtils;

/**
 * This propagator manages a singleton nogood.
 * <p>
 * <p>
 * Project: choco-solver.
 *
 * @author Charles Prud'homme
 * @since 12/10/2016.
 */
@Explained
public class PropMember extends Propagator<IntVar> implements UpdatablePropagator<IntIterableRangeSet> {

    /**
     * List of possible values.
     */
    private final IntIterableRangeSet range;

    /**
     * Maintain : <i>var</i>&isin;<i>range</i>
     *
     * @param var   a variable
     * @param range list of possible values
     */
    public PropMember(IntVar var, IntIterableRangeSet range, boolean isObjectiveFunction) {
        super(new IntVar[]{var}, PropagatorPriority.UNARY, false, !isObjectiveFunction);
        this.range = range.duplicate();
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.all();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (enforce(vars[0], range, this)) {
            setPassive();
        }
    }

    private static boolean enforce(IntVar var, IntIterableRangeSet fset, Propagator<IntVar> prop) throws ContradictionException {
        if (prop.lcg()) {
            boolean rem = false;
            for (int i : var) {
                if (fset.contains(i)) continue;
                rem |= var.removeValue(i, prop, Reason.undef());
            }
            return rem && (var.hasEnumeratedDomain() || IntIterableSetUtils.includedIn(var, fset));
        } else
            return var.removeAllValuesBut(fset, prop, Reason.undef())
                    && (var.hasEnumeratedDomain() || IntIterableSetUtils.includedIn(var, fset));
    }

    @Override
    public ESat isEntailed() {
        if (IntIterableSetUtils.includedIn(vars[0], range)) {
            return ESat.TRUE;
        } else if (range.intersect(vars[0])) {
            return ESat.UNDEFINED;
        }
        return ESat.FALSE;
    }

    @Override
    public String toString() {
        return vars[0].getName() + " \u2208 " + range;
    }

    @Override
    public void update(IntIterableRangeSet values, boolean thenForcePropagate) {
        this.range.clear();
        this.range.addAll(values);
        if (thenForcePropagate) forcePropagationOnBacktrack();
    }

    @Override
    public IntIterableRangeSet getUpdatedValue() {
        return this.range.duplicate();
    }
}
