package ca.pitt.demo;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@ApplicationScoped
public class IpamRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Polling Route
        // Recursive polling, noop=true to avoid deleting/moving files during dev/test
        from("file:src/main/resources/samples?recursive=true&noop=true&include=.*.csv&delay=5000")
            .routeId("ipam-poller")
            .log("Processing file: ${header.CamelFilePath}")
            .choice()
                .when(header("CamelFileParent").contains("bluecat"))
                    .to("direct:parseBlueCat")
                .when(header("CamelFileParent").contains("infoblox"))
                    .to("direct:parseInfoblox")
                .when(header("CamelFileParent").contains("other"))
                    .to("direct:parseOther")
                .otherwise()
                    .log("WARN: Unknown file format/location: ${header.CamelFilePath}")
            .end();

        // BlueCat Parser
        from("direct:parseBlueCat")
            .routeId("parse-bluecat")
            .log("Parsing BlueCat format")
            .unmarshal().csv()
            .process(exchange -> {
                List<List<String>> csvData = exchange.getIn().getBody(List.class);
                List<IpamRecord> records = new ArrayList<>();
                
                boolean first = true;
                for (List<String> row : csvData) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    if (row.size() < 5) continue;

                    String action = row.get(0);
                    String name = row.get(1);
                    String type = row.get(3);
                    String rdata = row.get(4);
                    // String comment = row.size() > 5 ? row.get(5) : null;

                    if ("delete".equalsIgnoreCase(action)) continue;
                    
                    if ("A".equalsIgnoreCase(type) || "AAAA".equalsIgnoreCase(type)) {
                        IpamRecord record = new IpamRecord();
                        record.ipAddress = rdata;
                        record.hostName = name;
                        record.status = "Assigned";
                        record.sourceSystem = "BlueCat";
                        records.add(record);
                    }
                }
                exchange.getMessage().setBody(records);
            })
            .to("direct:persist");

        // Infoblox Parser
        from("direct:parseInfoblox")
            .routeId("parse-infoblox")
            .log("Parsing Infoblox format")
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                List<IpamRecord> records = new ArrayList<>();
                String[] lines = body.split("\\R"); // split by line separator

                for (String line : lines) {
                    if (line.trim().isEmpty() || line.startsWith("HEADER-")) continue;

                    String[] parts = line.split(",", -1);
                    if (parts.length < 2) continue;

                    String type = parts[0];
                    
                    if ("NETWORK".equalsIgnoreCase(type)) {
                        // 2 | NETWORK,10.10.10.0/24,BLD-A-FLR1,IT-Corp,Main Office - First Floor Data
                        // We map this to a record representing the Subnet itself
                        IpamRecord record = new IpamRecord();
                        record.subnetCidr = parts[1];
                        if (parts.length > 3) record.ownerTeam = parts[3];
                        record.sourceSystem = "Infoblox-Network";
                        record.originalRecord = line;
                        records.add(record);

                    } else if ("HOSTRECORD".equalsIgnoreCase(type)) {
                        // 5 | HOSTRECORD,phone-101.voip.local,10.50.0.5,Reception Phone
                        IpamRecord record = new IpamRecord();
                        record.hostName = parts[1];
                        record.ipAddress = parts[2];
                        record.sourceSystem = "Infoblox-Host";
                        record.originalRecord = line;
                        records.add(record);
                    }
                }
                exchange.getMessage().setBody(records);
            })
            .to("direct:persist");

        // Other Parser
        from("direct:parseOther")
            .routeId("parse-other")
            .log("Parsing Other format")
            .unmarshal().csv()
            .process(exchange -> {
                List<List<String>> csvData = exchange.getIn().getBody(List.class);
                List<IpamRecord> records = new ArrayList<>();
                
                boolean first = true;
                for (List<String> row : csvData) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    if (row.size() < 4) continue;

                    // 1 | IPAddress,HostName,Status,MACAddress,Description
                    String ip = row.get(0);
                    String host = row.get(1);
                    String status = row.get(2);
                    String mac = row.get(3);
                    
                    IpamRecord record = new IpamRecord();
                    record.ipAddress = ip;
                    record.hostName = host;
                    record.status = status;
                    record.macAddress = mac;
                    record.sourceSystem = "Other";
                    records.add(record);
                }
                exchange.getMessage().setBody(records);
            })
            .to("direct:persist");

        // Persist Route Placeholder
        from("direct:persist")
            .routeId("persist-mongo")
            .log("Persisting ${body.size()} records to MongoDB")
            .marshal().json()
            .to("{{ipam.output.endpoint}}");
    }
}