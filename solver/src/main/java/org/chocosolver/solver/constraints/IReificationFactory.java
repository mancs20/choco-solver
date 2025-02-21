/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints;

import org.chocosolver.solver.ISelf;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.reification.*;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableRangeSet;

/**
 * Interface to reify constraints
 * A kind of factory relying on interface default implementation to allow (multiple) inheritance
 *
 * @author Jean-Guillaume FAGES
 * @since 4.0.0
 */
public interface IReificationFactory extends ISelf<Model> {

    //***********************************************************************************
    // Non-reifiable reification constraints
    //***********************************************************************************

    /**
     * Posts a constraint ensuring that if <i>ifCstr</i> is satisfied, then <i>thenCstr</i> must be satisfied as well
     * Otherwise, <i>elseCstr</i> must be satisfied
     * <i>ifCstr</i> => <i>ThenCstr</i>
     * <i>not(ifCstr)</i> => <i>ElseCstr</i>
     * <p>
     * BEWARE : it is automatically posted (it cannot be reified)
     *
     * @param ifCstr   a constraint
     * @param thenCstr a constraint
     * @param elseCstr a constraint
     */
    default void ifThenElse(Constraint ifCstr, Constraint thenCstr, Constraint elseCstr) {
        ifThenElse(ifCstr.reify(), thenCstr, elseCstr);
    }

    /**
     * Posts an implication constraint: <i>ifVar</i> => <i>thenCstr</i> && not(<i>ifVar</i>) => <i>elseCstr</i>.
     * <br/>
     * Ensures:
     * <p/>- <i>ifVar</i> = 1 =>  <i>thenCstr</i> is satisfied, <br/>
     * <p/>- <i>ifVar</i> = 0 =>  <i>elseCstr</i> is satisfied, <br/>
     * <p/>- <i>thenCstr</i> is not satisfied => <i>ifVar</i> = 0 <br/>
     * <p/>- <i>elseCstr</i> is not satisfied => <i>ifVar</i> = 1 <br/>
     * <p/>
     * <p/> In order to get <i>ifVar</i> <=> <i>thenCstr</i>, use reification
     * <p>
     * BEWARE : it is automatically posted (it cannot be reified)
     *
     * @param ifVar    variable of reification
     * @param thenCstr the constraint to be satisfied when <i>ifVar</i> = 1
     * @param elseCstr the constraint to be satisfied when <i>ifVar</i> = 0
     */
    default void ifThenElse(BoolVar ifVar, Constraint thenCstr, Constraint elseCstr) {
        ifThen(ifVar, thenCstr);
        ifThen(ifVar.not(), elseCstr);
    }

    /**
     * Posts a constraint ensuring that if <i>ifCstr</i> is satisfied, then <i>thenCstr</i> is satisfied as well
     * <p>
     * BEWARE : it is automatically posted (it cannot be reified)
     *
     * @param ifCstr   a constraint
     * @param thenCstr a constraint
     */
    default void ifThen(Constraint ifCstr, Constraint thenCstr) {
        ifThen(ifCstr.reify(), thenCstr);
    }

    /**
     * Posts an implication constraint: <i>ifVar</i> => <i>thenCstr</i>
     * Also called half reification constraint
     * Ensures:<br/>
     * <p/>- <i>ifVar</i> = 1 =>  <i>thenCstr</i> is satisfied, <br/>
     * <p/>- <i>thenCstr</i> is not satisfied => <i>ifVar</i> = 0 <br/>
     * <p/>
     * Example : <br/>
     * - <code>ifThen(b1, arithm(v1, "=", 2));</code>:
     * b1 is equal to 1 => v1 = 2, so v1 != 2 => b1 is equal to 0
     * But if b1 is equal to 0, nothing happens
     * <p>
     * BEWARE : it is automatically posted (it cannot be reified)
     * <p/>
     *
     * @param ifVar    variable of reification
     * @param thenCstr the constraint to be satisfied when <i>ifVar</i> = 1
     */
    default void ifThen(BoolVar ifVar, Constraint thenCstr) {
        // PRESOLVE
        if (ifVar.contains(1)) {
            if (ifVar.isInstantiated()) {
                thenCstr.post();
            } else if (thenCstr.isSatisfied() == ESat.FALSE) {
                thenCstr.ignore();
                ref().arithm(ifVar, "=", 0).post();
            }
            // END OF PRESOLVE
            else {
                ref().arithm(ifVar, "<=", thenCstr.reify()).post();
            }
        } else {
            thenCstr.ignore();
        }
    }

    /**
     * Posts an equivalence constraint stating that
     * <i>cstr1</i> is satisfied <=>  <i>cstr2</i> is satisfied,
     * <p>
     * BEWARE : it is automatically posted (it cannot be reified)
     *
     * @param cstr1 a constraint to be satisfied if and only if <i>cstr2</i> is satisfied
     * @param cstr2 a constraint to be satisfied if and only if <i>cstr1</i> is satisfied
     */
    default void ifOnlyIf(Constraint cstr1, Constraint cstr2) {
        reification(cstr1.reify(), cstr2);
    }

    /**
     * Reify a constraint with a boolean variable:
     * <i>var</i> = 1 <=>  <i>cstr</i> is satisfied,
     * <p>
     * Equivalent to ifOnlyIf
     * <p>
     * BEWARE : it is automatically posted (it cannot be reified)
     *
     * @param var  variable of reification
     * @param cstr the constraint to be satisfied if and only if <i>var</i> = 1
     */
    default void reification(BoolVar var, Constraint cstr) {
        // PRESOLVE
        ESat entail = cstr.isSatisfied();
        if (var.isInstantiatedTo(1)) {
            cstr.post();
        } else if (var.isInstantiatedTo(0)) {
            cstr.getOpposite().post();
        } else if (entail == ESat.TRUE) {
            cstr.ignore();
            ref().arithm(var, "=", 1).post();
        } else if (entail == ESat.FALSE) {
            cstr.ignore();
            ref().arithm(var, "=", 0).post();
        }
        // END OF PRESOLVE
        else {
            cstr.reifyWith(var);
        }
    }

    /**
     * Posts one constraint that expresses : (x = c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X a integer variable
     * @param C an int
     * @param B a boolean variable
     */
    default void reifyXeqC(IntVar X, int C, BoolVar B) {
        // no check to allow addition during resolution
        if (ref().getSolver().getSearchState() == SearchState.NEW) {
            if (X.isInstantiatedTo(C)) {
                ref().arithm(B, "=", 1).post();
                return;
            } else if (!X.contains(C)) {
                ref().arithm(B, "=", 0).post();
                return;
            }
        }
        reifXrelC(X, "=", C, B);
    }

    /**
     * Posts one constraint that expresses : (x &ne; c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X a integer variable
     * @param C an int
     * @param B a boolean variable
     */
    default void reifyXneC(IntVar X, int C, BoolVar B) {
        // no check to allow addition during resolution
        reifyXeqC(X, C, B.not());
    }

    /**
     * Posts one constraint that expresses : (x = y) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param B a boolean variable
     */
    @SuppressWarnings("SuspiciousNameCombination")
    default void reifyXeqY(IntVar X, IntVar Y, BoolVar B) {
        if (X == Y) {
            ref().arithm(B, "=", 1).post();
        } else if (X.isAConstant()) {
            reifyXeqC(Y, X.getValue(), B);
        } else if (Y.isAConstant()) {
            reifyXeqC(X, Y.getValue(), B);
        } else {
            // no check to allow addition during resolution
            reifyXeqYC(X, Y, 0, B);
        }
    }

    /**
     * Posts one constraint that expresses : (x &ne; y) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param B a boolean variable
     */
    @SuppressWarnings("SuspiciousNameCombination")
    default void reifyXneY(IntVar X, IntVar Y, BoolVar B) {
        if (X.isAConstant()) {
            reifyXneC(Y, X.getValue(), B);
        } else if (Y.isAConstant()) {
            reifyXneC(X, Y.getValue(), B);
        } else {
            // no check to allow addition during resolution
            reifyXeqY(X, Y, B.not());
        }
    }

    /**
     * Posts one constraint that expresses : (x = y + c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param C an int
     * @param B a boolean variable
     */
    @SuppressWarnings("SuspiciousNameCombination")
    default void reifyXeqYC(IntVar X, IntVar Y, int C, BoolVar B) {
//        if (C == 0) {
//            reifyXeqY(X, Y, B);
//        } else
        if (X.isAConstant()) {
            reifyXeqC(Y, X.getValue() - C, B);
        } else if (Y.isAConstant()) {
            reifyXeqC(X, Y.getValue() + C, B);
        } else {
            reifXrelYC(X, "=", Y, C, B);
        }
    }

    /**
     * Posts one constraint that expresses : (x &ne; y + c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param C an int
     * @param B a boolean variable
     */
    @SuppressWarnings("SuspiciousNameCombination")
    default void reifyXneYC(IntVar X, IntVar Y, int C, BoolVar B) {
        if (X.isAConstant()) {
            reifyXneC(Y, X.getValue() - C, B);
        } else if (Y.isAConstant()) {
            reifyXneC(X, Y.getValue() + C, B);
        } else {
            // no check to allow addition during resolution
            reifyXeqYC(X, Y, C, B.not());
        }
    }

    /**
     * Posts one constraint that expresses : (x < c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X a integer variable
     * @param C an int
     * @param B a boolean variable
     */
    default void reifyXltC(IntVar X, int C, BoolVar B) {
        // no check to allow addition during resolution
        if (ref().getSolver().getSearchState() == SearchState.NEW) {
            if (X.getUB() < C) {
                ref().arithm(B, "=", 1).post();
                return;
            } else if (X.getLB() >= C) {
                ref().arithm(B, "=", 0).post();
                return;
            }
        }
        reifXrelC(X, "<", C, B);
    }

    /**
     * Posts one constraint that expresses : (x > c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X a integer variable
     * @param C an int
     * @param B a boolean variable
     */
    default void reifyXgtC(IntVar X, int C, BoolVar B) {
        // no check to allow addition during resolution
        reifyXltC(X, C + 1, B.not());
    }

    /**
     * Posts one constraint that expresses : (x < y) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param B a boolean variable
     */
    @SuppressWarnings("SuspiciousNameCombination")
    default void reifyXltY(IntVar X, IntVar Y, BoolVar B) {
        if (X.isAConstant()) {
            reifyXgtC(Y, X.getValue(), B);
        } else if (Y.isAConstant()) {
            reifyXltC(X, Y.getValue(), B);
        } else {
            // no check to allow addition during resolution
            reifyXltYC(X, Y, 0, B);
        }
    }

    /**
     * Posts one constraint that expresses : (x > y) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param B a boolean variable
     */
    default void reifyXgtY(IntVar X, IntVar Y, BoolVar B) {
        reifyXltYC(X, Y, 1, B.not());
    }

    /**
     * Posts one constraint that expresses : (x &le; y) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param B a boolean variable
     */
    default void reifyXleY(IntVar X, IntVar Y, BoolVar B) {
        // no check to allow addition during resolution
        reifyXltYC(X, Y, 1, B);
    }

    /**
     * Posts one constraint that expresses : (x &ge; y) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param B a boolean variable
     */
    default void reifyXgeY(IntVar X, IntVar Y, BoolVar B) {
        // no check to allow addition during resolution
        reifyXltYC(X, Y, 0, B.not());
    }

    /**
     * Posts one constraint that expresses : (x < y + c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param C an int
     * @param B a boolean variable
     */
    @SuppressWarnings("SuspiciousNameCombination")
    default void reifyXltYC(IntVar X, IntVar Y, int C, BoolVar B) {
        // no check to allow addition during resolution
        if (X == Y) {
            if (C > 0) {
                ref().arithm(B, "=", 1).post();
            } else {
                ref().arithm(B, "=", 0).post();
            }
        } else if (X.isAConstant()) {
            reifyXgtC(Y, X.getValue() - C, B);
        } else if (Y.isAConstant()) {
            reifyXltC(X, Y.getValue() + C, B);
        } else {
            reifXrelYC(X, "<", Y, C, B);
        }
    }

    /**
     * Posts one constraint that expresses : (x > y + c) &hArr; b.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param Y an integer variable
     * @param C an int
     * @param B a boolean variable
     */
    default void reifyXgtYC(IntVar X, IntVar Y, int C, BoolVar B) {
        // no check to allow addition during resolution
        reifyXltYC(X, Y, C + 1, B.not());
    }

    /**
     * Posts one constraint that expresses : (X ∈ S) &hArr; B.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param S a set of values
     * @param B a boolean variable
     */
    default void reifyXinS(IntVar X, IntIterableRangeSet S, BoolVar B) {
        // no check to allow addition during resolution
        /* not obvious that this is more efficient -- see flatzinc/2020/skill_allocation+mzn_1m_1.fzn
        if (S.size() == 1) {
            reifyXeqC(X, S.min(), B);
        } else */
        {
            IntIterableRangeSet nS = S.duplicate().flip(X.getLB() - 1, X.getUB() + 1);
            ref().post(new Constraint(ConstraintsName.BASIC_REI,
                    new PropXinSHalfReif(X, S, B),
                    new PropXinSHalfReif(X, nS, B.not())));
        }
    }

    /**
     * Posts one constraint that expresses : (X &notin; S) &hArr; B.
     * Bypasses the reification system.
     *
     * @param X an integer variable
     * @param S a set of values
     * @param B a boolean variable
     */
    default void reifyXnotinS(IntVar X, IntIterableRangeSet S, BoolVar B) {
        // no check to allow addition during resolution
        /* not obvious that this is more efficient -- see flatzinc/2020/skill_allocation+mzn_1m_1.fzn
        if (S.size() == 1) {
            reifyXneC(X, S.min(), B);
        } else */{
            IntIterableRangeSet nS = S.duplicate().flip(X.getLB() - 1, X.getUB() + 1);
            ref().post(new Constraint(ConstraintsName.BASIC_REI,
                    new PropXinSHalfReif(X, S, B.not()),
                    new PropXinSHalfReif(X, nS, B)));
        }
    }


    /**
     * Posts one constraint that expresses : (X op C) &hArr; B.
     *
     * @param X  an integer variable
     * @param op an operator (allowed: =, !=, <, <=, >, >=)
     * @param C  an int
     * @param B  a boolean variable
     */
    default void reifXrelC(IntVar X, String op, int C, BoolVar B) {
        switch (Operator.get(op)) {
            case EQ:
                new Constraint(ConstraintsName.BASIC_REI,
                        new PropXeqCHalfReif(X, C, B),
                        new PropXneCHalfReif(X, C, B.not())
                ).post();
                break;
            case NQ:
                reifXrelC(X, "=", C, B.not());
                break;
            case LT:
                // x < c <-> r <=> x <= c - 1 <-> r
                reifXrelC(X, "<=", C - 1, B);
                break;
            case LE:
                // x <= c <-> r <=> x <= c <- r /\ x >= c + 1 <- !r
                new Constraint(ConstraintsName.BASIC_REI,
                        new PropXleCHalfReif(X, C, B),
                        new PropXgeCHalfReif(X, C + 1, B.not())
                ).post();
                break;
            case GT:
                // x > c <-> r <=> c + 1 <= x <-> r
                reifXrelC(X, ">=", C + 1, B);
                break;
            case GE:
                // x >= c <-> r <=> x >= c <- r /\ x <= c - 1 <- !r
                new Constraint(ConstraintsName.BASIC_REI,
                        new PropXgeCHalfReif(X, C, B),
                        new PropXleCHalfReif(X, C - 1, B.not())
                ).post();
                break;
            default:
                throw new SolverException("Unexpected operator: " + op);
        }
    }

    /**
     * Posts one constraint that expresses : (X op Y + C) &hArr; B.
     *
     * @param X  an integer variable
     * @param op an operator (allowed: =, !=, <, <=, >, >=)
     * @param Y  an integer variable
     * @param C  an int
     * @param B  a boolean variable
     */
    default void reifXrelYC(IntVar X, String op, IntVar Y, int C, BoolVar B) {
        if (Y.isInstantiated()) {
            reifXrelC(X, op, Y.getValue() + C, B);
            return;
        }
        switch (Operator.get(op)) {
            case EQ:
                new Constraint(ConstraintsName.BASIC_REI,
                        new PropXeqYHalfReif(X, ref().intView(1, Y, C), B),
                        new PropXneYHalfReif(X, ref().intView(1, Y, C), B.not())
                ).post();
                break;
            case NQ:
                reifXrelYC(X, "=", Y, C, B.not());
                break;
            case LT:
                // x < y + c <-> r <=> x <= y + c - 1 <-> r
                reifXrelYC(X, "<=", Y, C - 1, B);
                break;
            case LE:
                // x <= y + c <-> r <=> x <= y + c <- r /\ y <= x - c - 1 <- !r
                new Constraint(ConstraintsName.BASIC_REI,
                        new PropXleYHalfReif(X, ref().intView(1, Y, C), B),
                        new PropXleYHalfReif(Y, ref().intView(1, X, -C - 1), B.not())
                ).post();
                break;
            case GT:
                // x > y + c <-> r <=> y <= x - c - 1 <-> r
                reifXrelYC(Y, "<=", X, -C - 1, B);
                break;
            case GE:
                // x >= y + c <-> r <=> y <= x - c <-> r
                reifXrelYC(Y, "<=", X, -C, B);
                break;
            default:
                throw new SolverException("Unexpected operator: " + op);
        }
    }

    /**
     * Posts one constraint that expresses : (X op C) &lArr; B.
     *
     * @param X  an integer variable
     * @param op an operator (allowed: =, !=, <, <=, >, >=)
     * @param C  an int
     * @param B  a boolean variable
     */
    default void impXrelC(IntVar X, String op, int C, BoolVar B) {
        switch (Operator.get(op)) {
            case EQ:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXeqCHalfReif(X, C, B)
                ));
                break;
            case NQ:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXneCHalfReif(X, C, B)
                ));
                break;
            case LT:
                impXrelC(X, "<=", C - 1, B);
                break;
            case LE:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXleCHalfReif(X, C, B)
                ));
                break;
            case GT:
                impXrelC(X, ">=", C + 1, B);
                break;
            case GE:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXgeCHalfReif(X, C, B)
                ));
                break;
            default:
                throw new SolverException("Unexpected operator: " + op);
        }
    }

    /**
     * Posts one constraint that expresses : (X op Y + C) &lArr; B.
     *
     * @param X  an integer variable
     * @param op an operator (allowed: =, !=, <, <=, >, >=)
     * @param Y  an integer variable
     * @param C  an int
     * @param B  a boolean variable
     */
    default void impXrelYC(IntVar X, String op, IntVar Y, int C, BoolVar B) {
        if (Y.isInstantiated()) {
            impXrelC(X, op, Y.getValue() + C, B);
            return;
        }
        switch (Operator.get(op)) {
            case EQ:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXeqYHalfReif(X, ref().intView(1, Y, C), B)
                ));
                break;
            case NQ:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXneYHalfReif(X, ref().intView(1, Y, C), B)
                ));
                break;
            case LT:
                impXrelYC(X, "<=", Y, C - 1, B);
                break;
            case LE:
                ref().post(new Constraint(ConstraintsName.BASIC_REI,
                        new PropXleYHalfReif(X, ref().intView(1, Y, C), B)
                ));
                break;
            case GT:
                impXrelYC(Y, "<=", X, -C - 1, B);
                break;
            case GE:
                impXrelYC(Y, "<=", X, -C, B);
                break;
            default:
                throw new SolverException("Unexpected operator: " + op);
        }
    }


}