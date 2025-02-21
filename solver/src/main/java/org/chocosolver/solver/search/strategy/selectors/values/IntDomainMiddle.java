/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.selectors.values;

import org.chocosolver.solver.variables.IntVar;

import java.util.function.ToDoubleFunction;

/**
 * Selects the value in the variable domain closest to the mean of its current bounds.
 * <br/>
 * It computes the middle value of the domain. Then it checks if the mean is contained in the domain.
 * If not, the closest value to the middle is chosen. It uses a policy to define whether the mean value should
 * be floored or ceiled
 * <br/>
 * <p>
 * BEWARE: should not be used with assignment decisions over bounded variables (because the decision negation
 * would result in no inference)
 *
 * @author Charles Prud'homme, Jean-Guillaume Fages
 * @since 2 juil. 2010
 */
public class IntDomainMiddle implements IntValueSelector {

    // VARIABLES
    public final static boolean FLOOR = true;
    private final static ToDoubleFunction<IntVar> MIDEDEF = var -> (var.getLB() + var.getUB()) / 2.;
    private final boolean roundingPolicy;
    private final ToDoubleFunction<IntVar> middle;

    /**
     * Selects the middle value
     *
     * @param roundingPolicy should be either FLOOR or !FLOOR (ceil)
     */
    public IntDomainMiddle(boolean roundingPolicy) {
        this(MIDEDEF, roundingPolicy);
    }

    public IntDomainMiddle(ToDoubleFunction<IntVar> middle, boolean roudingPolicy) {
        this.middle = middle;
        this.roundingPolicy = roudingPolicy;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("PointlessBooleanExpression")
    @Override
    public int selectValue(IntVar var) {
        int value;
        if (var.hasEnumeratedDomain()) {
            int min = var.getLB();
            int max = var.getUB();
            if (roundingPolicy == FLOOR) {
                value = min + (max - min ) / 2;
            } else {
                value = min + (max - min + 1) / 2;
            }
        } else {
            if (roundingPolicy == FLOOR) {
                value = var.getLB();
            } else {
                value = var.getUB();
            }
        }
        return value;
    }
}
