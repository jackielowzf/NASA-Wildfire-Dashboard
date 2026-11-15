package wildfire;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class FireController {
	
	private final HarmonizationService harmonizationService;
	
	public FireController(HarmonizationService harmonizationService) {
		this.harmonizationService = harmonizationService;
	}
	
	
	@GetMapping("/api/harmonized")
	public List<GridCell> getHarmonizedFires(
				@RequestParam String date,
				@RequestParam(defaultValue = "90") double north,
				@RequestParam(defaultValue = "-90") double south,
				@RequestParam(defaultValue = "180") double east,
				@RequestParam(defaultValue = "-180") double west) throws Exception {
		
		List<Fire> fires = new ArrayList<>();
		
		fires.addAll(readFires("modis.csv", "MODIS", date, north, south, east, west, 0));
		
		fires.addAll(readFires("viirs.csv", "VIIRS", date, north, south, east, west, 0));
		
		return harmonizationService.harmonize(fires);
		
	}
	
	private List<Fire> readFires(
			String filename, 
			String source,
			String date,
			double north,
			double south,
			double east,
			double west,
			int limit) throws Exception{
		
		List<Fire> fires = new ArrayList<>();
		
		ClassPathResource resource = new ClassPathResource(filename);
		
		try (BufferedReader reader = new BufferedReader (
				new InputStreamReader(
						resource.getInputStream(),
						StandardCharsets.UTF_8))) {
			
			String header = reader.readLine();
			String[] columns = header.split(",");
			
			int latIndex = -1;
			int lonIndex = -1;
			int dateIndex = -1;
			
			for (int i = 0; i < columns.length; i++) {
				String column = columns[i].trim();
				
				if (column.equals("latitude")) latIndex = i;
				if (column.equals("longitude")) lonIndex = i;
				if (column.equals("acq_date")) dateIndex = i;
			}
			
			if (latIndex == -1 || lonIndex == -1 || dateIndex == -1) {
				throw new IllegalArgumentException(
						"Required CSV columns are missing in " + filename);
			}
			
			String line;
			
			while ((line = reader.readLine()) != null && fires.size() < limit) {
				
				String[] values = line.split(",");
				
				double latitude = Double.parseDouble(values[latIndex]);
				double longitude = Double.parseDouble(values[lonIndex]);
				String fireDate = values[dateIndex].trim();
				
				//Date filtering
				if (date != null && !date.isEmpty() && !fireDate.equals(date)) {
					continue;
				}
				
				// Geographic filtering
				if (latitude > north || latitude < south || longitude > east || longitude < west) {
					continue;
				}
				
				fires.add(new Fire(latitude, longitude, fireDate, source));						
			}
		}
		return fires;
	}
}

