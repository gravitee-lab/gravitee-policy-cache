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
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class CachePolicy {

    static Map<String, CacheElement> cache = new HashMap<>();

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (request.method() == HttpMethod.GET ||
                request.method() == HttpMethod.OPTIONS ||
                request.method() == HttpMethod.HEAD) {

            // Override the invoker for safe request to cache content (if required)
            Invoker defaultInvoker = (Invoker) executionContext.getAttribute(ExecutionContext.ATTR_INVOKER);
            executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, new CacheInvoker(defaultInvoker));
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
            String cacheId = hash(serverRequest);

            if (cache.containsKey(cacheId)) {
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
                // No value, let's to the default invocation and cache result in response
                return invoker.invoke(executionContext, serverRequest, new Handler<ClientResponse>() {
                    @Override
                    public void handle(ClientResponse clientResponse) {
                        System.out.println("Receive a response from invoker");
                        CacheElement element = new CacheElement();

                        handler.handle(new ClientResponse() {

                            private StringBuilder content = new StringBuilder();

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
                            public ClientResponse bodyHandler(Handler<BodyPart> handler) {
                                return clientResponse.bodyHandler(new Handler<BodyPart>() {
                                    @Override
                                    public void handle(BodyPart bodyPart) {
                                        content.append(new String(bodyPart.getBodyPartAsBytes()));
                                        handler.handle(bodyPart);
                                    }
                                });
                            }

                            @Override
                            public ClientResponse endHandler(Handler<Void> handler) {

                                return clientResponse.endHandler(new Handler<Void>() {
                                    @Override
                                    public void handle(Void result) {
                                        element.setContent(content.toString());
                                        cache.put(cacheId, element);
                                        handler.handle(result);
                                    }
                                });
                            }
                        });
                    }
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

    static String hash(Request request) {
        return Integer.toString(request.path().hashCode());
    }
}
