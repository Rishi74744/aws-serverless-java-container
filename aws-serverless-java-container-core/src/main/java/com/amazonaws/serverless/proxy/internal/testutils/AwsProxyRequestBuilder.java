/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal.testutils;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.ApiGatewayAuthorizerContext;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestContext;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestIdentity;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.CognitoAuthorizerClaims;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;


/**
 * Request builder object. This is used by unit proxy to quickly create an AWS_PROXY request object
 */
public class AwsProxyRequestBuilder {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private AwsProxyRequest request;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsProxyRequestBuilder() {
        this(null, null);
    }


    public AwsProxyRequestBuilder(String path) {
        this(path, null);
    }


    public AwsProxyRequestBuilder(String path, String httpMethod) {

        this.request = new AwsProxyRequest();
        this.request.setMultiValueHeaders(new MultiValuedTreeMap<>(String.CASE_INSENSITIVE_ORDER)); // avoid NPE
        this.request.setHttpMethod(httpMethod);
        this.request.setPath(path);
        this.request.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
        this.request.setRequestContext(new ApiGatewayRequestContext());
        this.request.getRequestContext().setRequestId(UUID.randomUUID().toString());
        this.request.getRequestContext().setExtendedRequestId(UUID.randomUUID().toString());
        this.request.getRequestContext().setStage("test");
        this.request.getRequestContext().setProtocol("HTTP/1.1");
        this.request.getRequestContext().setRequestTimeEpoch(System.currentTimeMillis());
        ApiGatewayRequestIdentity identity = new ApiGatewayRequestIdentity();
        identity.setSourceIp("127.0.0.1");
        this.request.getRequestContext().setIdentity(identity);
    }


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public AwsProxyRequestBuilder stage(String stageName) {
        this.request.getRequestContext().setStage(stageName);
        return this;
    }

    public AwsProxyRequestBuilder method(String httpMethod) {
        this.request.setHttpMethod(httpMethod);
        return this;
    }


    public AwsProxyRequestBuilder path(String path) {
        this.request.setPath(path);
        return this;
    }


    public AwsProxyRequestBuilder json() {
        return this.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }


    public AwsProxyRequestBuilder form(String key, String value) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new MultiValuedTreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }
        request.getMultiValueHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        String body = request.getBody();
        if (body == null) {
            body = "";
        }
        body += (body.equals("")?"":"&") + key + "=" + value;
        request.setBody(body);
        return this;
    }


    public AwsProxyRequestBuilder header(String key, String value) {
        if (this.request.getMultiValueHeaders() == null) {
            this.request.setMultiValueHeaders(new MultiValuedTreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }

        this.request.getMultiValueHeaders().add(key, value);
        return this;
    }


    public AwsProxyRequestBuilder queryString(String key, String value) {
        if (this.request.getMultiValueQueryStringParameters() == null) {
            this.request.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
        }

        this.request.getMultiValueQueryStringParameters().add(key, value);
        return this;
    }


    public AwsProxyRequestBuilder body(String body) {
        this.request.setBody(body);
        return this;
    }

    public AwsProxyRequestBuilder nullBody() {
        this.request.setBody(null);
        return this;
    }

    public AwsProxyRequestBuilder body(Object body) {
        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON)) {
            try {
                return body(LambdaContainerHandler.getObjectMapper().writeValueAsString(body));
            } catch (JsonProcessingException e) {
                throw new UnsupportedOperationException("Could not serialize object: " + e.getMessage());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported content type in request");
        }
    }

    public AwsProxyRequestBuilder binaryBody(InputStream is)
            throws IOException {
        this.request.setIsBase64Encoded(true);
        return body(Base64.getMimeEncoder().encodeToString(IOUtils.toByteArray(is)));
    }


    public AwsProxyRequestBuilder authorizerPrincipal(String principal) {
        if (this.request.getRequestContext().getAuthorizer() == null) {
            this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        }
        this.request.getRequestContext().getAuthorizer().setPrincipalId(principal);
        if (this.request.getRequestContext().getAuthorizer().getClaims() == null) {
            this.request.getRequestContext().getAuthorizer().setClaims(new CognitoAuthorizerClaims());
        }
        this.request.getRequestContext().getAuthorizer().getClaims().setSubject(principal);
        return this;
    }

    public AwsProxyRequestBuilder authorizerContextValue(String key, String value) {
        if (this.request.getRequestContext().getAuthorizer() == null) {
            this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        }
        this.request.getRequestContext().getAuthorizer().setContextValue(key, value);
        return this;
    }


    public AwsProxyRequestBuilder cognitoUserPool(String identityId) {
        this.request.getRequestContext().getIdentity().setCognitoAuthenticationType("POOL");
        this.request.getRequestContext().getIdentity().setCognitoIdentityId(identityId);
        if (this.request.getRequestContext().getAuthorizer() == null) {
            this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        }
        this.request.getRequestContext().getAuthorizer().setClaims(new CognitoAuthorizerClaims());
        this.request.getRequestContext().getAuthorizer().getClaims().setSubject(identityId);

        return this;
    }

    public AwsProxyRequestBuilder claim(String claim, String value) {
        this.request.getRequestContext().getAuthorizer().getClaims().setClaim(claim, value);

        return this;
    }


    public AwsProxyRequestBuilder cognitoIdentity(String identityId, String identityPoolId) {
        this.request.getRequestContext().getIdentity().setCognitoAuthenticationType("IDENTITY");
        this.request.getRequestContext().getIdentity().setCognitoIdentityId(identityId);
        this.request.getRequestContext().getIdentity().setCognitoIdentityPoolId(identityPoolId);
        return this;
    }


    public AwsProxyRequestBuilder cookie(String name, String value) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new MultiValuedTreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }

        String cookies = request.getMultiValueHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookies == null) {
            cookies = "";
        }

        cookies += (cookies.equals("")?"":"; ") + name + "=" + value;
        request.getMultiValueHeaders().putSingle(HttpHeaders.COOKIE, cookies);
        return this;
    }

    public AwsProxyRequestBuilder scheme(String scheme) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new MultiValuedTreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }

        request.getMultiValueHeaders().putSingle("CloudFront-Forwarded-Proto", scheme);
        return this;
    }

    public AwsProxyRequestBuilder serverName(String serverName) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new MultiValuedTreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }

        request.getMultiValueHeaders().putSingle("Host", serverName);
        return this;
    }

    public AwsProxyRequestBuilder userAgent(String agent) {
        if (request.getRequestContext() == null) {
            request.setRequestContext(new ApiGatewayRequestContext());
        }
        if (request.getRequestContext().getIdentity() == null) {
            request.getRequestContext().setIdentity(new ApiGatewayRequestIdentity());
        }

        request.getRequestContext().getIdentity().setUserAgent(agent);
        return this;
    }

    public AwsProxyRequestBuilder referer(String referer) {
        if (request.getRequestContext() == null) {
            request.setRequestContext(new ApiGatewayRequestContext());
        }
        if (request.getRequestContext().getIdentity() == null) {
            request.getRequestContext().setIdentity(new ApiGatewayRequestIdentity());
        }

        request.getRequestContext().getIdentity().setCaller(referer);
        return this;
    }

    public AwsProxyRequestBuilder fromJsonString(String jsonContent)
            throws IOException {
        request = LambdaContainerHandler.getObjectMapper().readValue(jsonContent, AwsProxyRequest.class);
        return this;
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public AwsProxyRequestBuilder fromJsonPath(String filePath)
            throws IOException {
        request = LambdaContainerHandler.getObjectMapper().readValue(new File(filePath), AwsProxyRequest.class);
        return this;
    }

    public AwsProxyRequest build() {
        return this.request;
    }

    public InputStream buildStream() {
        try {
            String requestJson = LambdaContainerHandler.getObjectMapper().writeValueAsString(request);
            return new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
