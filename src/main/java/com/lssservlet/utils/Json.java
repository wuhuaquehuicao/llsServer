
/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package com.lssservlet.utils;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@SuppressWarnings("unchecked")
public class Json {
    public static class JsonObjectMapper extends ObjectMapper {
        public JsonGenerator createGenerator(OutputStream out) throws IOException {
            return _jsonFactory.createGenerator(out, JsonEncoding.UTF8);
        }
    }

    public static JsonObjectMapper mapper = new JsonObjectMapper();
    public static JsonObjectMapper prettyMapper = new JsonObjectMapper();
    public static JsonObjectMapper nonNullMapper = new JsonObjectMapper();
    protected static final Logger log = LogManager.getLogger(Json.class);

    public static class JsonBeanPropertyWriter extends BeanPropertyWriter {
        private static final long serialVersionUID = 1L;

        public JsonBeanPropertyWriter(BeanPropertyWriter base, String newFieldName) {
            super(base, new SerializedString(newFieldName));
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            // code from BeanPropertyWriter serializeAsField
            SerializableString name = _name;
            Object value = (_accessorMethod == null) ? _field.get(bean) : _accessorMethod.invoke(bean);
            if (bean instanceof JsonData) {
                String n = ((JsonData) bean).getSerializerName(this);
                if (n == null)
                    return;
                value = ((JsonData) bean).getSerializerValue(this);
                name = new SerializedString(n);
            }
            // Null handling is bit different, check that first
            if (value == null) {
                if (_nullSerializer != null) {
                    gen.writeFieldName(name);
                    _nullSerializer.serialize(null, gen, prov);
                }
                return;
            }
            // then find serializer to use
            JsonSerializer<Object> ser = _serializer;
            if (ser == null) {
                Class<?> cls = value.getClass();
                PropertySerializerMap m = _dynamicSerializers;
                ser = m.serializerFor(cls);
                if (ser == null) {
                    ser = _findAndAddDynamic(m, cls, prov);
                }
            }
            // and then see if we must suppress certain values (default, empty)
            if (_suppressableValue != null) {
                if (MARKER_FOR_EMPTY == _suppressableValue) {
                    if (ser.isEmpty(prov, value)) {
                        return;
                    }
                } else if (_suppressableValue.equals(value)) {
                    return;
                }
            }
            // For non-nulls: simple check for direct cycles
            if (value == bean) {
                // three choices: exception; handled by call; or pass-through
                if (_handleSelfReference(bean, gen, prov, ser)) {
                    return;
                }
            }
            gen.writeFieldName(name);
            if (_typeSerializer == null) {
                ser.serialize(value, gen, prov);
            } else {
                ser.serializeWithType(value, gen, prov, _typeSerializer);
            }
        }
    }

    public static class JsonData {
        protected void beforeDeserializerData() {

        }

        protected Map<String, Object> afterSerializerData(Map<String, Object> data) {
            return data;
        }

        protected String getSerializerName(JsonBeanPropertyWriter writer) {
            return writer.getName();
        }

        protected Object getSerializerValue(JsonBeanPropertyWriter writer) throws Exception {
            return writer.get(this);
        }
    }

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        prettyMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        prettyMapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        nonNullMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        nonNullMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        nonNullMapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        SimpleModule module = new SimpleModule();
        module.addSerializer(JsonObject.class, new JsonObjectSerializer());
        module.addSerializer(JsonArray.class, new JsonArraySerializer());
        module.addSerializer(HashSet.class, new HashSetSerializer());
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                    JsonDeserializer<?> deserializer) {
                if (JsonData.class.isAssignableFrom(beanDesc.getBeanClass())) {
                    return new JsonDataDeserializer((JsonDeserializer<JsonData>) deserializer);
                }
                return deserializer;
            }

            // @Override
            // public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
            // BeanDeserializerBuilder builder) {
            // ArrayList<SettableBeanProperty> pl = new ArrayList<SettableBeanProperty>();
            // builder.getProperties().forEachRemaining(p -> {
            // pl.add(p);
            // });
            // return builder;
            // }
        });

        module.setSerializerModifier(new BeanSerializerModifier() {
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                    JsonSerializer<?> serializer) {
                if (JsonData.class.isAssignableFrom(beanDesc.getBeanClass()))
                    return new JsonDataSerializer((JsonSerializer<JsonData>) serializer);
                return super.modifySerializer(config, beanDesc, serializer);
            }

            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                    List<BeanPropertyWriter> beanProperties) {
                ArrayList<BeanPropertyWriter> nl = new ArrayList<BeanPropertyWriter>();
                for (BeanPropertyWriter bpw : beanProperties) {
                    nl.add(new JsonBeanPropertyWriter(bpw, bpw.getName()));
                }
                return super.orderProperties(config, beanDesc, nl);
            }
        });

        mapper.registerModule(module);
        nonNullMapper.registerModule(module);
        prettyMapper.registerModule(module);
    }

    public static JsonObject jsonEncode(Object obj) {
        try {
            return new JsonObject(encode(obj));
        } catch (Exception e) {
            log.warn("jsonEncode error", e);
        }
        return null;
    }

    public static String encode(Object obj) throws RuntimeException {
        try {
            if (obj == null)
                return null;
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode as JSON: " + e.getMessage());
        }
    }

    public static String encodePretty(Object obj) throws RuntimeException {
        try {
            return prettyMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode as JSON: " + e.getMessage());
        }
    }

    public static String encodeNonNull(Object obj) throws RuntimeException {
        try {
            return nonNullMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode as JSON: " + e.getMessage());
        }
    }

    public static JsonObject jsonEncodeNonNull(Object obj) {
        try {
            return new JsonObject(nonNullMapper.writeValueAsString(obj));
        } catch (Exception e) {
            log.warn("jsonEncodeNonNull error", e);
        }
        return null;
    }

    public static <T> T decodeValue(T data, String str) throws RuntimeException {
        try {
            return mapper.readerForUpdating(data).readValue(str);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode:" + e.getMessage());
        }
    }

    public static <T> T decodeValue(String str, Class<T> clazz) throws RuntimeException {
        try {
            return mapper.readValue(str, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode:" + e.getMessage());
        }
    }

    public static <T> List<T> decodeListValue(String str, Class<T> clazz) throws RuntimeException {
        try {
            return mapper.readValue(str, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode:" + e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    static Object checkAndCopy(Object val, boolean copy) {
        if (val == null) {
            // OK
        } else if (val instanceof Number && !(val instanceof BigDecimal)) {
            // OK
        } else if (val instanceof Boolean) {
            // OK
        } else if (val instanceof String) {
            // OK
        } else if (val instanceof Character) {
            // OK
        } else if (val instanceof CharSequence) {
            val = val.toString();
        } else if (val instanceof JsonObject) {
            if (copy) {
                val = ((JsonObject) val).copy();
            }
        } else if (val instanceof JsonArray) {
            if (copy) {
                val = ((JsonArray) val).copy();
            }
        } else if (val instanceof Map) {
            if (copy) {
                val = (new JsonObject((Map) val)).copy();
            } else {
                val = new JsonObject((Map) val);
            }
        } else if (val instanceof List) {
            if (copy) {
                val = (new JsonArray((List) val)).copy();
            } else {
                val = new JsonArray((List) val);
            }
        } else if (val instanceof Set) {
            if (copy) {
                val = (new JsonArray((Set) val)).copy();
            } else {
                val = new JsonArray((Set) val);
            }
        } else if (val instanceof byte[]) {
            val = Base64.getEncoder().encodeToString((byte[]) val);
        } else if (val instanceof Instant) {
            val = ISO_INSTANT.format((Instant) val);
        } else {
            throw new IllegalStateException("Illegal type in JsonObject: " + val.getClass());
        }
        return val;
    }

    static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private static class HashSetSerializer extends JsonSerializer<HashSet> {
        @Override
        public void serialize(HashSet value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            ArrayList<Object> v = new ArrayList<Object>();
            value.forEach(action -> {
                v.add(action);
            });
            v.sort(null);
            jgen.writeObject(v);
        }
    }

    private static class JsonObjectSerializer extends JsonSerializer<JsonObject> {
        @Override
        public void serialize(JsonObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.getMap());
        }
    }

    private static class JsonArraySerializer extends JsonSerializer<JsonArray> {
        @Override
        public void serialize(JsonArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.getList());
        }
    }

    private static class JsonDataDeserializer extends StdDeserializer<JsonData> implements ResolvableDeserializer {
        private static final long serialVersionUID = 1L;
        private final JsonDeserializer<JsonData> defaultDeserializer;

        JsonDataDeserializer(JsonDeserializer<JsonData> defaultDeserializer) {
            super(JsonData.class);
            this.defaultDeserializer = defaultDeserializer;
        }

        @Override
        public JsonData deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonData obj = defaultDeserializer.deserialize(jp, ctxt);
            if (obj != null) {
                obj.beforeDeserializerData();
                return obj;
            }
            return obj;
        }

        @Override
        public JsonData deserialize(JsonParser p, DeserializationContext ctxt, JsonData intoValue)
                throws IOException, JsonProcessingException {
            JsonData obj = defaultDeserializer.deserialize(p, ctxt, intoValue);
            if (obj != null) {
                obj.beforeDeserializerData();
                return obj;
            }
            return obj;
        }

        @Override
        public void resolve(DeserializationContext ctxt) throws JsonMappingException {
            ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
        }
    }

    private static class JsonDataSerializer extends StdSerializer<JsonData> implements ResolvableSerializer {
        private static final long serialVersionUID = 1L;
        private final JsonSerializer<JsonData> defaultSerializer;

        JsonDataSerializer(JsonSerializer<JsonData> serializer) {
            super(JsonData.class);
            this.defaultSerializer = serializer;
        }

        @Override
        public void resolve(SerializerProvider provider) throws JsonMappingException {
            ((ResolvableSerializer) defaultSerializer).resolve(provider);
        }

        @Override
        public void serialize(JsonData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (gen.getCodec() instanceof JsonObjectMapper) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                JsonObjectMapper mapper = (JsonObjectMapper) gen.getCodec();
                JsonGenerator tmp = mapper.createGenerator(outStream);
                if (tmp != null) {
                    defaultSerializer.serialize(value, tmp, provider);
                    tmp.close();
                }
                if (outStream.size() > 0) {
                    String result = new String(outStream.toByteArray(), "UTF-8");
                    JsonObject d = new JsonObject(result);
                    Map<String, Object> nd = value.afterSerializerData(d.getMap());
                    gen.writeObject(nd);
                    return;
                }
            }
            defaultSerializer.serialize(value, gen, provider);
        }
    }
}
