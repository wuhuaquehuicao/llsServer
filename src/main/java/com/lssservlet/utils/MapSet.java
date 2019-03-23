package com.lssservlet.utils;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.redisson.api.RSet;

public class MapSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    public interface KeySetListener<E> {
        public void onKeySetAdd(MapSet<E> set, E aItem);

        public void onKeySetRemove(MapSet<E> set, E aItem);
    }

    static final long serialVersionUID = 1l;

    private transient RSet<E> map;

    private String name = null;

    private KeySetListener<E> _listener = null;

    public MapSet(RSet<E> set, KeySetListener<E> listener) {
        map = set;
        name = set.getName();
        _listener = listener;
    }

    public String getName() {
        return name;
    }

    public Iterator<E> iterator() {
        return map.iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.contains(o);
    }

    public boolean add(E e) {
        boolean ret = map.add(e);
        if (ret && _listener != null) {
            _listener.onKeySetAdd(this, e);
        }
        return ret;
    }

    public boolean remove(Object o) {
        boolean ret = map.remove(o);
        if (ret && _listener != null) {
            _listener.onKeySetRemove(this, (E) o);
        }
        return ret;
    }

    public void clear() {
        if (_listener == null)
            map.clear();
        else {
            map.forEach(k -> {
                _listener.onKeySetRemove(this, (E) k);
            });
            map.clear();
        }
    }

    public List<E> toList() {
        ArrayList<E> list = new ArrayList<E>();
        forEach(e -> {
            list.add(e);
        });
        return list;
    }
}
