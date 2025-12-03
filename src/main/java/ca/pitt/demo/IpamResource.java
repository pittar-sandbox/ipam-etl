package ca.pitt.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

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

    @Inject
    Template ipam;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> getAllIpamRecords() {
        MongoCollection<Document> collection = mongoClient.getDatabase("ipam").getCollection("ipam_records");
        return collection.find().into(new ArrayList<>());
    }

    @GET
    @Path("/view")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance view() {
        return ipam.data("records", getAllIpamRecords());
    }

    @GET
    @Path("/delete")
    @Produces(MediaType.TEXT_PLAIN)
    public String deleteAll() {
        MongoCollection<Document> collection = mongoClient.getDatabase("ipam").getCollection("ipam_records");
        collection.deleteMany(new Document());
        return "All records deleted";
    }
}