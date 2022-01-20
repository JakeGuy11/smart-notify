package com.jakeguy11.smartnotify;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

    private File logFile;
    private FileWriter writer = null;
    private PrintWriter printer = null;
    private String className;
    private boolean valid = true;

    /**
     * The different urgencies to log with
     */
    public enum LogLevel {
        VERBOSE,
        INFO,
        WARNING,
        ERROR,
        FATAL
    }

    /**
     * Create a new Logger.
     *
     * @param fileStem The name of the file to log to (without an extension or path).
     * @param cls The name  of the class to write from.
     * @param cxt The context of the calling class.
     */
    public Logger(String fileStem, String cls, Context cxt) {
        // First make the directory for the logs if it doesn't exist
        createLogDir(cxt);

        // Next, create the actual file. Overwrite if it exists
        this.logFile = new File(cxt.getFilesDir() + "/logs/" + fileStem + ".log");

        // Create the fileWriter
        try {
            this.writer = new FileWriter(this.logFile, true);
        } catch (IOException e) { this.valid = false; }

        // Create the actual printer
        this.printer = new PrintWriter(new BufferedWriter(this.writer));

        // Assign the className
        this.className = cls;
    }

    /**
     * Log a new message.
     *
     * @param msg The message to log.
     * @param lv The level to log it with.
     */
    public void log(String msg, LogLevel lv) {
        // First, get the date/time to log with
        String timeStamp = getDateTime();
        String logLine = timeStamp + " [" + this.className + "] " + lv + ": " + msg;

        // Print the message
        this.printer.println(logLine);
    }

    /**
     * Log a new message with the default LogLevel 'INFO'.
     *
     * @param msg The message to log.
     */
    public void log(String msg) {
        // First, get the date/time to log with
        String timeStamp = getDateTime();
        String logLine = timeStamp + " [" + this.className + "] " + LogLevel.INFO + ": " + msg;

        // Print the message
        this.printer.println(logLine);
    }

    /**
     * Write the contents of the printer to the file.
     */
    public void write() {
        this.printer.flush();
    }

    /**
     * Create the directory for the logs if it doesn't exist already.
     *
     * @param cxt The context of the files to write to.
     */
    private void createLogDir(Context cxt) {
        // Create the dir as a file
        File logDir = new File(cxt.getFilesDir() + "/logs");

        // If it doesn't exist, create it
        if (!logDir.exists())
            logDir.mkdir();
    }

    /**
     * Get a well-formatted date and time string
     *
     * @return "yyyy-MM-dd    HH:mm:ss"
     */
    private String getDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    /**
     * Check whether or not the logger is valid. Should be called after creation.
     *
     * @return Whether or not the logger is valid.
     */
    public boolean isValid() {
        return this.valid;
    }

}
