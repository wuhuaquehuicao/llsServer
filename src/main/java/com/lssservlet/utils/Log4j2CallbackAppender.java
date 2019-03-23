package com.lssservlet.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.CyclicBuffer;

import com.lssservlet.core.DataManager;

/**
 * Created by ramon on 4/12/17.
 */
@Plugin(name = "Callback", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4j2CallbackAppender extends AbstractAppender {
    private static final int DEFAULT_BUFFER_SIZE = 64;
    private final CyclicBuffer<LogEvent> buffer;

    private Log4j2CallbackAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, int bufferSize) {
        super(name, filter, layout, ignoreExceptions);
        buffer = new CyclicBuffer<>(LogEvent.class, bufferSize);
    }

    @PluginFactory
    public static Log4j2CallbackAppender createAppender(@PluginConfiguration final Configuration config,
            @PluginAttribute("name") final String name, @PluginAttribute("bufferSize") final String bufferSizeStr,
            @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filter") Filter filter,
            @PluginAttribute("ignoreExceptions") final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for SmtpAppender");
            return null;
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final int bufferSize = bufferSizeStr == null ? DEFAULT_BUFFER_SIZE : Integer.parseInt(bufferSizeStr);

        if (layout == null) {
            layout = HtmlLayout.createDefaultLayout();
        }
        if (filter == null) {
            filter = ThresholdFilter.createFilter(null, null, null);
        }
        // final Configuration configuration = config != null ? config : new DefaultConfiguration();
        return new Log4j2CallbackAppender(name, filter, layout, ignoreExceptions, bufferSize);
    }

    @Override
    public boolean isFiltered(final LogEvent event) {
        final boolean filtered = super.isFiltered(event);
        if (filtered) {
            buffer.add(event);
        }
        return filtered;
    }

    protected byte[] formatContentToBytes(final LogEvent[] priorEvents, final LogEvent appendEvent,
            final Layout<?> layout) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        writeContent(priorEvents, appendEvent, layout, raw);
        return raw.toByteArray();
    }

    private void writeContent(final LogEvent[] priorEvents, final LogEvent appendEvent, final Layout<?> layout,
            final ByteArrayOutputStream out) throws IOException {
        writeHeader(layout, out);
        writeBuffer(priorEvents, appendEvent, layout, out);
        writeFooter(layout, out);
    }

    protected void writeHeader(final Layout<?> layout, final OutputStream out) throws IOException {
        final byte[] header = layout.getHeader();
        if (header != null) {
            out.write(header);
        }
    }

    protected void writeBuffer(final LogEvent[] priorEvents, final LogEvent appendEvent, final Layout<?> layout,
            final OutputStream out) throws IOException {
        for (final LogEvent priorEvent : priorEvents) {
            final byte[] bytes = layout.toByteArray(priorEvent);
            out.write(bytes);
        }

        final byte[] bytes = layout.toByteArray(appendEvent);
        out.write(bytes);
    }

    protected void writeFooter(final Layout<?> layout, final OutputStream out) throws IOException {
        final byte[] footer = layout.getFooter();
        if (footer != null) {
            out.write(footer);
        }
    }

    @Override
    public void append(final LogEvent event) {
        Layout<?> layout = getLayout();
        try {
            final LogEvent[] priorEvents = buffer.removeAll();
            final byte[] rawBytes = formatContentToBytes(priorEvents, event, layout);
            if (rawBytes != null && rawBytes.length > 0) {
                String content = new String(rawBytes, Charset.forName("UTF-8"));
                if (content != null)
                    DataManager.getInstance().onLogEvent(event, content.trim());
            }
        } catch (final IOException | RuntimeException e) {
            throw new LoggingException("Error occurred while notification", e);
        }
    }
}
