package ca.pitt.demo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

@ApplicationScoped
public class IpamAiService {

    private static final Logger LOG = Logger.getLogger(IpamAiService.class);

    @Inject
    @RestClient
    LlmClient llmClient;

    @Inject
    ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are an expert data analyst and parser.
        Your task is to analyze the provided CSV data (which may have an unknown or weird header) and map it to a list of normalized IPAM records.
        
        The target schema has the following fields:
        - ipAddress (String): The IPv4 or IPv6 address.
        - hostName (String): The DNS hostname.
        - status (String): The status of the IP (e.g., Assigned, Free, Reserved).
        - macAddress (String): The MAC address.
        - subnetCidr (String): The CIDR notation of the subnet (e.g., 192.168.1.0/24).
        - ownerTeam (String): The team or owner of the record.
        - sourceSystem (String): Set this to 'AI-Parsed' for all records.
        
        Instructions:
        1. Ignore any header rows if they exist but don't look like data.
        2. Extract valid IP addresses and corresponding information.
        3. If a field is missing in the input, leave it null.
        4. Return the result strictly as a JSON array of objects matching the fields above.
        5. Do not include markdown formatting like ```json ... ```. Just the raw JSON array.
        """;

    public List<IpamRecord> convertUnknownCsv(String csvData) {
        try {
            JsonObject request = new JsonObject()
                .put("model", "Qwen3-14B-quantized.w4a16")
                .put("messages", new JsonArray()
                    .add(new JsonObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .add(new JsonObject().put("role", "user").put("content", "Please parse the following CSV data:\n\n" + csvData))
                )
                .put("temperature", 0.1);

            JsonObject response = llmClient.chat(request, "Bearer dummy");
            
            String content = response.getJsonArray("choices")
                .getJsonObject(0)
                .getJsonObject("message")
                .getString("content");

            // Clean up any potential markdown code blocks if the model ignores instruction 5
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            
            content = content.trim();

            return objectMapper.readValue(content, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, IpamRecord.class));

        } catch (Exception e) {
            LOG.error("Failed to parse CSV with LLM", e);
            return new ArrayList<>();
        }
    }
}