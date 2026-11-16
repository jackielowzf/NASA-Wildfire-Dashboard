
package wildfire;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.domain.Sort;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;

@RestController
public class FireController {

    private final HarmonizationService harmonizationService;
    private final FireRepository fireRepository;
    private final MongoTemplate mongoTemplate;
    private final FireSyncService fireSyncService;

    public FireController(
            HarmonizationService harmonizationService,
            FireRepository fireRepository,
            MongoTemplate mongoTemplate,
            FireSyncService fireSyncService) {

        this.harmonizationService = harmonizationService;
        this.fireRepository = fireRepository;
        this.mongoTemplate = mongoTemplate;
        this.fireSyncService = fireSyncService;
    }

    @GetMapping("/api/fires")
    public List<Fire> getFires(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "90") double north,
            @RequestParam(defaultValue = "-90") double south,
            @RequestParam(defaultValue = "180") double east,
            @RequestParam(defaultValue = "-180") double west,
            @RequestParam(defaultValue = "ALL") String source) {

        return queryFires(date, north, south, east, west,
                source, 500);
    }

    @GetMapping("/api/harmonized")
    public List<GridCell> getHarmonizedFires(
            @RequestParam String date,
            @RequestParam(defaultValue = "90") double north,
            @RequestParam(defaultValue = "-90") double south,
            @RequestParam(defaultValue = "180") double east,
            @RequestParam(defaultValue = "-180") double west) {

        List<Fire> fires = queryFires(
                date, north, south, east, west, "ALL", 0);

        return harmonizationService.harmonize(fires);
    }
    
    @PostMapping("/api/sync")
    public ResponseEntity<Map<String, String>> syncData() {

        try {
            fireSyncService.refreshIfNeeded();

            return ResponseEntity.ok(
                    Map.of("status", "ok")
            );

        } catch (Exception e) {

            return ResponseEntity.status(503).body(
                    Map.of("status", "NASA refresh unavailable")
            );
        }
    }

    private List<Fire> queryFires(
            String date,
            double north,
            double south,
            double east,
            double west,
            String source,
            int limit) {

        Query query = new Query();

        query.addCriteria(
            Criteria.where("latitude").gte(south).lte(north));

        query.addCriteria(
            Criteria.where("longitude").gte(west).lte(east));

        if (date != null && !date.isBlank()) {
            query.addCriteria(
                Criteria.where("date").is(date));
        }

        if (!source.equals("ALL")) {
            query.addCriteria(
                Criteria.where("source").is(source));
        }

        if (limit > 0) {
            query.limit(limit);
        }

        return mongoTemplate.find(query, Fire.class);
    }

    @GetMapping("/api/test-db")
    public long testDatabase() {
        return fireRepository.count();
    }
    

	@GetMapping("/api/history")
	public List<DailyStatistics> getHistory() {
	
	    Aggregation aggregation = Aggregation.newAggregation(
	
	        Aggregation.group("date")
	            .sum(
	                ConditionalOperators.when(
	                    Criteria.where("source").is("MODIS")
	                ).then(1).otherwise(0)
	            ).as("modisCount")
	
	            .sum(
	                ConditionalOperators.when(
	                    Criteria.where("source").is("VIIRS")
	                ).then(1).otherwise(0)
	            ).as("viirsCount"),
	
	        Aggregation.project("modisCount", "viirsCount")
	            .and("_id").as("date")
	            .andExclude("_id"),
	
	        Aggregation.sort(Sort.Direction.ASC, "date")
	    );
	
	    return mongoTemplate.aggregate(
	        aggregation,
	        Fire.class,
	        DailyStatistics.class
	    ).getMappedResults();
	}
}
