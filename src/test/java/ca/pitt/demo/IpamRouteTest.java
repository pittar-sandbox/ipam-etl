package ca.pitt.demo;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class IpamRouteTest {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    public void testBlueCatParsing() throws Exception {
        AdviceWith.adviceWith(camelContext, "parse-bluecat", route -> {
            route.weaveByToUri("direct:persist").replace().to("mock:persist");
        });

        MockEndpoint mock = camelContext.getEndpoint("mock:persist", MockEndpoint.class);
        mock.reset();
        mock.setExpectedMessageCount(1);

        String blueCatCsv = "action,name,ttl,type,rdata,comment\n" +
                "add,app-db-01.corp.db,3600,A,172.20.10.10,Primary Application Database";
        
        producerTemplate.sendBodyAndHeader("direct:parseBlueCat", blueCatCsv, "CamelFileParent", "bluecat");

        mock.assertIsSatisfied();
        List records = mock.getExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testInfobloxParsing() throws Exception {
        AdviceWith.adviceWith(camelContext, "parse-infoblox", route -> {
             route.weaveByToUri("direct:persist").replace().to("mock:persist");
        });

        MockEndpoint mock = camelContext.getEndpoint("mock:persist", MockEndpoint.class);
        mock.reset();
        mock.setExpectedMessageCount(1);

        String infobloxData = "HEADER-NETWORK,network,EA-Location,EA-Owner,comment\n" +
                "NETWORK,10.10.10.0/24,BLD-A-FLR1,IT-Corp,Main Office - First Floor Data\n" +
                "\n" +
                "HEADER-HOSTRECORD,fqdn*,address*,comment\n" +
                "HOSTRECORD,phone-101.voip.local,10.50.0.5,Reception Phone";

        producerTemplate.sendBodyAndHeader("direct:parseInfoblox", infobloxData, "CamelFileParent", "infoblox");

        mock.assertIsSatisfied();
        List records = mock.getExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(records);
        // Should have 2 records (1 network, 1 host)
        assertEquals(2, records.size());
    }

     @Test
    public void testOtherParsing() throws Exception {
         AdviceWith.adviceWith(camelContext, "parse-other", route -> {
             route.weaveByToUri("direct:persist").replace().to("mock:persist");
        });

        MockEndpoint mock = camelContext.getEndpoint("mock:persist", MockEndpoint.class);
        mock.reset();
        mock.setExpectedMessageCount(1);

        String otherCsv = "IPAddress,HostName,Status,MACAddress,Description\n" +
                "10.10.1.25,corp-dc01.corp.local,Assigned,00:50:56:A0:00:01,Primary Domain Controller";

        producerTemplate.sendBodyAndHeader("direct:parseOther", otherCsv, "CamelFileParent", "other");

        mock.assertIsSatisfied();
        List records = mock.getExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(records);
        assertEquals(1, records.size());
    }
}