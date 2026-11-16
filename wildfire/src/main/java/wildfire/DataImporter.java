
package wildfire;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DataImporter implements CommandLineRunner {

    private final FireRepository fireRepository;

    public DataImporter(FireRepository fireRepository) {
        this.fireRepository = fireRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        importData("modis.csv", "MODIS");
        importData("viirs.csv", "VIIRS");

        System.out.println("Total detections: " 
                + fireRepository.count());
    }

    private void importData(String filename, String source)
            throws Exception {

        if (fireRepository.existsBySource(source)) {
            System.out.println(source + " data already imported.");
            return;
        }

        List<Fire> fires = new ArrayList<>();

        ClassPathResource resource = new ClassPathResource(filename);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8))) {

            String[] headers = reader.readLine().split(",");

            int latIndex = -1;
            int lonIndex = -1;
            int dateIndex = -1;

            for (int i = 0; i < headers.length; i++) {

                String header = headers[i].trim();

                if (header.equals("latitude"))
                    latIndex = i;

                if (header.equals("longitude"))
                    lonIndex = i;

                if (header.equals("acq_date"))
                    dateIndex = i;
            }

            if (latIndex == -1 || lonIndex == -1 || dateIndex == -1) {
                throw new IllegalArgumentException(
                    "Missing required CSV columns");
            }

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isBlank())
                    continue;

                String[] values = line.split(",");

                double latitude =
                    Double.parseDouble(values[latIndex]);

                double longitude =
                    Double.parseDouble(values[lonIndex]);

                String date = values[dateIndex].trim();

                fires.add(new Fire(
                    latitude, longitude, date, source));
            }
        }

        fireRepository.insert(fires);

        System.out.println("Imported " + fires.size()
                + " " + source + " detections.");
    }
}
