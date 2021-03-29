package org.chocosolver.util.objects.setDataStructures;

import java.util.BitSet;

/**
 * Set representing the union of a set of sets. This class relies on a dynamic iterator and does not
 * create new data structures. This set is read-only.
 *
 * @author Dimitri Justeau-Allaire
 * @since 29/03/2021
 */
public class SetUnion implements ISet {

    public ISet[] sets;
    protected SetUnionIterator iterator;

    public SetUnion(ISet... sets) {
        this.sets = sets;
        this.iterator = new SetUnionIterator(sets);
    }

    @Override
    public ISetIterator iterator() {
        iterator.reset();
        return iterator;
    }

    @Override
    public SetUnionIterator newIterator() {
        SetUnionIterator iter = new SetUnionIterator(sets);
        iter.reset();
        return iter;

    }

    @Override
    public boolean add(int element) {
        throw new UnsupportedOperationException("this set is read-only");
    }

    @Override
    public boolean remove(int element) {
        throw new UnsupportedOperationException("this set is read-only");
    }


    @Override
    public boolean contains(int element) {
        for (ISet set : sets) {
            if (set.contains(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        int size = 0;
        SetUnionIterator iter = newIterator();
        while (iter.hasNext()) {
            size++;
            iter.findNext();
        }
        return size;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("this set is read-only");
    }

    @Override
    public int min() {
        int minVal = Integer.MAX_VALUE;
        for (ISet set : sets) {
            int currMin = set.min();
            if (currMin < minVal) {
                minVal = currMin;
            }
        }
        return minVal;
    }

    @Override
    public int max() {
        int maxVal = Integer.MIN_VALUE;
        for (ISet set : sets) {
            int currMax = set.max();
            if (currMax > maxVal) {
                maxVal = currMax;
            }
        }
        return maxVal;
    }

    @Override
    public SetType getSetType() {
        return SetType.DYNAMIC;
    }

    private class SetUnionIterator implements ISetIterator {

        ISet[] sets;
        ISetIterator[] iterators;
        ISet checked;
        int currentSet;
        Integer next = null;

        SetUnionIterator(ISet... sets) {
            this.sets = sets;
            this.iterators = new ISetIterator[sets.length];
            for (int i = 0; i < sets.length; i++) {
                this.iterators[i] = sets[i].newIterator();
            }
            this.checked = SetFactory.makeRangeSet();
            this.currentSet = 0;
        }

        @Override
        public void reset() {
            this.checked.clear();
            this.currentSet = 0;
            for (ISetIterator iterator : iterators) {
                iterator.reset();
            }
            findNext();
        }

        protected void findNext() {
            next = null;
            while (!hasNext()) {
                while (!iterators[currentSet].hasNext()) {
                    currentSet++;
                    if (currentSet == sets.length) {
                        return;
                    }
                }
                int v = iterators[currentSet].next();
                if (!this.checked.contains(v)) {
                    this.checked.add(v);
                    next = v;
                }
            }
        }

        @Override
        public int nextInt() {
            int value = next;
            findNext();
            return value;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }
    }
}
