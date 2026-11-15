package wildfire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class HarmonizationService {

    public List<GridCell> harmonize(List<Fire> fires) {

        double gridSize = 0.01;

        Map<String, GridCell> grid = new HashMap<>();

        for (Fire fire : fires) {

            int latIndex = (int) Math.floor(
                    fire.getLatitude() / gridSize);

            int lonIndex = (int) Math.floor(
                    fire.getLongitude() / gridSize);

            String key = latIndex + "," + lonIndex;

            if (!grid.containsKey(key)) {

                double centerLat =
                        (latIndex + 0.5) * gridSize;

                double centerLon =
                        (lonIndex + 0.5) * gridSize;

                grid.put(key,
                        new GridCell(centerLat, centerLon));
            }

            grid.get(key).addDetection(fire.getSource());
        }

        return new ArrayList<>(grid.values());
    }
}
