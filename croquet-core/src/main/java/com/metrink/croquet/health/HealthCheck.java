package com.metrink.croquet.health;

import java.io.IOException;

import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health Check.
 */
public class HealthCheck extends AbstractResource {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheck.class);
    private static final int DEFAULT_SC = 200;

    /**
     * Get the body of the health check.
     * @return the body
     */
    protected String getBody() {
        return "SUCCESS";
    }

    /**
     * Get the status code of the health check.
     * @return the status code
     */
    protected int getStatusCode() {
        return DEFAULT_SC;
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        final ResourceResponse response = new ResourceResponse();
        final String body = getBody();

        response.setContentType("text/plain");
        response.setTextEncoding("utf-8");
        response.setCacheDuration(Duration.NONE);
        response.setContentLength(body.length());

        final int sc = getStatusCode();
        if (sc != DEFAULT_SC) {
            response.setError(sc);
        }

        response.setWriteCallback(new WriteCallback() {
            @Override
            public void writeData(final Attributes attributes) throws IOException {
                attributes.getResponse().write(body);
            }
        });

        return response;
    }

}
