package com.forestfull.log.up.util;

import com.forestfull.log.up.formatter.LogFormatter;
import com.forestfull.log.up.spring.LogLocationInfo;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MessageFormatter {

    abstract String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args);

    static String[] splitWithDelimiter(String placeholder) {
        Pattern pattern = Pattern.compile("\\{\\w+}|\\{new-line}");
        Matcher matcher = pattern.matcher(placeholder);
        List<String> result = new ArrayList<>();

        int lastEnd = 0;
        while (matcher.find()) {
            if (lastEnd != matcher.start()) {
                result.add(placeholder.substring(lastEnd, matcher.start()));
            }
            result.add(matcher.group());
            lastEnd = matcher.end();
        }

        if (lastEnd != placeholder.length()) {
            result.add(placeholder.substring(lastEnd));
        }

        return result.toArray(new String[0]);

    }

    static MessageFormatter[] replaceMatchPlaceholder(String[] placeholder) {
        MessageFormatter[] messageFormatters = new MessageFormatter[placeholder.length];

        for (int i = 0; i < placeholder.length; i++) {
            switch (placeholder[i]) {
                case LogFormatter.MessagePattern.DATETIME:
                    messageFormatters[i] = new DateMessageFormatter();
                    break;

                case LogFormatter.MessagePattern.LEVEL:
                    messageFormatters[i] = new Level();
                    break;

                case LogFormatter.MessagePattern.THREAD:
                    messageFormatters[i] = new Thread();
                    break;

                case LogFormatter.MessagePattern.NEW_LINE:
                    messageFormatters[i] = new LineSeparator();
                    break;

                case LogFormatter.MessagePattern.MESSAGE:
                    messageFormatters[i] = new Message();
                    break;

                default:
                    messageFormatters[i] = new Mime(placeholder[i]);
                    break;
            }
        }

        return messageFormatters;
    }

    static class DateMessageFormatter extends MessageFormatter {

        @Override
        public String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args) {
            return LogUpFactoryBean.logFormatter.getDateTimeFormat().format(System.currentTimeMillis());
        }
    }

    static class Level extends MessageFormatter {

        @Override
        public String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args) {
            return level.getColor() + String.format("%5s", level.name()) + com.forestfull.log.up.Level.COLOR.RESET;
        }
    }

    static class Thread extends MessageFormatter {
        private static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        @Override
        public String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args) {
            StringBuilder currentThreadName = new StringBuilder()
                    .append(com.forestfull.log.up.Level.COLOR.PURPLE)
                    .append("[PID:").append(PID).append("] ")
                    .append(com.forestfull.log.up.Level.COLOR.RESET)
                    .append(com.forestfull.log.up.Level.COLOR.CYAN)
                    .append(java.lang.Thread.currentThread().getName())
                    .append(com.forestfull.log.up.Level.COLOR.RESET)
                    .append(' ');
            String[] split = logLocationInfo.getClassName().split("\\.");
            for (int i = 0; i < split.length - 1; i++) {
                currentThreadName.append(split[i].charAt(0)).append('.');
            }
            currentThreadName.append(split[split.length - 1])
                    .append('.').append(logLocationInfo.getMethodName())
                    .append('(').append(logLocationInfo.getFileName())
                    .append(':').append(logLocationInfo.getLineNumber())
                    .append(')');

            return currentThreadName.toString();
        }
    }

    static class LineSeparator extends MessageFormatter {

        @Override
        String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args) {
            return System.lineSeparator();
        }
    }

    static class Message extends MessageFormatter {

        @Override
        String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args) {
            StringBuilder message = new StringBuilder();
            for (Object arg : args)
                message.append(arg);

            return level.getColor() + message + com.forestfull.log.up.Level.OFF.getColor();
        }
    }

    static class Mime extends MessageFormatter {

        private final String mime;

        public Mime(String mime) {
            this.mime = mime;
        }

        @Override
        String call(final com.forestfull.log.up.Level level, final LogLocationInfo logLocationInfo, final Object... args) {
            return this.mime;
        }
    }
}