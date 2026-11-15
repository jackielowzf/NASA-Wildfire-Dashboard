
package wildfire;

public class GridCell {

    private double latitude;
    private double longitude;
    private int modisCount;
    private int viirsCount;

    public GridCell(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void addDetection(String source) {
        if (source.equals("MODIS")) {
            modisCount++;
        } else if (source.equals("VIIRS")) {
            viirsCount++;
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getModisCount() {
        return modisCount;
    }

    public int getViirsCount() {
        return viirsCount;
    }

    public int getTotalCount() {
        return modisCount + viirsCount;
    }

    public boolean isShared() {
        return modisCount > 0 && viirsCount > 0;
    }
}
