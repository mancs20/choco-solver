/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.view.integer.IntAffineView;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Container representing a task:
 * It ensures that: start + duration = end
 *
 * @author Jean-Guillaume Fages
 * @since 04/02/2013
 */
public class Task {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final IntVar start;
    private final IntVar duration;
    private final IntVar end;
    private IVariableMonitor<IntVar> update;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param model the Model of the variables
     * @param est   earliest starting time
     * @param lst   latest starting time
     * @param d     duration
     * @param ect   earliest completion time
     * @param lct   latest completion time time
     */
    public Task(Model model, int est, int lst, int d, int ect, int lct) {
        start = model.intVar(est, lst);
        duration = model.intVar(d);
        if (ect == est + d && lct == lst + d) {
            end = start.getModel().offset(start, d);
        } else {
            end = model.intVar(ect, lct);
            declareMonitor();
        }
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     */
    public Task(IntVar s, int d) {
        start = s;
        duration = start.getModel().intVar(d);
        end = start.getModel().offset(start, d);
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param e end variable
     */
    public Task(IntVar s, int d, IntVar e) {
        start = s;
        duration = start.getModel().intVar(d);
        end = e;
        if (!isOffsetView(s, d, e)) {
            declareMonitor();
        }
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end
     *
     * @param s start variable
     * @param d duration variable
     * @param e end variable
     */
    public Task(IntVar s, IntVar d, IntVar e) {
        start = s;
        duration = d;
        end = e;
        if (!d.isInstantiated() || !isOffsetView(s, d.getValue(), e)) {
            declareMonitor();
        }
    }

    private static boolean isOffsetView(IntVar s, int d, IntVar e) {
        if (e instanceof IntAffineView) {
            IntAffineView<?> intOffsetView = (IntAffineView<?>) e;
            return intOffsetView.equals(s, 1, d);
        }
        return false;
    }

    private void declareMonitor() {
        if (start.hasEnumeratedDomain() || duration.hasEnumeratedDomain() || end.hasEnumeratedDomain()) {
            update = new TaskMonitor(start, duration, end, true);
        } else {
            update = new TaskMonitor(start, duration, end, false);
        }
        Model model = start.getModel();
        //noinspection unchecked
        ArrayList<Task> tset = (ArrayList<Task>) model.getHook(Model.TASK_SET_HOOK_NAME);
        if (tset == null) {
            tset = new ArrayList<>();
            model.addHook(Model.TASK_SET_HOOK_NAME, tset);
        }
        tset.add(this);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    /**
     * Applies BC-filtering so that start + duration = end
     *
     * @throws ContradictionException thrown if a inconsistency has been detected between start, end and duration
     */
    public void ensureBoundConsistency() throws ContradictionException {
        update.onUpdate(start, IntEventType.REMOVE);
    }

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

    public IntVar getStart() {
        return start;
    }

    public IntVar getDuration() {
        return duration;
    }

    public IntVar getEnd() {
        return end;
    }

    public IVariableMonitor<IntVar> getMonitor() {
        return update;
    }

    @Override
    public String toString() {
        return "Task[" +
                "start=" + start +
                ", duration=" + duration +
                ", end=" + end +
                ']';
    }

    private static class TaskMonitor implements IVariableMonitor<IntVar> {
        private final IntVar S, D, E;
        private final boolean isEnum;

        private TaskMonitor(IntVar S, IntVar D, IntVar E, boolean isEnum) {
            this.S = S;
            this.D = D;
            this.E = E;
            S.addMonitor(this);
            D.addMonitor(this);
            E.addMonitor(this);
            this.isEnum = isEnum;
        }

        @Override
        public void onUpdate(IntVar var, IEventType evt) throws ContradictionException {
            boolean lcg = var.getModel().getSolver().isLCG();
            boolean fixpoint;
            do {
                // start
                fixpoint = S.updateLowerBound(E.getLB() - D.getUB(), this,
                        lcg ? Reason.r(E.getMinLit(), D.getMaxLit()) : Reason.undef());
                fixpoint |= S.updateUpperBound(E.getUB() - D.getLB(), this,
                        lcg ? Reason.r(E.getMaxLit(), D.getMinLit()) : Reason.undef());
                // end
                fixpoint |= E.updateLowerBound(S.getLB() + D.getLB(), this,
                        lcg ? Reason.r(S.getMinLit(), D.getMinLit()) : Reason.undef());
                fixpoint |= E.updateUpperBound(S.getUB() + D.getUB(), this,
                        lcg ? Reason.r(S.getMaxLit(), D.getMaxLit()) : Reason.undef());
                // duration
                fixpoint |= D.updateLowerBound(E.getLB() - S.getUB(), this,
                        lcg ? Reason.r(E.getMinLit(), S.getMaxLit()) : Reason.undef());
                fixpoint |= D.updateUpperBound(E.getUB() - S.getLB(), this,
                        lcg ? Reason.r(E.getMaxLit(), S.getMinLit()) : Reason.undef());
            } while (fixpoint && isEnum);
        }

        @Override
        public void forEachIntVar(Consumer<IntVar> action) {
            action.accept(S);
            action.accept(D);
            action.accept(E);
        }

        @Override
        public String toString() {
            return "Task[" + S.getName() + "+" + D.getName() + "=" + E.getName() + "]";
        }
    }
}
