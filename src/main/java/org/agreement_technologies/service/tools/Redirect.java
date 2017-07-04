package org.agreement_technologies.service.tools;

import java.io.PrintStream;

/**
 * @author Oscar
 */
public class Redirect {
    private final PrintStream outputStream, errorStream;

    public Redirect() {
        outputStream = captureStandardOutput();
        errorStream = captureStandardError();
    }

    public static Redirect captureOutput() {
        return new Redirect();
    }

    public static PrintStream captureStandardOutput() {
        PrintStream consoleOut = System.out;
        try {
            java.io.PipedInputStream readBuffer = new java.io.PipedInputStream();
            java.io.PipedOutputStream outBuffer = new java.io.PipedOutputStream(readBuffer);
            System.setOut(new java.io.PrintStream(outBuffer));
        } catch (java.io.IOException e) {
        }
        return consoleOut;
    }

    public static PrintStream captureStandardError() {
        PrintStream consoleOut = System.err;
        try {
            java.io.PipedInputStream readBuffer = new java.io.PipedInputStream();
            java.io.PipedOutputStream outBuffer = new java.io.PipedOutputStream(readBuffer);
            System.setErr(new java.io.PrintStream(outBuffer));
        } catch (java.io.IOException e) {
        }
        return consoleOut;
    }

    public void releaseOutput() {
        System.setErr(errorStream);
        System.setOut(outputStream);
    }
}
