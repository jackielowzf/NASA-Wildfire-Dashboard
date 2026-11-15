package wildfire;

public class Fire {
	
	private double latitude;
	private double longitude;
	private String date;
	private String source;
	
	public Fire(double latitude, double longitude, String date, String source) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.date = date;
		this.source = source;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getSource() {
		return source;
	}

}
