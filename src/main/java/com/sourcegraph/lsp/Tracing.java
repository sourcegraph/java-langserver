package com.sourcegraph.lsp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.sourcegraph.common.Config;
import com.sourcegraph.lsp.domain.Mapper;
import com.sourcegraph.lsp.domain.Method;
import com.sourcegraph.lsp.domain.Request;
import io.opentracing.NoopSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Utility methods for tracing.
 *
 * Created by beyang on 2/21/17.
 */
public final class Tracing {

    private static final String TRACER_CONTEXT_ITEM = "TRACER_CONTEXT_ITEM";
    private static final String ROOT_SPAN_CONTEXT_ITEM = "ROOT_SPAN_CONTEXT_ITEM";

    public static @Nullable String lightstepLink(Span span) {
        if (span instanceof com.lightstep.tracer.shared.Span) {
            // sanitize access token from appearing in logs
            return ((com.lightstep.tracer.shared.Span) span).generateTraceURL().replace(Config.LIGHTSTEP_TOKEN,
                    Config.LIGHTSTEP_PROJECT);
        }
        return null;
    }

    /**
     * Sets the tracer in the context. This should only be called once per context.
     */
    public static void contextSetTracer(Map<String, Object> ctx, Tracer tracer) {
        if (tracer == null) {
            return;
        }
        ctx.put(TRACER_CONTEXT_ITEM, tracer);
    }

    /**
     * Returns the tracer in the context or null if it doesn't exist.
     */
    public static Tracer tracerFromContext(Map<String, Object> ctx) {
        Object item = ctx.get(TRACER_CONTEXT_ITEM);
        if (item instanceof Tracer) {
            return (Tracer)item;
        }
        return null;
    }

    /**
     * Set the root span in the context. This should only be called once, at the beginning of a request cycle.
     */
    public static void contextSetRootSpan(Map<String, Object> ctx, Span span) {
        if (span == null) {
            return;
        }
        ctx.put(ROOT_SPAN_CONTEXT_ITEM, span);
    }

    private static Span rootSpanFromContext(Map<String, Object> ctx) {
        Object item = ctx.get(ROOT_SPAN_CONTEXT_ITEM);
        if (item instanceof Span) {
            return (Span)item;
        }
        return NoopSpan.INSTANCE;
    }

    /**
     * Returns a new span using the tracer in the context (or null if no tracer is present). If the context has a
     * root span, the returned span will be a child of the root span. It is the caller's responsibility to call
     * `span.finish()`.
     */
    public static <P> Span startSpanFromContext(Map<String, Object> ctx, String name) {
        return startSpanFromContext(ctx, name, null, ImmutableMap.of());
    }

    public static <P> Span startSpanFromContext(Map<String, Object> ctx, String name, @Nullable Span parent) {
        return startSpanFromContext(ctx, name, parent, ImmutableMap.of());
    }

    private static <P> Span startSpanFromContext(Map<String, Object> ctx, String name, Span parent, Map<String, String> tags) {
        Tracer tracer = tracerFromContext(ctx);
        if (tracer == null) {
            return NoopSpan.INSTANCE;
        }

        Tracer.SpanBuilder span = tracer.buildSpan(name);
        if (parent == null) { // if parent is null, set root as parent
            Span root = rootSpanFromContext(ctx);
            if (root != null) {
                span = span.asChildOf(root);
            }
        } else {
            span = span.asChildOf(parent);
        }

        if (Config.LIGHTSTEP_INCLUDE_SENSITIVE) {
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                span = span.withTag(tag.getKey(), tag.getValue());
            }
        }

        return span.start();
    }

    /**
     * Returns a new span for the request. This should be called at the beginning of a request and typically the caller
     * should set the span in the context associated with the request cycle. It is the caller's responsibility to call
     * `span.finish()`.
     */
    public static <P> Span startSpanForRequest(Map<String, Object> ctx, Request<P> request) throws JsonProcessingException {
        Tracer tracer = tracerFromContext(ctx);
        if (tracer == null) {
            return NoopSpan.INSTANCE;
        }

        Tracer.SpanBuilder span = tracer.buildSpan("LSP lang server: " + request.getMethodAsString())
                .withTag("mode", "java");
        if (request.getMeta() != null) {
            // Try to get our parent span context from the JSON-RPC request from the LSP proxy.
            SpanContext parent = tracer.extract(Format.Builtin.TEXT_MAP,
                    new TextMapExtractAdapter(request.getMeta()));
            span = span.asChildOf(parent).withTag("span.kind", "server");
        }
        if (!Method.isFileSystemMethod(request.getMethod())) {
            Object params = request.getParams();
            if (params != null && Config.LIGHTSTEP_INCLUDE_SENSITIVE) {
                span = span.withTag("params", Mapper.getObjectMapper().writeValueAsString(params));
            }
        }

        return span.start();
    }

    public static void endSpan(Span span) {
        if (span != null) {
            span.finish();
        }
    }

    public static void endSpan(Span span, String tagName, Number tagValue) {
        if (span != null) {
            span.setTag(tagName, tagValue);
            span.finish();
        }
    }
}
