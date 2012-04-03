/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package solver.propagation.generator;

import choco.kernel.common.util.tools.ArrayUtils;
import solver.exception.ContradictionException;
import solver.propagation.ISchedulable;
import solver.recorders.IEventRecorder;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

/**
 * A specific propagation engine that works like a list, each element has a fixed index.
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 15/12/11
 * @revision 04/03/12 change schedule
 */
public final class Sort<S extends ISchedulable> extends PropagationStrategy<S> {

    protected Comparator<S> comparator;
    protected S lastPopped;

    protected S[] elements;
    protected BitSet toPropagate;
    protected boolean init = false;

    public Sort(Generator<S>... generators) {
        this(null, generators);
    }

    @SuppressWarnings({"unchecked"})
    public Sort(Comparator<S> comparator, Generator<S>... generators) {
        this.elements = (S[]) new ISchedulable[0];
        for (int i = 0; i < generators.length; i++) {
            Generator gen = generators[i];
            elements = ArrayUtils.append(elements, (S[]) gen.getElements());
        }
        this.comparator = comparator;
        if (comparator != null) {
            Arrays.sort(elements, comparator);
        }
        for (int e = 0; e < elements.length; e++) {
            elements[e].setScheduler(this, e);
        }
        this.toPropagate = new BitSet(elements.length);
    }


    @Override
    public S[] getElements() {
        return (S[]) new ISchedulable[]{this};
    }

    //<-- PROPAGATION ENGINE
    @Override
    public void schedule(ISchedulable element) {
        // CONDITION: the element must not be already present (checked in element)
        int idx = element.getIndexInScheduler();
        toPropagate.set(idx);
        element.enqueue();
        if (!enqueued) {
            scheduler.schedule(this);
        }
    }

    @Override
    public void remove(ISchedulable element) {
        element.deque();
        int idx = element.getIndexInScheduler();
        toPropagate.clear(idx);
    }


    @Override
    protected boolean _pickOne() throws ContradictionException {
        if (!toPropagate.isEmpty()) {
            int idx = toPropagate.nextSetBit(0);
            toPropagate.clear(idx);
            lastPopped = elements[idx];
            lastPopped.deque();
            if (!lastPopped.execute()) {
                schedule(lastPopped);
            }
        }
        return toPropagate.isEmpty();
    }

    @Override
    protected boolean _sweepUp() throws ContradictionException {
        int idx = toPropagate.nextSetBit(0);
        while (idx >= 0) {
            toPropagate.clear(idx);
            lastPopped = elements[idx];
            lastPopped.deque();
            if (!lastPopped.execute()) {
                schedule(lastPopped);
            }
            idx = toPropagate.nextSetBit(idx + 1);
        }
        return toPropagate.isEmpty();
    }

    protected boolean _loopOut() throws ContradictionException {
        int idx = toPropagate.nextSetBit(0);
        while (!toPropagate.isEmpty()) {
            toPropagate.clear(idx);
            lastPopped = elements[idx];
            lastPopped.deque();
            if (!lastPopped.execute()) {
                schedule(lastPopped);
            }
            idx = toPropagate.nextSetBit(idx + 1);
            if (idx == -1) {
                idx = toPropagate.nextSetBit(0);
            }
        }
        return true;
    }

    protected boolean _clearOut() throws ContradictionException {
        while (!toPropagate.isEmpty()) {
            int idx = toPropagate.nextSetBit(0);
            toPropagate.clear(idx);
            lastPopped = elements[idx];
            lastPopped.deque();
            if (!lastPopped.execute()) {
                schedule(lastPopped);
            }
        }
        return true;
    }

    @Override
    public void flush() {
        if (lastPopped != null) {
            lastPopped.flush();
        }
        for (int i = toPropagate.nextSetBit(0); i >= 0; i = toPropagate.nextSetBit(i)) {
            toPropagate.clear(i);
            lastPopped = elements[i];
            if (IEventRecorder.LAZY) {
                lastPopped.flush();
            }
            lastPopped.deque();
        }
    }

    @Override
    public boolean isEmpty() {
        return toPropagate.isEmpty();
    }

    @Override
    public int size() {
        return toPropagate.cardinality();
    }

    //-->
}
