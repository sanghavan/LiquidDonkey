/*
 * The MIT License
 *
 * Copyright 2015 cain.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.http.retryhandler;

import com.github.horrorho.liquiddonkey.exception.FatalException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggressive HttpRequestRetryHandler implementation.
 *
 * @author cain
 */
@Immutable
@ThreadSafe
public final class PersistentHttpRequestRetryHandler implements HttpRequestRetryHandler {

    private static final Logger logger = LoggerFactory.getLogger(PersistentHttpRequestRetryHandler.class);

    private final int retryCount;
    private final int retryDelayMs;
    private final int timeOutMs;
    private final boolean requestSentRetryEnabled;

    public PersistentHttpRequestRetryHandler(
            int retryCount,
            int retryDelayMs,
            int timeOutMs,
            boolean requestSentRetryEnabled) {

        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.timeOutMs = timeOutMs;
        this.requestSentRetryEnabled = requestSentRetryEnabled;
    }

    @Override
    public boolean retryRequest(
            final IOException exception,
            final int executionCount,
            final HttpContext context) {
        logger.trace("<< retryRequest() < exception: {} executionCount: {} context: {}",
                exception, executionCount, context);

        boolean toRetry = doRetryRequest(exception, executionCount, context);
        logger.trace(">> retryRequest() > {}", toRetry);
        return toRetry;
    }

    private boolean doRetryRequest(
            final IOException exception,
            final int executionCount,
            final HttpContext context) {

        if (Thread.currentThread().isInterrupted()) {
            throw new FatalException("Interruped.");
        }

        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();

        if (request instanceof HttpExecutionAware && (((HttpExecutionAware) request).isAborted())) {
            logger.debug("-- retryRequest() > {} {} > false (aborted)", request.getRequestLine(), exception.toString());
            return false;
        }

        if (executionCount > retryCount) {
            logger.debug("-- retryRequest() > {} {} > false (limit)", request.getRequestLine(), exception.toString());
            return false;
        }

        if (exception instanceof SSLException) {
            logger.debug("-- retryRequest() > {} {} > false (SSLException)",
                    request.getRequestLine(), exception.toString());
            return false;
        }

        if (exception instanceof UnknownHostException && retryCount > 3) {
            logger.trace("-- retryRequest() > {} {} > UnknownHostException sleep({})",
                    request.getRequestLine(), exception.toString(), retryDelayMs);
            sleep(timeOutMs);
        }

        if (retryCount > 3) {
            logger.trace("-- retryRequest() > {} {} > sleep({})",
                    request.getRequestLine(), exception.toString(), retryDelayMs);
            sleep(timeOutMs);
        }

        if (!(request instanceof HttpEntityEnclosingRequest)) {
            logger.debug("-- retryRequest() > {} {} > true (idempotent)",
                    request.getRequestLine(), exception.toString());
            return true;
        }

        if (!clientContext.isRequestSent() || requestSentRetryEnabled) {
            logger.debug("-- retryRequest() > {} {} > true (non-idempotent)",
                    request.getRequestLine(), exception.toString());
            return true;
        }

        logger.debug("-- retryRequest() : {} >> {} > false (non-idempotent)",
                request.getRequestLine(), exception.toString());
        return false;
    }

    private void sleep(int timeMs) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeMs);
        } catch (InterruptedException ex) {
            throw new FatalException("Interrupted.", ex);
        }
    }
}