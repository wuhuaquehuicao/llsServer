package com.lssservlet.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RLexSortedSet;

public class LexSortSet {
    public interface KeySortSetListener {
        public void onKeySortSetAdd(SortSet set, String aItem);

        public void onKeySortSetRemove(SortSet set, String aItem);
    }

    protected static final Logger log = LogManager.getLogger(SortSet.class);
    private RLexSortedSet _set;
    private KeySortSetListener _listener;

    public interface ItemHandler {
        public boolean handle(String item);
    }

    public LexSortSet(RLexSortedSet set, KeySortSetListener listener) {
        _set = set;
        _listener = listener;
    }

    public String getName() {
        return _set.getName();
    }

    public int size() {
        return _set.size();
    }

    public void add(String key) {
        if (key == null)
            return;
        _set.add(key);
    }

    public boolean remove(String key) {
        if (key == null)
            return false;
        return _set.remove(key);
    }

    public void iterEachA(ItemHandler handler) {
        Collection<String> ret = _set.range(0, _set.size() - 1);
        for (String k : ret) {
            if (handler.handle(k))
                break;
        }
    }

    public void iterEachD(ItemHandler handler) {
        Collection<String> ret = _set.range(0, _set.size() - 1);
        ArrayList<String> results = new ArrayList<>(ret);
        for (int index = results.size() - 1; index >= 0; index--) {
            if (handler.handle(results.get(index)))
                break;
        }
    }

    public Collection<String> getCollections() {
        return _set.range(0, _set.size() - 1);
    }

    public boolean contains(String key) {
        return _set.contains(key);
    }

    public void clear() {
        _set.clear();
    }

    @Override
    public String toString() {
        return _set.toString();
    }
}
