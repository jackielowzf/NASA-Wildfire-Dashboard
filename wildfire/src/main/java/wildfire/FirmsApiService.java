
package wildfire;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirmsApiService {

    @Value("${nasa.firms.api-key}")
    private String apiKey;

    @Value("${nasa.firms.base-url}")
    private String baseUrl;

    @Value("${nasa.firms.area}")
    private String area;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private String fetchCsv(String path) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "NASA API request failed: " + response.statusCode()
            );
        }

        return response.body();
    }

    public LocalDate getLatestCommonDate() throws Exception {

        String csv = fetchCsv(
                "/data_availability/csv/" + apiKey + "/ALL"
        );

        LocalDate modisDate = null;
        LocalDate viirsDate = null;

        for (String line : csv.split("\\R")) {

            String[] values = line.split(",");

            if (values.length < 3) {
                continue;
            }

            if (values[0].equals("MODIS_NRT")) {
                modisDate = LocalDate.parse(values[2].trim());
            }

            if (values[0].equals("VIIRS_NOAA20_NRT")) {
                viirsDate = LocalDate.parse(values[2].trim());
            }
        }

        if (modisDate == null || viirsDate == null) {
            throw new IllegalStateException(
                    "Satellite availability could not be determined."
            );
        }

        return modisDate.isBefore(viirsDate)
                ? modisDate : viirsDate;
    }

    public List<Fire> fetchFires(
            String product,
            String source,
            LocalDate date
    ) throws Exception {

        String csv = fetchCsv(
                "/area/csv/" + apiKey
                + "/" + product
                + "/" + area
                + "/1/" + date
        );

        String[] lines = csv.split("\\R");

        List<Fire> fires = new ArrayList<>();

        String[] headers = lines[0].split(",");

        int latIndex = -1;
        int lonIndex = -1;
        int dateIndex = -1;

        for (int i = 0; i < headers.length; i++) {

            switch (headers[i].trim()) {
                case "latitude" -> latIndex = i;
                case "longitude" -> lonIndex = i;
                case "acq_date" -> dateIndex = i;
            }
        }

        if (latIndex == -1 || lonIndex == -1 || dateIndex == -1) {
            throw new IllegalStateException(
                    "Invalid NASA CSV response."
            );
        }

        for (int i = 1; i < lines.length; i++) {

            if (lines[i].isBlank()) {
                continue;
            }

            String[] values = lines[i].split(",", -1);

            double latitude = Double.parseDouble(values[latIndex]);
            double longitude = Double.parseDouble(values[lonIndex]);
            String observationDate = values[dateIndex].trim();

            fires.add(new Fire(
                    latitude,
                    longitude,
                    observationDate,
                    source
            ));
        }

        return fires;
    }

    public List<Fire> fetchLatestFires() throws Exception {

        LocalDate date = getLatestCommonDate();

        List<Fire> fires = new ArrayList<>();

        fires.addAll(fetchFires("MODIS_NRT", "MODIS", date));

        fires.addAll(fetchFires("VIIRS_NOAA20_NRT", "VIIRS", date));

        return fires;
    }
}
