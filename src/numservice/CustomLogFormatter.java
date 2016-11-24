package numservice;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Custom log message formatter for improved readability
 * Default one is a cluttered mess
 * Created by samlinz on 24.11.2016.
 */
public class CustomLogFormatter extends Formatter {

    // HOURS MINUTES SECONDS MILLISECONDS
    private static SimpleDateFormat dFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    public String format(LogRecord logRecord) {
        StringBuilder str = new StringBuilder();
        str.append(dFormat.format(new Date()));
        str.append(" " + logRecord.getLevel() + " - " + logRecord.getMessage());
        str.append("\n");
        return str.toString();
    }
}
