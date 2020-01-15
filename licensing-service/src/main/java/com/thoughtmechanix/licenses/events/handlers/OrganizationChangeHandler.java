package com.thoughtmechanix.licenses.events.handlers;

import com.thoughtmechanix.licenses.events.CustomChannels;
import com.thoughtmechanix.licenses.events.models.OrganizationChangeModel;
import com.thoughtmechanix.licenses.repository.OrganizationRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;


@EnableBinding(CustomChannels.class)
public class OrganizationChangeHandler {

    @Autowired
    private Tracer tracer;

    @Autowired
    private OrganizationRedisRepository organizationRedisRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrganizationChangeHandler.class);

    @StreamListener("inboundOrgChanges")
    public void loggerSink(OrganizationChangeModel orgChange) {
        Span newSpan = tracer.createSpan("inboundOrgChanges");
        logger.debug("Received a message of type " + orgChange.getType());
        try {
            switch (orgChange.getAction()) {
                case "GET":
                    logger.debug("Received a GET event from the organization service for organization id {}", orgChange.getOrganizationId());
                    break;
                case "SAVE":
                    logger.debug("Received a SAVE event from the organization service for organization id {}", orgChange.getOrganizationId());
                    break;
                case "UPDATE":
                    logger.debug("Received a UPDATE event from the organization service for organization id {}", orgChange.getOrganizationId());
                    organizationRedisRepository.deleteOrganization(orgChange.getOrganizationId());
                    break;
                case "DELETE":
                    logger.debug("Received a DELETE event from the organization service for organization id {}", orgChange.getOrganizationId());
                    organizationRedisRepository.deleteOrganization(orgChange.getOrganizationId());
                    break;
                default:
                    logger.error("Received an UNKNOWN event from the organization service of type {}", orgChange.getType());
                    break;

            }
        }finally {
            newSpan.tag("peer.kafka", "kafka");
            newSpan.logEvent(org.springframework.cloud.sleuth.Span.CLIENT_RECV);
            tracer.close(newSpan);
        }
    }

    @StreamListener("inboundNewOrganizationChanges")
    public void loggerSink2(OrganizationChangeModel orgChange) {
        Span newSpan = tracer.createSpan("inboundNewOrganizationChanges");
        logger.debug("Received a new message of type " + orgChange.getType());
        try {
            switch (orgChange.getAction()) {
                case "GET":
                    logger.debug("Received a GET event from  NEW organization service for organization id {}", orgChange.getOrganizationId());
                    break;
                case "SAVE":
                    logger.debug("Received a SAVE event from NEW organization service for organization id {}", orgChange.getOrganizationId());
                    break;
                case "UPDATE":
                    logger.debug("Received a UPDATE event from NEW organization service for organization id {}", orgChange.getOrganizationId());
                    organizationRedisRepository.deleteOrganization(orgChange.getOrganizationId());
                    break;
                case "DELETE":
                    logger.debug("Received a DELETE event from NEW organization service for organization id {}", orgChange.getOrganizationId());
                    organizationRedisRepository.deleteOrganization(orgChange.getOrganizationId());
                    break;
                default:
                    logger.error("Received an UNKNOWN event from NEW organization service of type {}", orgChange.getType());
                    break;

            }
        }finally {
            newSpan.tag("peer.kafka", "kafka");
            newSpan.logEvent(org.springframework.cloud.sleuth.Span.CLIENT_RECV);
            tracer.close(newSpan);
        }
    }
}
