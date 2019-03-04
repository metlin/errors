public class ErrorInfo {
    private String hour;
    private String minute;
    private String type;

    ErrorInfo(String hour, String minute, String type) {
        this.hour = hour;
        this.minute = minute;
        this.type = type;
    }

    public String getMinute() {
        return minute;
    }

    public String getType() {
        return type;
    }

    public String getHour() {
        return hour;
    }
}
