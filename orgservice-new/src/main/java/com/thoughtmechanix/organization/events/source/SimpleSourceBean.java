package com.thoughtmechanix.organization.events.source;

import com.thoughtmechanix.organization.events.models.OrganizationChangeModel;
import com.thoughtmechanix.organization.utils.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
public class SimpleSourceBean {
    private Source source;

    private static final Logger logger = LoggerFactory.getLogger(SimpleSourceBean.class);

    @Autowired
    Tracer tracer;

    @Autowired
    public SimpleSourceBean(Source source){
        this.source = source;
    }

    public void publishOrgChange(String action,String orgId){
       logger.debug("Sending Kafka message {} for Organization Id: {}", action, orgId);
       Span newSpan = tracer.createSpan("publishOrgChange");
       try {
           OrganizationChangeModel change =  new OrganizationChangeModel(
                   OrganizationChangeModel.class.getTypeName(),
                   action,
                   orgId,
                   UserContext.getCorrelationId());

           source.output().send(MessageBuilder.withPayload(change).build());
       }finally {
           newSpan.tag("peer.service", "kafka_message");
           newSpan.logEvent(org.springframework.cloud.sleuth.Span.CLIENT_RECV);
           tracer.close(newSpan);
       }
    }
}
