package ca.pitt.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import jakarta.inject.Inject; // FIX: Changed from javax.inject
import jakarta.ws.rs.GET;      // FIX: Changed from jakarta.ws.rs.core.GET
import jakarta.ws.rs.Path;     // FIX: Changed from jakarta.ws.rs.core.Path
import jakarta.ws.rs.Produces; // FIX: Changed from jakarta.ws.rs.core.Produces
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.ArrayList;

@Path("/api/ipam")
public class IpamResource {

    @Inject
    MongoClient mongoClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> getAllIpamRecords() {
        MongoCollection<Document> collection = mongoClient.getDatabase("ipam").getCollection("ipam_records");
        return collection.find().into(new ArrayList<>());
    }
}