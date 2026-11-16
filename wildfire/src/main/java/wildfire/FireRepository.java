
package wildfire;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface FireRepository extends MongoRepository<Fire, String> {

    boolean existsBySource(String source);

}
