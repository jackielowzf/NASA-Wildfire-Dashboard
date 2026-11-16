
package wildfire;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "fires")
public class Fire {

    @Id
    private String id;
    private double latitude;
    private double longitude;
    private String date;
    private String source;
    private boolean live;
    
    public boolean isLive() {
    	return live;
    }
    
    public void setLive(boolean live) {
    	this.live = live;
    }

    public Fire(double latitude, double longitude, String date, String source) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.source = source;
    }

    public String getId() {
        return id;
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
