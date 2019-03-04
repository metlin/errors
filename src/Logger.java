import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Logger {
    private final Map<String, Integer> hoursErrorDistribution;
    private final Map<String, Integer> minutesErrorDistribution;
    private final Map<String, Integer> errorTypeDistribution;

    Logger(final String logsDirectory, final String outputPath) {
        hoursErrorDistribution = new ConcurrentHashMap<>();
        minutesErrorDistribution = new ConcurrentHashMap<>();
        errorTypeDistribution = new ConcurrentHashMap<>();

        List<File> files = getFiles(logsDirectory);
        processFiles(files);
        writeStatisticsInFile(outputPath);
    }

    private void processFiles(final List<File> files) {
        files
            .stream()
            .map(file -> {
                FileThread thread = new FileThread(file);
                thread.start();
                return thread;
            })
            .forEach(thread -> {
                try {
                    thread.join();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
    }

    private List<File> getFiles(final String logsDirectory) {
        try {
            Stream<Path> paths = Files.walk(Paths.get(logsDirectory));
            return paths
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return new ArrayList<>();
    }

    private void writeStatisticsInFile(String outputPath) {
        final Map<String, Integer> hours = new TreeMap<>(hoursErrorDistribution);
        final Map<String, Integer> minutes = new TreeMap<>(minutesErrorDistribution);
        final Map<String, Integer> types = new TreeMap<>(errorTypeDistribution);

        List<String> allLines = new ArrayList<String>();
        hours.forEach((h, i) -> allLines.add("Hour " + h + " errors " + i));
        minutes.forEach((m, i) -> allLines.add("Minute " + m + " errors " + i));
        types.forEach((t, i) -> allLines.add("Type " + t + " errors " + i));

        try {
            Files.write(Paths.get(outputPath), allLines);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private class FileThread extends Thread {
        final File file;

        FileThread(final File file) {
            this.file = file;
        }

        public void run(){
            List<String> errors = getErrorsFromFile(file);
            errors.forEach(this::processError);
        }

        private void processError(final String error) {
            try {
                ErrorInfo errorInfo = getHourAndMinute(error);
                hoursErrorDistribution.merge(errorInfo.getHour(), 1, Integer::sum);
                minutesErrorDistribution.merge(errorInfo.getMinute(), 1, Integer::sum);
                errorTypeDistribution.merge(errorInfo.getType(), 1, Integer::sum);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        private List<String> getErrorsFromFile(final File file) {
            try {
                final Charset cyrillic = Charset.forName("windows-1251");
                return Files
                    .lines(Paths.get(file.getAbsolutePath()), cyrillic)
                    .filter(line -> line.contains("ERROR"))
                    .collect(Collectors.toList());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            return new ArrayList<>();
        }

        private ErrorInfo getHourAndMinute(final String error) throws Exception {
            Pattern p = Pattern.compile("^.*(?:\\d{2}\\.\\d{2}\\.\\d{4}\\s)?(\\d{2}):(\\d{2}):\\d{2}.+ ERROR ([^-]+) -");
            Matcher m = p.matcher(error);

            while (m.find()) {
                return new ErrorInfo(m.group(1), m.group(2), m.group(3));
            }

            throw new Exception("Invalid error string format: " + error);
        }
    }
}
