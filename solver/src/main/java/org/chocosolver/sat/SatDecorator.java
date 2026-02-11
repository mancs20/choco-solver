/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2026, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.sat;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.queues.CircularQueue;

import java.util.*;
import java.util.function.Consumer;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 19/03/2021
 */
public class SatDecorator extends MiniSat {

    // store clauses dynamically added from outside
    public ArrayList<Clause> dynClauses = new ArrayList<>();
    // store literals dynamically added from outside
    public TIntArrayList dynLits = new TIntArrayList();
    private final TIntObjectHashMap<Literalizer> lits = new TIntObjectHashMap<>();
    private final HashMap<Variable, List<Literalizer>> vars = new HashMap<>();

    // --- bucket mode (optional) ---
    private ArrayDeque<Clause>[] buckets = null; // buckets[decisionDepth]
    private int deepestNonEmpty = -1;
    // identity presence: avoids problems if a clause is removed elsewhere
    private final IdentityHashMap<Clause, Integer> dynIndex = new IdentityHashMap<>();


    /**
     * For comparison with SAT solver trail, to deal properly with backtrack
     */
    private final IStateInt sat_trail_;
    /**
     * Since there is no domain-clause, a fix point may not be reached by SatSolver itself.
     * Stores all modified variable to make sure a fix point is reached.
     */
    private final CircularQueue<Variable> toCheck = new CircularQueue<>(16);

    /**
     * List of early deduction literals
     */
    private final TIntArrayList early_deductions_;

    private final TIntArrayList touched_variables_;

    // --- bucketed storage of dynamic learnt clauses ---
    private final int maxDynClauses = 50_000;      // start here
    private final int trimTo = 40_000;            // hysteresis (avoid trimming every clause)
    public boolean trimDynClausesAlready = false;           // whether to trim or not (for debugging)

    public SatDecorator(Model model) {
        super(false);
        sat_trail_ = model.getEnvironment().makeInt();
        early_deductions_ = new TIntArrayList();
        touched_variables_ = new TIntArrayList();
    }

    public void beforeAddingClauses() {
        this.synchro();
    }

    public void afterAddingClauses() {
        this.storeEarlyDeductions();
    }

    private void trimDynClauses() {
        if (dynClauses.size() <= maxDynClauses) return;

        // Prefer to remove longer clauses first (cheap-ish heuristic: skip binaries)
        // FIFO removal among removable ones.
        int i = 0;
        while (dynClauses.size() > trimTo && i < dynClauses.size()) {
            Clause c = dynClauses.get(i);
            if (c.size() <= 2) { // keep binaries if possible
                i++;
                continue;
            }
            detachLearntBucket(i); // removes element at i, so don't increment i
        }
        // If still too many (e.g., mostly binaries), drop oldest anyway
        while (dynClauses.size() > trimTo) {
            detachLearntBucket(0);
        }

        // tod debug
        System.out.println("Trimmed learnt clauses: " + dynClauses.size() + " remaining");
        trimDynClausesAlready = true;
    }


    /**
     * Add a clause during resolution
     *
     * @param ps clause to add
     */
    public void learnClause(int... ps) {
        Arrays.sort(ps);
        switch (ps.length) {
            case 0:
                ok_ = false;
                return;
            case 1:
                dynLits.add(ps[0]);
                dynUncheckedEnqueue(ps[0]);
                propagate();
                ok_ = (confl == C_Undef);
                return;
            default:
                Clause cr = new Clause(ps);
                //removeDominated(cr);
                dynClauses.add(cr);
                attachClause(cr);
                break;
        }
    }

    /** Same semantics as learnClause, but also puts the clause into a decision-depth bucket */
    public void learnClauseAtDecisionDepth(int decisionDepth, int... ps) {
        Arrays.sort(ps);
        switch (ps.length) {
            case 0:
                ok_ = false;
                return;
            case 1:
                dynLits.add(ps[0]);
                dynUncheckedEnqueue(ps[0]);
                propagate();
                ok_ = (confl == C_Undef);
                return;
            default:
                Clause cr = new Clause(ps);
                dynIndex.put(cr, dynClauses.size());
                dynClauses.add(cr);

                attachClause(cr);

                if (buckets != null) {
                    int d = clampDepth(decisionDepth);
                    buckets[d].addLast(cr);
                    if (d > deepestNonEmpty) deepestNonEmpty = d;
                    trimDynClausesBucketed(); // only if bucket mode enabled
                } else {
                    // fallback: keep your existing trimming if you want
                    trimDynClauses(); // your current one (optional)
                }
        }
    }

    /**
     * Check wether {@code cr} dominates one or more learnt clauses.
     *
     * @param last the clause to compare the other with
     */
    private void removeDominated(Clause last) {
        for (int c = dynClauses.size() - 1; c >= 0; c--) {
            Clause prev = dynClauses.get(c);
            if (last.size() < prev.size()) {
                int i = 0, j = 0;
                while (i < last.size() && j < prev.size()) {
                    int l = last._g(i);
                    int p = prev._g(j);
                    if (l < p) break;
                    j++;
                    if (l == p) {
                        i++;
                    }
                }
                if (i == last.size() && j == prev.size()) {
                    // then 'last' dominates 'prev'
                    detachLearnt(c);
                }
            }
        }
    }

    private void detachLearnt(int ci) {
        Clause cr = dynClauses.get(ci);
        detachClause(cr);
        dynClauses.remove(ci);
    }

    private void detachLearntBucket(int ci) {
        Clause cr = dynClauses.get(ci);
        detachDynClause(cr); // O(1) removal + map update
    }

    public void reset() {
        deleteLearntLits();
        deleteLearntClauses();
    }

    private void deleteLearntClauses() {
        for (int i = dynClauses.size() - 1; i >= 0; i--) {
            detachClause(dynClauses.get(i));
        }
        dynClauses.clear();
        dynIndex.clear();
        deepestNonEmpty = -1;
    }


    private void deleteLearntLits() {
        for (int i = 0; i < dynLits.size(); i++) {
            this.early_deductions_.remove(dynLits.get(i));
        }
        dynLits.resetQuick();
    }

    private void dynUncheckedEnqueue(int l) {
        touched_variables_.add(l);
    }

    public int nLearnt() {
        return dynClauses.size();
    }

    public ESat value(int svar) {
        switch (valueVar(svar)) {
            case lFalse:
                return ESat.FALSE;
            case lTrue:
                return ESat.TRUE;
            default:
            case lUndef:
                return ESat.UNDEFINED;
        }
    }

    /**
     * Propagates one literal, returns true if successful, false in case of failure.
     *
     * @param lit literal to propagate
     * @return {@code false} if a failure occurs.
     * @implNote A call to this fill {@link #touched_variables_} with modified literals.
     */
    public boolean propagateOneLiteral(int lit) {
        assert ok_;
        touched_variables_.resetQuick();
        propagate();
        if (confl != C_Undef) {
            return false;
        }
        if (valueLit(lit) == lTrue) {
            // Dummy decision level:
            pushTrailMarker();
            return true;
        } else if (valueLit(lit) == lFalse) {
            return false;
        }
        pushTrailMarker();
        // Unchecked enqueue
        assert valueLit(lit) == lUndef;
        assignment_.set(var(lit), makeBoolean(sgn(lit)));
        trail_.add(lit);
        propagate();
        return confl == C_Undef;
    }

    public void bound(Variable cpvar, ICause cause) throws ContradictionException {
        try {
            if (sat_trail_.get() < trailMarker()) {
                cancelUntil(sat_trail_.get());
                assert (sat_trail_.get() == trailMarker());
            }
            toCheck.addFirst(cpvar);
            while (!toCheck.isEmpty()) {
                Variable cvar = toCheck.pollFirst();
                List<Literalizer> myLits = vars.get(cvar);
                for (int i = 0; i < myLits.size(); i++) {
                    Literalizer ltz = myLits.get(i);
                    if (ltz.canReact()) {
                        int lit = ltz.toLit();
                        if (propagateOneLiteral(lit)) {
                            sat_trail_.set(trailMarker());
                            for (int j = 0; j < touched_variables_.size(); ++j) {
                                lit = touched_variables_.get(j);
                                Literalizer lzr = lits.get(var(lit));
                                if (lzr != null && lzr.toEvent(lit, cause)) {
                                    toCheck.addFirst(lzr.cvar());
                                }// else case only for addSumBoolArrayLessEqKVar extra variable
                            }
                        } else {
                            ltz.toEvent(neg(lit), cause);
                        }
                    }
                }
            }
        } finally {
            touched_variables_.resetQuick(); // issue#327
            toCheck.clear();
        }
    }

    public void storeEarlyDeductions() {
        for (int i = 0; i < touched_variables_.size(); ++i) {
            int lit = touched_variables_.get(i);
            early_deductions_.add(lit);
        }
        touched_variables_.resetQuick();
    }

    public void applyEarlyDeductions(ICause cause) throws ContradictionException {
        for (int i = 0; i < early_deductions_.size(); ++i) {
            int lit = early_deductions_.get(i);
            lits.get(var(lit)).toEvent(lit, cause);
        }
    }

    public void cancelUntil(int level) {
        super.cancelUntil(level);
    }

    @Override
    public void onLiteralPushed(int l) {
        touched_variables_.add(l);
    }

    /**
     * Bind a boolean variable {@code bvar}, from CP side, to a variable from SAT side.
     * It creates the SAT variable and {@link Literalizer.BoolLit} that connect both world.
     *
     * @param bvar a boolean variable
     * @return the SAT variable (an int)
     */
    public <V extends Variable> int bind(V bvar, Literalizer ltz, Consumer<V> actionOnNew) {
        List<Literalizer> tmp = vars.computeIfAbsent(bvar, k -> new ArrayList<>());
        if (tmp.isEmpty()) {
            actionOnNew.accept(bvar);
        }
        Optional<Literalizer> opt = tmp.stream().filter(l -> l.equals(ltz)).findFirst();
        if (!opt.isPresent()) {
            int var = newVariable();
            ltz.svar(var);
            lits.put(var, ltz);
            tmp.add(ltz);
            opt = Optional.of(ltz);
        }
        return opt.get().svar();
    }


    public void synchro() {
        if (sat_trail_.get() < trailMarker()) {
            cancelUntil(sat_trail_.get());
            assert (sat_trail_.get() == trailMarker());
        }
    }

    /**
     * Checks if all clauses from <code>clauses</code> are satisfied
     *
     * @param clauses list of clause
     * @return <tt>true</tt> if all clauses are satisfied, <tt>false</tt> otherwise
     */
    public boolean clauseEntailed(ArrayList<Clause> clauses) {
        int lit;
        cl:
        for (Clause c : clauses) {
            for (int i = 0; i < c.size(); i++) {
                lit = c._g(i);
                Literalizer ltz = lits.get(var(lit));
                // ltz is null only for 'addClausesSumBoolArrayLessEqKVar' that needs an extra var.
                if (ltz == null || lits.get(var(lit)).check(sgn(lit))) {
                    continue cl;
                }
            }
            return false;
        }
        return true;
    }

    // bucket related
    /** Call from outside once (e.g., from NogoodFromDomFails init) */
    public void enableDecisionBuckets(int maxDecisionDepth) {
        // +1 so depth==maxDecisionDepth is valid
        buckets = (ArrayDeque<Clause>[]) new ArrayDeque[maxDecisionDepth + 1];
        for (int i = 0; i < buckets.length; i++) buckets[i] = new ArrayDeque<>();
        deepestNonEmpty = -1;
        dynIndex.clear();
    }

    private int clampDepth(int depth) {
        if (depth < 0) return 0;
        if (buckets == null) return 0;
        return Math.min(depth, buckets.length - 1);
    }

    private void trimDynClausesBucketed() {
        if (dynClauses.size() <= maxDynClauses) return;

        int toRemove = dynClauses.size() - trimTo; // how many we need to drop

        while (toRemove > 0 && deepestNonEmpty >= 0) {

            // move deepestNonEmpty left until we find a non-empty bucket
            while (deepestNonEmpty >= 0 && (buckets[deepestNonEmpty] == null || buckets[deepestNonEmpty].isEmpty())) {
                deepestNonEmpty--;
            }
            if (deepestNonEmpty < 0) break;

            ArrayDeque<Clause> q = buckets[deepestNonEmpty];

            // DRAIN this bucket while needed
            while (toRemove > 0 && !q.isEmpty()) {
                Clause victim = q.pollFirst();

                // if it was removed by some other path, skip it
                if (!dynIndex.containsKey(victim)) continue;

                detachDynClause(victim);
                toRemove--;
            }

            // if bucket now empty, loop will decrement deepestNonEmpty next iteration
        }
        trimDynClausesAlready = true;
        // tod debug
        System.out.println("Trimmed learnt clauses: " + dynClauses.size() + " remaining");
    }

    private void detachDynClause(Clause cr) {
        detachClause(cr);
        removeDynClauseFast(cr);
    }

    private void removeDynClauseFast(Clause target) {
        Integer idxObj = dynIndex.get(target);
        if (idxObj == null) return; // already removed
        int idx = idxObj;
        int last = dynClauses.size() - 1;

        if (idx != last) {
            Clause moved = dynClauses.get(last);
            dynClauses.set(idx, moved);
            dynIndex.put(moved, idx);
        }
        dynClauses.remove(last);
        dynIndex.remove(target);
    }
}
