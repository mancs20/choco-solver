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

import org.chocosolver.sat.Literalizer;
import org.chocosolver.sat.Reason;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.constraints.nary.cnf.ILogical;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.expression.discrete.relational.ReExpression;
import org.chocosolver.util.ESat;

import java.util.HashSet;

/**
 * <br/>
 * CPRU r544: remove default implementation
 *
 * @author Charles Prud'homme
 * @since 18 nov. 2010
 */
public interface BoolVar extends IntVar, ILogical, ReExpression {

    int kFALSE = 0;
    int kTRUE = 1;
    int kUNDEF = 2;

    ESat getBooleanValue();

    default boolean setToTrue(ICause cause) throws ContradictionException {
        return instantiateTo(kTRUE, cause, cause.defaultReason(this));
    }

    default boolean setToTrue(ICause cause, Reason reason) throws ContradictionException {
        return instantiateTo(kTRUE, cause, reason);
    }

    default boolean setToFalse(ICause cause) throws ContradictionException {
        return instantiateTo(kFALSE, cause, cause.defaultReason(this));
    }

    default boolean setToFalse(ICause cause, Reason reason) throws ContradictionException {
        return instantiateTo(kFALSE, cause, reason);
    }

    BoolVar not();

    boolean hasNot();

    void _setNot(BoolVar not);

    @Override
    default IntVar intVar() {
        return boolVar();
    }

    @Override
    default BoolVar boolVar() {
        return this;
    }

    @Override
    default void extractVar(HashSet<IntVar> variables) {
        variables.add(this);
    }

    /**
     * Creates, or returns if already existing, the SAT variable twin of this.
     *
     * @return the SAT variable of this
     */
    default int satVar() {
        return this.getModel().satVar(this, new Literalizer.BoolLit(this));
    }
}
