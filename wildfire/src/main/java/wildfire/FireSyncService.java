
package wildfire;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class FireSyncService {

    private final FirmsApiService firmsApiService;
    private final MongoTemplate mongoTemplate;

    private Instant lastAttempt = Instant.EPOCH;

    public FireSyncService(
            FirmsApiService firmsApiService,
            MongoTemplate mongoTemplate) {

        this.firmsApiService = firmsApiService;
        this.mongoTemplate = mongoTemplate;
    }

    public synchronized void refreshIfNeeded() throws Exception {
    	System.out.println(
    		    "SYNC: Database server: "
    		    + mongoTemplate.getDb()
    		        .runCommand(new Document("hello", 1))
    		        .get("me")
    		);

        Instant now = Instant.now();

        // Retrieve the previous successful refresh time
        Document cache = mongoTemplate.findById(
                "firms",
                Document.class,
                "sync_metadata"
        );

        if (cache != null) {

            Instant lastRefresh = Instant.parse(
                    cache.getString("lastRefresh")
            );

            if (Duration.between(lastRefresh, now)
                    .toMinutes() < 60) {
                System.out.println("SYNC: Using cached data.");
                return;
            }
        }

        // Avoid repeated requests after an API failure
        if (Duration.between(lastAttempt, now)
                .toMinutes() < 5) {
            System.out.println("SYNC: Refresh cooldown active.");
            return;
        }

        lastAttempt = now;

        // Retrieve NASA observations
        System.out.println("SYNC: Contacting NASA API...");

        List<Fire> fires = firmsApiService.fetchLatestFires();

        System.out.println("SYNC: Retrieved " + fires.size() + " detections.");

        if (fires.isEmpty()) {
            return;
        }

        // Mark records as live API observations
        fires.forEach(fire -> fire.setLive(true));

        // Identify the observation dates
        Set<String> dates = fires.stream()
                .map(Fire::getDate)
                .collect(Collectors.toSet());

        // Replace previous live observations for these dates
        Query query = new Query();

        query.addCriteria(
                Criteria.where("live").is(true)
                        .and("date").in(dates)
        );

        mongoTemplate.remove(query, Fire.class);

        // Bulk insert the latest observations
        mongoTemplate.insert(fires, Fire.class);

        // Persist the successful refresh time
        Document metadata = new Document("_id", "firms")
                .append("lastRefresh", Instant.now().toString());

        mongoTemplate.save(metadata, "sync_metadata");

        System.out.println(
                "NASA synchronization complete: "
                + fires.size() + " detections"
        );
    }
}
