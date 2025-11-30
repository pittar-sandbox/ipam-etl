package ca.pitt.demo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

@ApplicationScoped
@Named("ipamProcessor")
public class IpamProcessor {

    @Inject
    IpamAiService ipamAiService;

    @Inject
    ObjectMapper objectMapper;

    public List<Document> toDocuments(List<IpamRecord> records) {
        List<Document> docs = new ArrayList<>();
        for (IpamRecord record : records) {
            try {
                String json = objectMapper.writeValueAsString(record);
                docs.add(Document.parse(json));
            } catch (Exception e) {
                // Ignore bad records
            }
        }
        return docs;
    }

    public List<IpamRecord> parseBlueCat(List<List<String>> csvData) {
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
        return records;
    }

    public List<IpamRecord> parseInfoblox(String body) {
        List<IpamRecord> records = new ArrayList<>();
        String[] lines = body.split("\\R"); // split by line separator

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("HEADER-")) continue;

            String[] parts = line.split(",", -1);
            if (parts.length < 2) continue;

            String type = parts[0];
            
            if ("NETWORK".equalsIgnoreCase(type)) {
                IpamRecord record = new IpamRecord();
                record.subnetCidr = parts[1];
                if (parts.length > 3) record.ownerTeam = parts[3];
                record.sourceSystem = "Infoblox-Network";
                record.originalRecord = line;
                records.add(record);

            } else if ("HOSTRECORD".equalsIgnoreCase(type)) {
                IpamRecord record = new IpamRecord();
                record.hostName = parts[1];
                record.ipAddress = parts[2];
                record.sourceSystem = "Infoblox-Host";
                record.originalRecord = line;
                records.add(record);
            }
        }
        return records;
    }

    public List<IpamRecord> parseOther(List<List<String>> csvData) {
        List<IpamRecord> records = new ArrayList<>();
        
        boolean first = true;
        for (List<String> row : csvData) {
            if (first) {
                first = false;
                continue;
            }
            if (row.size() < 4) continue;

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
        return records;
    }
    
    public List<IpamRecord> parseUnknown(String body) {
        return ipamAiService.convertUnknownCsv(body);
    }
    }