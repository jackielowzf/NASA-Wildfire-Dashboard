
package wildfire;

public class DailyStatistics {

    private String date;
    private long modisCount;
    private long viirsCount;

    public DailyStatistics() {
    }

    public String getDate() {
        return date;
    }

    public long getModisCount() {
        return modisCount;
    }

    public long getViirsCount() {
        return viirsCount;
    }

    public long getTotalCount() {
        return modisCount + viirsCount;
    }
}
