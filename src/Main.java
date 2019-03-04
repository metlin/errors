public class Main {
    public static void main(String[] args) {
        String logsDirectory = "path/to/errors/logs";
        String outputPath = "path/to/errors/statistics.txt";
        Logger logger = new Logger(logsDirectory, outputPath);
    }
}
