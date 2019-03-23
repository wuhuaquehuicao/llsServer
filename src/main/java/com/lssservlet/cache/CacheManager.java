package com.lssservlet.cache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.MapOptions;
import org.redisson.api.RBitSet;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RGeo;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.Json;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.LexSortSet;
import com.lssservlet.utils.MapSet;
import com.lssservlet.utils.MapSet.KeySetListener;
import com.lssservlet.utils.SortSet;
import com.lssservlet.utils.SortSet.KeySortSetListener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class CacheManager implements KeySetListener<String>, KeySortSetListener, LexSortSet.KeySortSetListener,
        MessageListener<JsonObject> {

    private static volatile CacheManager sInstance = null;

    protected static final Logger log = LogManager.getLogger(CacheManager.class);
    protected static final Logger loglock = LogManager.getLogger("com.lssservlet.lock");

    private String redisPrefix = null;
    private Long createdTime = null;
    private RedissonClient client = null;
    private CacheMap<Long, JsonObject> noteList = null;
    private RTopic<JsonObject> topicRPC = null;
    private AtomicLong rpcId = new AtomicLong();

    private ConcurrentHashMap<String, MapSet<String>> setMap = new ConcurrentHashMap<String, MapSet<String>>();
    private ConcurrentHashMap<String, SortSet> sortSetMap = new ConcurrentHashMap<String, SortSet>();
    private ConcurrentHashMap<String, LexSortSet> lexSortSetMap = new ConcurrentHashMap<String, LexSortSet>();
    @SuppressWarnings("rawtypes")
    private ConcurrentHashMap<String, CacheMap> cacheMap = new ConcurrentHashMap<String, CacheMap>();
    private ConcurrentHashMap<String, Collection<Object>> rpcResult = new ConcurrentHashMap<String, Collection<Object>>();

    private CacheMap<String, ADSData> keyCacheMap = null;
    private CacheMap<String, ADSData> expiredCacheMap = null;

    public static class JCCodec implements org.redisson.client.codec.Codec {
        public enum JCCodecType {
            EMapValue, EMapKey, EValue;
        }

        @SuppressWarnings("hiding")
        public static class JCDecoder<Object> implements org.redisson.client.protocol.Decoder<Object> {
            private JCCodecType _type;

            public JCDecoder(JCCodecType type) {
                _type = type;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Object decode(ByteBuf buf, State state) throws IOException {
                try {
                    String s = buf.toString(Charset.forName("UTF-8"));
                    if (_type == JCCodecType.EMapKey) {
                        return (Object) s;
                    }
                    JsonObject obj = new JsonObject(s);
                    if (obj.containsKey(ADSDbKey.TBase.DATA_TYPE)) {
                        String t = obj.getString(ADSDbKey.TBase.DATA_TYPE);
                        return Json.mapper.readerFor(JCPersistence.getInstance().getModelClass(t)).readValue(s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        }

        public static class JCEncoder implements org.redisson.client.protocol.Encoder {
            private JCCodecType _type;

            public JCEncoder(JCCodecType type) {
                _type = type;
            }

            @Override
            public ByteBuf encode(Object in) throws IOException {
                ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
                if (_type == JCCodecType.EMapKey) {
                    out.writeBytes(((String) in).getBytes(Charset.forName("UTF-8")));
                } else
                    out.writeBytes(Json.encode(in).getBytes(Charset.forName("UTF-8")));
                return out;
            }
        }

        private JCDecoder<Object> _mapValueDecoder = new JCDecoder<Object>(JCCodecType.EMapValue);
        private JCEncoder _mapValueEncoder = new JCEncoder(JCCodecType.EMapValue);

        private JCDecoder<Object> _mapKeyDecoder = new JCDecoder<Object>(JCCodecType.EMapKey);
        private JCEncoder _mapKeyEncoder = new JCEncoder(JCCodecType.EMapKey);

        // private UFODecoder<Object> _valueDecoder = new UFODecoder<Object>(UFOCodecType.EValue);
        // private UFOEncoder _valueEncoder = new UFOEncoder(UFOCodecType.EValue);

        @Override
        public Decoder<Object> getMapValueDecoder() {
            return _mapValueDecoder;
        }

        @Override
        public Encoder getMapValueEncoder() {
            return _mapValueEncoder;
        }

        @Override
        public Decoder<Object> getMapKeyDecoder() {
            return _mapKeyDecoder;
        }

        @Override
        public Encoder getMapKeyEncoder() {
            return _mapKeyEncoder;
        }

        @Override
        public Decoder<Object> getValueDecoder() {
            return null;
        }

        @Override
        public Encoder getValueEncoder() {
            return null;
        }
    }

    public static class RedissonOnlineOfflineClosure {
        public void apply() {
            CacheManager.getInstance().listNode();
        }
    }

    public static class RedissonDataChangedClosure {
        public void apply(String cacheKey) {
            // JCData data = DataManager.getInstance().getCache(cacheKey);
            // TODO: may need send websocket message
        }
    }

    private JCCodec _codec = new JCCodec();
    private StringCodec _defaultCodec = new StringCodec();

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        if (sInstance == null) {
            synchronized (CacheManager.class) {
                if (sInstance == null) {
                    sInstance = new CacheManager();
                }
            }
        }

        return sInstance;
    }

    public void clear() {
        RKeys keys = client.getKeys();
        long count = keys.deleteByPattern(redisPrefix + ":*");
        if (count > 0) {
            log.warn("clear cache: {}", count);
        }
    }

    public void start() throws DataException {
        if (Config.getInstance().getRedisRootKey() != null)
            redisPrefix = Config.getInstance().getRedisRootKey();
        if (client != null)
            return;
        createdTime = DataManager.getInstance().time();
        JsonObject redisConfig = Config.getInstance().getRedisMasterConfig();
        if (redisConfig != null) {
            try {
                org.redisson.config.Config cfg = org.redisson.config.Config
                        .fromJSON(Config.getInstance().getRedisMasterConfig().toString());
                client = Redisson.create(cfg);
                noteList = getOrCreateCache("nodelist");
                updateNode();

                topicRPC = client.getTopic(redisPrefix + ":topic:" + "rpc");
                topicRPC.addListener(this);
                publishMessage(RedissonOnlineOfflineClosure.class.getName());
                return;
            } catch (Exception e) {
                log.warn("Redisson create error ", e);
            }
        }
        throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "cache config error");
    }

    @Override
    public void onMessage(String topic, JsonObject message) {
        if (message.containsKey("result")) {
            // reply
            String key = "_rpc_" + message.getLong("node") + "_" + message.getLong("id");
            if (message.getLong("node") == Config.getInstance().getServerId()) {
                synchronized (rpcResult) {
                    Collection<Object> result = rpcResult.get(key);
                    if (result == null) {
                        result = new ArrayList<Object>();
                        rpcResult.put(key, result);
                    }
                    result.add(message.getValue("result"));
                }
            }
            RCountDownLatch latch = client.getCountDownLatch(key);
            latch.countDown();
        } else if (message.containsKey("id") && message.containsKey("method") && message.containsKey("params")) {
            String method = message.getString("method");
            try {
                Method[] ms = Class.forName(method).getMethods();
                for (Method m : ms) {
                    if (m.getName().equals("apply")) {
                        JsonArray params = message.getJsonArray("params");
                        try {
                            Object ret = m.invoke(Class.forName(method).newInstance(), params.getList().toArray());
                            if (ret == null)
                                message.putNull("result");
                            else
                                message.put("result", ret);
                            topicRPC.publish(message);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Collection<Object> publishMessage(String method, Object... args) {
        if (method == null)
            return null;
        JsonArray params = new JsonArray();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                params.add(args[i]);
            }
        }
        JsonObject m = new JsonObject();
        m.put("method", method);
        m.put("params", params);
        m.put("id", rpcId.incrementAndGet());
        m.put("node", Config.getInstance().getServerId());

        String key = "_rpc_" + m.getLong("node") + "_" + m.getLong("id");
        RCountDownLatch latch = client.getCountDownLatch(key);
        latch.trySetCount(topicRPC.publish(m));
        try {
            log.info("publishMessage - await begin");
            latch.await();
            log.info("publishMessage - await finished");
            Collection<Object> result = rpcResult.get(key);
            rpcResult.remove(key);
            log.debug("rpc result:{}", result);
            return result;
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    public void updateNode() {
        noteList.put(Config.getInstance().getServerId(),
                new JsonObject().put("name", Config.getInstance().getServerName())
                        .put("timestamp", DataManager.getInstance().time()).put("createdtime", createdTime));
    }

    public void listNode() {
        if (noteList.size() > 0) {
            TimeZone tz = TimeZone.getTimeZone("Asia/Hong_Kong");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(tz);
            StringBuilder info = new StringBuilder();
            info.append("\r\nNode List:\r\n=====================================================================\r\n");
            noteList.forEach((k, v) -> {
                // name id ip local node
                info.append(v.getString("name") + " - " + k + " - " + sdf.format(new Date(v.getLong("createdtime")))
                        + "\r\n");
            });
            info.append("=====================================================================");
            log.info(info);
        }
    }

    public JsonArray getNodes() {
        JsonArray result = new JsonArray();
        noteList.forEach((k, v) -> {
            result.add(v);
        });
        return result;
    }

    public CacheMap<String, ADSData> getKeyCache() {
        if (keyCacheMap == null) {
            synchronized (rpcId) {
                if (keyCacheMap == null) {
                    MapOptions<String, ADSData> options = MapOptions.<String, ADSData>defaults()
                            .loader(CacheStore.getInstance());
                    keyCacheMap = new CacheMap<String, ADSData>(
                            client.getMap(redisPrefix + ":map:keyCache", _codec, options), 0);
                }
            }
        }
        return keyCacheMap;
    }

    public CacheMap<String, ADSData> getExpiredCache() {
        if (expiredCacheMap == null) {
            synchronized (rpcId) {
                if (expiredCacheMap == null) {
                    MapOptions<String, ADSData> options = MapOptions.<String, ADSData>defaults()
                            .loader(CacheStore.getInstance());
                    expiredCacheMap = new CacheMap<String, ADSData>(
                            client.getMapCache(redisPrefix + ":map:expireCache", _codec, options), 7200);
                }
            }
        }
        return expiredCacheMap;
    }

    @SuppressWarnings("unchecked")
    public <K, V> CacheMap<K, V> getOrCreateCache(String name) {
        if (name.equalsIgnoreCase("keyCache"))
            return (CacheMap<K, V>) getKeyCache();
        if (name.equalsIgnoreCase("expireCache"))
            return (CacheMap<K, V>) getExpiredCache();
        CacheMap<K, V> map = cacheMap.get(name);
        if (map == null) {
            if (name.equalsIgnoreCase("nodelist")) {
                if (name.equalsIgnoreCase("nodelist"))
                    map = new CacheMap<K, V>(client.getMap(redisPrefix + ":map:" + name), 20);
            } else
                map = new CacheMap<K, V>(client.getMap(redisPrefix + ":map:" + name, _defaultCodec), 0);
            cacheMap.putIfAbsent(name, map);
            map = cacheMap.get(name);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public <K, V> CacheMap<K, V> getOrCreateCache(String name, Long expire) {
        CacheMap<K, V> map = cacheMap.get(name);
        if (map == null) {
            map = new CacheMap<K, V>(client.getMap(redisPrefix + ":map:" + name, _defaultCodec), expire);
            cacheMap.putIfAbsent(name, map);
            map = cacheMap.get(name);
        }
        return map;
    }

    public SortSet getOrCreateSortSet(String name) {
        SortSet set = sortSetMap.get(name);
        if (set == null) {
            sortSetMap.putIfAbsent(name,
                    new SortSet(client.getScoredSortedSet(redisPrefix + ":sortset:" + name, _defaultCodec), this));
            set = sortSetMap.get(name);
        }
        return set;
    }

    public LexSortSet getOrCreateLexSortSet(String name) {
        LexSortSet set = lexSortSetMap.get(name);
        if (set == null) {
            lexSortSetMap.putIfAbsent(name,
                    new LexSortSet(client.getLexSortedSet(redisPrefix + ":lexsortset:" + name), this));
            set = lexSortSetMap.get(name);
        }

        return set;
    }

    public RGeo<String> getGeo(String name) {
        return client.getGeo(redisPrefix + ":geo:" + name);
    }

    public RBitSet getBitSet(String name) {
        return client.getBitSet(redisPrefix + ":bitset:" + name);
    }

    public MapSet<String> getOrCreateSet(String name) {
        MapSet<String> set = setMap.get(name);
        if (set == null) {
            setMap.putIfAbsent(name,
                    new MapSet<String>(client.getSet(redisPrefix + ":set:" + name, _defaultCodec), this));
            set = setMap.get(name);
        }
        return set;
    }

    public void stop() {
        if (topicRPC != null) {
            topicRPC.removeAllListeners();
            topicRPC = null;
        }

        if (noteList != null) {
            noteList.remove(Config.getInstance().getServerId());
            noteList = null;
        }

        publishMessage(RedissonOnlineOfflineClosure.class.getName());
        if (client != null)
            client.shutdown();
        client = null;
    }

    public Long incrementAndGet(String name) {
        return client.getAtomicLong(redisPrefix + ":number:" + name).incrementAndGet();
    }

    @Override
    public void onKeySetAdd(MapSet<String> set, String aItem) {

    }

    @Override
    public void onKeySetRemove(MapSet<String> set, String aItem) {

    }

    @Override
    public void onKeySortSetAdd(SortSet set, String aItem) {

    }

    @Override
    public void onKeySortSetRemove(SortSet set, String aItem) {

    }

    public <T, R> R reentrantLock(String name, T t, Function<T, R> handler) {
        loglock.debug("lockstatus: 1 {}", name);
        RLock lock = client.getLock(redisPrefix + ":lock:" + name);
        lock.lock();
        loglock.debug("lockstatus: 2 {}", name);
        try {
            return handler.apply(t);
        } finally {
            loglock.debug("lockstatus: 3 {}", name);
            lock.unlock();
            loglock.debug("lockstatus: 4 {}", name);
        }
    }

    public boolean tryLock(String cacheKey) {
        loglock.debug("lockstatus: 1 {}", cacheKey);
        RLock lock = client.getLock(redisPrefix + ":lock:" + cacheKey);
        boolean ret = lock.tryLock();
        if (ret)
            loglock.debug("lockstatus: 2 {}", cacheKey);
        else
            loglock.debug("lockstatus: -1 {}", cacheKey);
        return ret;
    }

    public boolean tryLock(String cacheKey, int timeout) {
        loglock.debug("lockstatus: 1 {} ,timeout {}", cacheKey, timeout);
        RLock lock = client.getLock(redisPrefix + ":lock:" + cacheKey);
        boolean ret = false;
        try {
            ret = lock.tryLock(timeout, TimeUnit.SECONDS);
            if (ret)
                loglock.debug("lockstatus: 2 {}", cacheKey);
            else
                loglock.debug("lockstatus: -1 {}", cacheKey);
        } catch (InterruptedException e) {
            loglock.debug("lockstatus: -2  {}", cacheKey);
        }
        return ret;
    }

    public void unlock(String cacheKey) {
        loglock.debug("lockstatus: 3 {}", cacheKey);
        RLock lock = client.getLock(redisPrefix + ":lock:" + cacheKey);
        if (lock != null) {
            if (lock.isLocked()) {
                lock.unlock();
            }
            loglock.debug("lockstatus: 4 {}", cacheKey);
        }
    }

    public boolean islock(String cacheKey) {
        RLock lock = client.getLock(redisPrefix + ":lock:" + cacheKey);
        if (lock != null) {
            return lock.isLocked();
        }
        return false;
    }
}
