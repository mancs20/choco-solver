/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.cumulative;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

/**
 * Cumulative propagator
 * Performs energy checking and mandatory part based filtering
 * BEWARE : not idempotent, use two propagators to get the fix point
 *
 * @author Jean-Guillaume Fages
 * @since 31/01/13
 */
public class PropCumulative extends Propagator<IntVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected final int n;
    protected final IntVar[] s, d, e, h;
    protected final IntVar capa;
    protected CumulFilter[] filters;
    protected ISet allTasks;
    protected final IStateInt lastCapaMax;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * protected constructor, should not be called by a user
     */
    protected PropCumulative(IntVar[] s, IntVar[] d, IntVar[] e, IntVar[] h, IntVar capa,
                             boolean reactToFineEvt, CumulFilter... filters) {
        super(ArrayUtils.append(s, d, e, h, new IntVar[]{capa}), PropagatorPriority.QUADRATIC, reactToFineEvt);
        this.n = s.length;
        if (!(n == d.length && n == e.length && n == h.length)) {
            throw new UnsupportedOperationException();
        }
        this.s = Arrays.copyOfRange(vars, 0, n);
        this.d = Arrays.copyOfRange(vars, n, n * 2);
        this.e = Arrays.copyOfRange(vars, n * 2, n * 3);
        this.h = Arrays.copyOfRange(vars, n * 3, n * 4);
        this.capa = this.vars[4 * n];
        this.filters = filters;
        lastCapaMax = model.getEnvironment().makeInt(capa.getUB() + 1);
        allTasks = SetFactory.makeStoredSet(SetType.BIPARTITESET,0,getModel());
        for(int t=0;t<n;t++){
            allTasks.add(t);
        }
    }

    /**
     * Classical cumulative propagator
     *
     * @param s       start 		variables
     * @param d       duration	variables
     * @param e       end			variables
     * @param h       height		variables
     * @param capa    capacity	variable
     *                (only reacts to instantiation events)
     * @param filters filtering algorithm to use
     */
    public PropCumulative(IntVar[] s, IntVar[] d, IntVar[] e, IntVar[] h, IntVar capa,
                          CumulFilter... filters) {
        this(s, d, e, h, capa, false, filters);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int idx) {
        if (idx == vars.length - 1) {
            return IntEventType.upperBoundAndInst();
        }
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (PropagatorEventType.isFullPropagation(evtmask)) {
            propIni();
        }
        updateMaxCapa();
        filter(allTasks);
    }

    protected void propIni() throws ContradictionException {
        for (int i = 0; i < n; i++) {
            d[i].updateLowerBound(0, this);
            h[i].updateLowerBound(0, this);
            s[i].updateBounds(e[i].getLB() - d[i].getUB(), e[i].getUB() - d[i].getLB(), this);
            e[i].updateBounds(s[i].getLB() + d[i].getLB(), s[i].getUB() + d[i].getUB(), this);
            d[i].updateBounds(e[i].getLB() - s[i].getUB(), e[i].getUB() - s[i].getLB(), this);
        }
    }

    protected void updateMaxCapa() throws ContradictionException {
        if (lastCapaMax.get() != capa.getUB()) {
            int capaMax = capa.getUB();
            lastCapaMax.set(capaMax);
            for (int i = 0; i < n; i++) {
                if(d[i].getLB()>0) {
                    h[i].updateUpperBound(capaMax, this);
                }else if(h[i].getLB()>capaMax){
                    d[i].instantiateTo(0,this);
                }
            }
        }
    }

    public void filter(ISet tasks) throws ContradictionException {
        ISetIterator tIter = tasks.iterator();
        while (tIter.hasNext()){
            int t = tIter.nextInt();
            if(h[t].getUB()==0 || d[t].getUB()==0){
                tasks.remove(t);
            }
        }
        for (CumulFilter cf : filters) {
            cf.filter(s, d, e, h, capa, tasks, this);
        }
    }

    @Override
    public ESat isEntailed() {
        int min = s[0].getUB();
        int max = e[0].getLB();
        // check start + duration = end
        for (int i = 0; i < n; i++) {
            min = Math.min(min, s[i].getUB());
            max = Math.max(max, e[i].getLB());
            if (s[i].getLB() + d[i].getLB() > e[i].getUB()
                    || s[i].getUB() + d[i].getUB() < e[i].getLB()) {
                return ESat.FALSE;
            }
        }
        // check capacity
        int maxLoad = 0;
        if (min <= max) {
            int capamax = capa.getUB();
            int[] consoMin = new int[max - min];
            for (int i = 0; i < n; i++) {
                for (int t = s[i].getUB(); t < e[i].getLB(); t++) {
                    consoMin[t - min] += h[i].getLB();
                    if (consoMin[t - min] > capamax) {
                        return ESat.FALSE;
                    }
                    maxLoad = Math.max(maxLoad, consoMin[t - min]);
                }
            }
        }
        // check variables are instantiated
        for (int i = 0; i < vars.length - 1; i++) {
            if (!vars[i].isInstantiated()) {
                return ESat.UNDEFINED;
            }
        }
        assert min <= max;
        // capacity check entailed
        if (maxLoad <= vars[4 * n].getLB()) {
            return ESat.TRUE;
        }
        // capacity not instantiated
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + "(");
        for (int i = 0; i < n; i++) {
            sb.append("[").append(vars[i].toString());
            sb.append(",").append(vars[i + n].toString());
            sb.append(",").append(vars[i + 2 * n].toString());
            sb.append(",").append(vars[i + 3 * n].toString()).append("],");
        }
        sb.append(vars[4 * n].toString()).append(")");
        return sb.toString();
    }
}
