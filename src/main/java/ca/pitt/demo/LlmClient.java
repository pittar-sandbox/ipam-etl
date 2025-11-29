package ca.pitt.demo;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.HeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.vertx.core.json.JsonObject;

@Path("/v1")
@RegisterRestClient(configKey = "llm-api")
public interface LlmClient {

    @POST
    @Path("/chat/completions")
    JsonObject chat(JsonObject request, @HeaderParam("Authorization") String apiKey);
}