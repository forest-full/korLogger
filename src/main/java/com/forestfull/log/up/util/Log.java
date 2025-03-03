package com.forestfull.log.up.util;

import com.forestfull.log.up.Level;
import com.forestfull.log.up.formatter.FileRecorder;
import com.forestfull.log.up.formatter.LogFormatter;
import com.forestfull.log.up.spring.LogLocationInfo;
import com.forestfull.log.up.spring.LogUpStackTrace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * A utility class for logging messages at different levels (INFO, WARN, ERROR).
 *
 * @author <a href="https://vigfoot.com">Vigfoot</a>
 * @see Level
 */
public class Log {

    private Log() {
    }

    /**
     * Logs an debug message.
     *
     * @param msg The message to log.
     * @author <a href="https://vigfoot.com">Vigfoot</a>
     */
    public static void debug(Object... msg) {
        try {
            throw new LogUpStackTrace();
        } catch (LogUpStackTrace e) {
            LogLocationInfo logLocationInfo = new LogLocationInfo(e);
            write(Level.DEBUG, logLocationInfo, msg);
        }
    }

    /**
     * Logs an informational message.
     *
     * @param msg The message to log.
     * @author <a href="https://vigfoot.com">Vigfoot</a>
     */
    public static void info(Object... msg) {
        try {
            throw new LogUpStackTrace();
        } catch (LogUpStackTrace e) {
            LogLocationInfo logLocationInfo = new LogLocationInfo(e);
            write(Level.INFO, logLocationInfo, msg);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param msg The message to log.
     * @author <a href="https://vigfoot.com">Vigfoot</a>
     */
    public static void warn(Object... msg) {
        try {
            throw new LogUpStackTrace();
        } catch (LogUpStackTrace e) {
            LogLocationInfo logLocationInfo = new LogLocationInfo(e);
            write(Level.WARN, logLocationInfo, msg);
        }
    }

    /**
     * Logs an error message.
     *
     * @param msg The message to log.
     * @author <a href="https://vigfoot.com">Vigfoot</a>
     */
    public static void error(Object... msg) {
        try {
            throw new LogUpStackTrace();
        } catch (LogUpStackTrace e) {
            LogLocationInfo logLocationInfo = new LogLocationInfo(e);
            write(Level.ERROR, logLocationInfo, msg);
        }
    }

    /**
     * Writes a log message with a specific level, placeholder pattern, and messages.
     *
     * @param level       The log level (INFO, WARN, ERROR).
     * @param placeholder The placeholder pattern for the log message.
     * @param messages    The messages to log.
     * @author <a href="https://vigfoot.com">Vigfoot</a>
     */
    static void write(final Level level, final LogLocationInfo logLocationInfo, final Object... messages) {
        if (level.compareTo(LogUpFactoryBean.level) < 0) return;
        if (messages == null || messages.length == 0) return;
        if (LogUpFactoryBean.logFormatter == null) return;

        final StringBuilder logMessage = new StringBuilder();

        for (MessageFormatter messageFormatter : LogUpFactoryBean.messageFormatter) {
            logMessage.append(messageFormatter.call(level, logLocationInfo, messages));
        }

        LogFactory.console(logMessage.toString());
        if (LogUpFactoryBean.fileRecorder != null)
            LogFactory.file(logMessage.toString());
    }

    static void write(final Level level, final String placeHolder, final Object... messages) {
        if (level.compareTo(LogUpFactoryBean.level) < 0) return;
        if (messages == null || messages.length == 0) return;
        if (LogUpFactoryBean.logFormatter == null) return;

        final StringBuilder logMessage = new StringBuilder();
        logMessage.append(placeHolder
                .replaceAll(LogFormatter.MessagePattern.DATETIME, LogUpFactoryBean.logFormatter.getDateTimeFormat().format(System.currentTimeMillis()))
                .replaceAll(LogFormatter.MessagePattern.LEVEL, level.getColor() + level.name() + Level.COLOR.RESET));

        for (Object message : messages)
            logMessage.append(message);

        LogFactory.console(logMessage.toString());
        if (LogUpFactoryBean.fileRecorder != null)
            LogFactory.file(logMessage.toString());
    }

    /**
     * A factory class for handling log output to console and files.
     *
     * @author <a href="https://vigfoot.com">Vigfoot</a>
     */
    public static class LogFactory {
        /**
         * Outputs the log message to the console.
         *
         * @param msg The log message to output.
         * @author <a href="https://vigfoot.com">Vigfoot</a>
         */
        protected static synchronized void console(final String msg) {
            final Writer fdOut = new PrintWriter(new FileWriter(FileDescriptor.out));

            try {
                fdOut.write(msg);
                fdOut.flush();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        /**
         * Outputs the log message to a file.
         *
         * @param msg The log message to output.
         * @author <a href="https://vigfoot.com">Vigfoot</a>
         */
        protected static synchronized void file(String msg) {
            final FileRecorder fileRecorder = LogUpFactoryBean.fileRecorder;

            String logFileDirectory = fileRecorder.getDirectory();

            if (logFileDirectory == null || logFileDirectory.isEmpty()) return;

            if (!File.separator.equals(logFileDirectory.substring(0, 1)) && !"/".equals(logFileDirectory.substring(0, 1)))
                logFileDirectory = System.getProperty("user.dir") + File.separator + logFileDirectory;

            if (!File.separator.equals(logFileDirectory.substring(logFileDirectory.length() - 1))
                    && !"/".equals(logFileDirectory.substring(logFileDirectory.length() - 1)))
                logFileDirectory += File.separator;

            try {
                final File rootDirectory = new File(logFileDirectory);
                if (!rootDirectory.exists()) {
                    if (!rootDirectory.isDirectory()) rootDirectory.deleteOnExit();
                    boolean isSucceed = rootDirectory.mkdirs();
                    if (!isSucceed) throw new IOException("Failed to create log directory: " + rootDirectory);
                }

                final File logFile = new File(logFileDirectory + File.separator + fileRecorder.getPlaceholder().replace(FileRecorder.FilePattern.DATE, ""));
                if (!logFile.exists()) {
                    boolean isSucceed = logFile.createNewFile();
                    if (!isSucceed) throw new IOException("Failed to create log file: " + logFile);
                } else if (logFile.isFile()) {
                    final String currentTimeFormatName = fileRecorder.getPlaceholder().replace(FileRecorder.FilePattern.DATE, fileRecorder.getDateFormat().format(new Date()));
                    final String existedTimeFormatName = fileRecorder.getPlaceholder().replace(FileRecorder.FilePattern.DATE, fileRecorder.getDateFormat().format(new Date(logFile.lastModified())));
                    if (!currentTimeFormatName.equals(existedTimeFormatName)) {
                        boolean isSucceed = logFile.renameTo(new File(logFileDirectory + File.separator + existedTimeFormatName));
                        if (!isSucceed) throw new IOException("Failed to rename log file: " + existedTimeFormatName);
                        logFile.deleteOnExit();
                    }
                }

                final FileOutputStream outputStream = new FileOutputStream(logFile, true);
                outputStream.write((msg.getBytes(StandardCharsets.UTF_8)));
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        protected static synchronized void initConsole() {
            Level level = LogUpFactoryBean.level; // touch
            console("=================================================================================================================================================================" + System.lineSeparator());
            console("Log Up by forest full's vigfoot" + System.lineSeparator());
            console("=================================================================================================================================================================" + System.lineSeparator());
        }
    }
}