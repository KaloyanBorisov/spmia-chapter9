package com.thoughtmechanix.zuulsvr.filters;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.thoughtmechanix.zuulsvr.model.AbTestingRoute;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.thoughtmechanix.zuulsvr.filters.FilterUtils.AUTH_TOKEN;

@Component
public class SpecialRoutesFilter extends ZuulFilter {
    private static final int FILTER_ORDER =  1;
    private static final boolean SHOULD_FILTER = true;

    @Autowired
    FilterUtils filterUtils;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Tracer tracer;

    @Override
    public String filterType() {
        return filterUtils.ROUTE_FILTER_TYPE;
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return SHOULD_FILTER;
    }

    private ProxyRequestHelper helper = new ProxyRequestHelper();

    private AbTestingRoute getAbRoutingInfo(String serviceName){
        System.out.println("getAbRoutingInfo()");
        Span span = tracer.createSpan("getAbRoutingInfo");
        ResponseEntity<AbTestingRoute> restExchange = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(FilterUtils.CORRELATION_ID, filterUtils.getCorrelationId());
            headers.add(FilterUtils.AUTH_TOKEN, filterUtils.getAuthToken());
            HttpEntity<String> requestEntity = new HttpEntity<String>(headers);

            restExchange = restTemplate.exchange(
                             "http://specialroutesservice/v1/route/abtesting/{serviceName}",
                             HttpMethod.GET,
                             requestEntity,
                             AbTestingRoute.class,
                             serviceName);
        }
        catch(HttpClientErrorException ex){
            if (ex.getStatusCode()== HttpStatus.NOT_FOUND) return null;
            //throw ex;
            ex.printStackTrace();
        }finally{
            span.tag("peer.service", "specialroutesservice");
            span.logEvent(org.springframework.cloud.sleuth.Span.CLIENT_RECV);
            tracer.close(span);
        }
        return restExchange.getBody();
    }

    private String buildRouteString(String oldEndpoint, String newEndpoint, String serviceName){
        System.out.println("buildRouteString()");
        int index = oldEndpoint.indexOf(serviceName);

        String strippedRoute = oldEndpoint.substring(index + serviceName.length());
        System.out.println("Target route: " + String.format("%s/%s", newEndpoint, strippedRoute));
        return String.format("%s/%s", newEndpoint, strippedRoute);
    }

    private String getVerb(HttpServletRequest request) {
        System.out.println("getVerb(HttpServletRequest request)");
        String sMethod = request.getMethod();
        return sMethod.toUpperCase();
    }

    private HttpHost getHttpHost(URL host) {
        System.out.println("getHttpHost(URL host)");
        HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(),
                host.getProtocol());
        return httpHost;
    }

    private Header[] convertHeaders(MultiValueMap<String, String> headers) {
        System.out.println("convertHeaders(MultiValueMap<String, String> headers)");
        List<Header> list = new ArrayList<>();
        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                list.add(new BasicHeader(name, value));
            }
        }
        return list.toArray(new BasicHeader[0]);
    }

    private HttpResponse forwardRequest(HttpClient httpclient, HttpHost httpHost,
                                        HttpRequest httpRequest) throws IOException {
        System.out.println("forwardRequest()");
        return httpclient.execute(httpHost, httpRequest);
    }


    private MultiValueMap<String, String> revertHeaders(Header[] headers) {
        System.out.println("revertHeaders(Header[] headers)");
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        for (Header header : headers) {
            String name = header.getName();
            if (!map.containsKey(name)) {
                map.put(name, new ArrayList<String>());
            }
            map.get(name).add(header.getValue());
        }
        return map;
    }

    private InputStream getRequestBody(HttpServletRequest request) {
        System.out.println("getRequestBody(HttpServletRequest request)");
        InputStream requestEntity = null;
        try {
            requestEntity = request.getInputStream();
        }
        catch (IOException ex) {
            // no requestBody is ok.
        }
        return requestEntity;
    }

    private void setResponse(HttpResponse response) throws IOException {
        System.out.println("setResponse(HttpResponse response)");
        this.helper.setResponse(response.getStatusLine().getStatusCode(),
                response.getEntity() == null ? null : response.getEntity().getContent(),
                revertHeaders(response.getAllHeaders()));
    }

    private HttpResponse forward(HttpClient httpclient, String verb, String uri,
                                 HttpServletRequest request, MultiValueMap<String, String> headers,
                                 MultiValueMap<String, String> params, InputStream requestEntity)
            throws Exception {
        System.out.println("HttpResponse forward()");
        Map<String, Object> info = this.helper.debug(verb, uri, headers, params,
                requestEntity);
        URL host = new URL( uri );
        HttpHost httpHost = getHttpHost(host);

        HttpRequest httpRequest;
        int contentLength = request.getContentLength();
        InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength,
                request.getContentType() != null
                        ? ContentType.create(request.getContentType()) : null);
        switch (verb.toUpperCase()) {
            case "POST":
                HttpPost httpPost = new HttpPost(uri);
                httpRequest = httpPost;
                httpPost.setEntity(entity);
                break;
            case "PUT":
                HttpPut httpPut = new HttpPut(uri);
                httpRequest = httpPut;
                httpPut.setEntity(entity);
                break;
            case "PATCH":
                HttpPatch httpPatch = new HttpPatch(uri );
                httpRequest = httpPatch;
                httpPatch.setEntity(entity);
                break;
            default:
                httpRequest = new BasicHttpRequest(verb, uri);
        }
        try {
            httpRequest.setHeaders(convertHeaders(headers));
            HttpResponse zuulResponse = forwardRequest(httpclient, httpHost, httpRequest);

            return zuulResponse;
        }
        finally {
        }
    }

    public boolean useSpecialRoute(AbTestingRoute testRoute){
        System.out.println("useSpecialRoute(AbTestingRoute testRoute)");
        Random random = new Random();

        if (testRoute.getActive().equals("N")) return false;

        int value = random.nextInt((10 - 1) + 1) + 1;

        if (testRoute.getWeight()<value) return true;

        return false;
    }

    @Override
    public Object run() {
        System.out.println("run()");
        RequestContext ctx = RequestContext.getCurrentContext();

        AbTestingRoute abTestRoute = getAbRoutingInfo( filterUtils.getServiceId() );

        if (abTestRoute!=null && useSpecialRoute(abTestRoute)) {
            String route = buildRouteString(ctx.getRequest().getRequestURI(),
                    abTestRoute.getEndpoint(),
                    ctx.get("serviceId").toString());
            forwardToSpecialRoute(route);
        }

        return null;
    }

    private void forwardToSpecialRoute(String route) {
        System.out.println("forwardToSpecialRoute(String route)");
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        MultiValueMap<String, String> headers = this.helper
                .buildZuulRequestHeaders(request);
        MultiValueMap<String, String> params = this.helper
                .buildZuulRequestQueryParams(request);
        String verb = getVerb(request);
        InputStream requestEntity = getRequestBody(request);
        if (request.getContentLength() < 0) {
            context.setChunkedRequestBody();
        }

        this.helper.addIgnoredHeaders();
        CloseableHttpClient httpClient = null;
        HttpResponse response = null;

        try {
            httpClient  = HttpClients.createDefault();
            response = forward(httpClient, verb, route, request, headers,
                    params, requestEntity);
            setResponse(response);
        }
        catch (Exception ex ) {
            ex.printStackTrace();

        }
        finally{
            try {
                httpClient.close();
            }
            catch(IOException ex){}
        }
    }
}
