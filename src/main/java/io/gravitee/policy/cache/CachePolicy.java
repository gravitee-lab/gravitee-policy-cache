/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.cache;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.*;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.BodyPart;
import io.gravitee.gateway.api.http.StringBodyPart;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.cache.configuration.CachePolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class CachePolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachePolicy.class);

    static Map<String, CacheElement> cache = new HashMap<>();

    /**
     * Cache policy configuration
     */
    private final CachePolicyConfiguration cachePolicyConfiguration;

    private static char KEY_SEPARATOR = '_';

    public CachePolicy(final CachePolicyConfiguration cachePolicyConfiguration) {
        this.cachePolicyConfiguration = cachePolicyConfiguration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (request.method() == HttpMethod.GET ||
                request.method() == HttpMethod.OPTIONS ||
                request.method() == HttpMethod.HEAD) {

            // Override the invoker for safe request to cache content (if required)
            Invoker defaultInvoker = (Invoker) executionContext.getAttribute(ExecutionContext.ATTR_INVOKER);
            executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, new CacheInvoker(defaultInvoker));
        } else {
            LOGGER.debug("Request {} is not a safe request, disable caching for it.", request.id());
        }

        policyChain.doNext(request, response);
    }

    class CacheInvoker implements Invoker {

        private final Invoker invoker;

        CacheInvoker(final Invoker invoker) {
            this.invoker = invoker;
        }

        @Override
        public ClientRequest invoke(ExecutionContext executionContext, Request serverRequest, Handler<ClientResponse> handler) {
            // Here we have to check if there is already a value in cache
            String cacheId = hash(serverRequest, executionContext);
            LOGGER.debug("Looking for element in cache with the key {}", cacheId);

            if (cache.containsKey(cacheId)) {
                LOGGER.debug("An element has been found for key {}, returning the cached response to the initial client", cacheId);

                // Ok, there is a value for this request in cache
                ClientRequest noOpClientRequest = new ClientRequest() {
                    @Override
                    public ClientRequest connectTimeoutHandler(Handler<Throwable> handler) {
                        return this;
                    }

                    @Override
                    public ClientRequest write(BodyPart bodyPart) {
                        return this;
                    }

                    @Override
                    public void end() {
                        CacheElement element = cache.get(cacheId);
                        CacheClientResponse response = new CacheClientResponse(element);

                        boolean hasContent = (element.getContent() != null && ! element.getContent().isEmpty());

                        handler.handle(response);

                        if (hasContent) {
                            response.bodyHandler.handle(new StringBodyPart(element.getContent()));
                        }

                        response.endHandler.handle(null);
                    }
                };

                serverRequest
                        .bodyHandler(noOpClientRequest::write)
                        .endHandler(endResult -> noOpClientRequest.end());

                return noOpClientRequest;
            } else {
                LOGGER.debug("No element for key {}, invoke upstream with invoker {}", cacheId, invoker.getClass().getName());

                // No value, let's to the default invocation and cache result in response
                return invoker.invoke(executionContext, serverRequest, clientResponse -> {
                    CacheElement element = new CacheElement();

                    handler.handle(new ClientResponse() {
                        final StringBuilder content = new StringBuilder();

                        @Override
                        public int status() {
                            element.setStatus(clientResponse.status());
                            return clientResponse.status();
                        }

                        @Override
                        public HttpHeaders headers() {
                            element.setHeaders(clientResponse.headers());
                            return clientResponse.headers();
                        }

                        @Override
                        public ReadStream<BodyPart> bodyHandler(Handler<BodyPart> handler1) {
                            return clientResponse.bodyHandler(bodyPart -> {
                                content.append(new String(bodyPart.getBodyPartAsBytes()));
                                handler1.handle(bodyPart);
                            });
                        }

                        @Override
                        public ReadStream<BodyPart> endHandler(Handler<Void> handler1) {

                            return clientResponse.endHandler(result -> {
                                // Do not put content if not a success status code
                                if (element.getStatus() >= 200 && element.getStatus() < 300) {
                                    element.setContent(content.toString());
                                    LOGGER.debug("Put response in cache for key {} and request {}", cacheId, serverRequest.id());
                                    cache.put(cacheId, element);
                                } else {
                                    LOGGER.debug("Response for key {} not put in cache because of the status code {}",
                                            cacheId, element.getStatus());
                                }
                                handler1.handle(result);
                            });
                        }
                    });
                });
            }
        }
    }

    class CacheClientResponse implements ClientResponse {
        private Handler<BodyPart> bodyHandler;
        private Handler<Void> endHandler;

        private final CacheElement cacheElement;

        CacheClientResponse(final CacheElement cacheElement) {
            this.cacheElement = cacheElement;
        }

        @Override
        public int status() {
            return cacheElement.getStatus();
        }

        @Override
        public HttpHeaders headers() {
            return cacheElement.getHeaders();
        }

        @Override
        public ClientResponse bodyHandler(Handler<BodyPart> bodyPartHandler) {
            this.bodyHandler = bodyPartHandler;
            return this;
        }

        @Override
        public ClientResponse endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }
    }

    /**
     * Generate a unique identifier for the cache key.
     *
     * @param request
     * @param executionContext
     * @return
     */
    public String hash(Request request, ExecutionContext executionContext) {
        StringBuilder sb = new StringBuilder();
        switch (cachePolicyConfiguration.getScope()) {
            case APPLICATION:
                sb.append(executionContext.getAttribute(ExecutionContext.ATTR_API)).append(KEY_SEPARATOR);
                sb.append(executionContext.getAttribute(ExecutionContext.ATTR_APPLICATION)).append(KEY_SEPARATOR);
                break;
            case API:
                sb.append(executionContext.getAttribute(ExecutionContext.ATTR_API)).append(KEY_SEPARATOR);
                break;
        }

        sb.append(request.path().hashCode()).append(KEY_SEPARATOR);

        String key = cachePolicyConfiguration.getKey();
        if (key != null && ! key.isEmpty()) {
            key = executionContext.getTemplateEngine().convert(key);
            sb.append(key);
        } else {
            // Remove latest separator
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }
}
