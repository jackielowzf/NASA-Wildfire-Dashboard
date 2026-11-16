
package wildfire;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final FireRepository fireRepository;

    public AdminController(FireRepository fireRepository) {
        this.fireRepository = fireRepository;
    }

    @GetMapping("/api/admin/status")
    public Map<String, Object> getStatus() {

        return Map.of(
            "status", "online",
            "authenticated", true,
            "totalDetections", fireRepository.count()
        );
    }
}
