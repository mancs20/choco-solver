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
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * A specific <code>Propagator</code> extension defining filtering algorithm for:
 * <br/>
 * <b>X =/= Y</b>
 * <br>where <i>X</i> and <i>Y</i> are <code>Variable</code> objects.
 * <br>
 * This <code>Propagator</code> defines the <code>propagate</code> and <code>awakeOnInst</code> methods. The other ones
 * throw <code>UnsupportedOperationException</code>.
 * <br/>
 * <br/>
 * <i>Based on Choco-2.1.1</i>
 *
 * @author Xavier Lorca
 * @author Charles Prud'homme
 * @author Arnaud Malapert
 * @version 0.01, june 2010
 * @since 0.01
 */
@Explained
public class PropNotEqualX_Y extends Propagator<IntVar> {

    private final IntVar x;
    private final IntVar y;

    @SuppressWarnings({"unchecked"})
    public PropNotEqualX_Y(IntVar x, IntVar y) {
        super(ArrayUtils.toArray(x, y), PropagatorPriority.BINARY, false);
        this.x = vars[0];
        this.y = vars[1];
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        //Principle : if v0 is instantiated and v1 is enumerated, then awakeOnInst(0) performs all needed pruning
        //Otherwise, we must check if we can remove the value from v1 when the bounds has changed.
        if (vars[vIdx].hasEnumeratedDomain()) {
            return IntEventType.instantiation();
        }
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (x.isInstantiated() && y.isInstantiated() && x.isInstantiatedTo(y.getValue())) {
            this.fails(lcg() ? Reason.r(x.getValLit(), y.getValLit()) : Reason.undef());
        }

        if (x.isInstantiated()) {
            if (y.removeValue(x.getValue(), this, lcg() ? Reason.r(x.getValLit()) : Reason.undef()) || !y.contains(x.getValue())) {
                this.setPassive();
            }
        } else if (y.isInstantiated()) {
            if (x.removeValue(y.getValue(), this, lcg() ? Reason.r(y.getValLit()) : Reason.undef()) || !x.contains(y.getValue())) {
                this.setPassive();
            }
        } else if (x.getUB() < (y.getLB()) || (y.getUB()) < x.getLB()) {
            setPassive();
        }
    }

    @Override
    public ESat isEntailed() {
        if ((x.getUB() < y.getLB()) || (y.getUB() < x.getLB()))
            return ESat.TRUE;
        else if (x.isInstantiated() && y.isInstantiated())
            return ESat.FALSE;
        else
            return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return "prop(" + vars[0].getName() + ".NEQ." + vars[1].getName() + ")";
    }

}
