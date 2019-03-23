package com.lssservlet.utils;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RScoredSortedSet;

public class SortSet {
    public interface KeySortSetListener {
        public void onKeySortSetAdd(SortSet set, String aItem);

        public void onKeySortSetRemove(SortSet set, String aItem);
    }

    protected static final Logger log = LogManager.getLogger(SortSet.class);
    private RScoredSortedSet<String> _set;
    private KeySortSetListener _listener;

    public interface ItemHandler {
        Boolean handle(String item);
    }

    public SortSet(RScoredSortedSet<String> set, KeySortSetListener listener) {
        _set = set;
        _listener = listener;
    }

    public String getName() {
        return _set.getName();
    }

    public int size() {
        return _set.size();
    }

    public void add(String key, long value) {
        if (key == null)
            return;
        _set.add(value, key);
    }

    public Long incr(String key, long value) {
        if (key == null)
            return -1l;
        return _set.addScore(key, value).longValue();
    }

    public boolean remove(String key) {
        if (key == null)
            return false;
        return _set.remove(key);
    }

    public void forEachA(ItemHandler handler) {
        Collection<String> ret = _set.valueRange(0, _set.size() - 1);
        for (String k : ret) {
            if (handler.handle(k))
                break;
        }
    }

    public void forEachD(ItemHandler handler) {
        Collection<String> ret = _set.valueRangeReversed(0, _set.size() - 1);
        for (String k : ret) {
            if (handler.handle(k))
                break;
        }
    }

    public void clear() {
        _set.clear();
    }

    @Override
    public String toString() {
        return _set.toString();
    }
}
