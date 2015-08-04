package org.wg3.log;

/**
 * Generic logging wrapper access for our stuff so that any type of logging
 * system can be plugged-in, included pluggable logging like slf4j or the AWIPS2
 * logging system...
 *
 * @author Robert Toomey
 */
public class LoggerFactory {

    /**
     * The LoggerFactory that will do the work
     */
    public static LoggerFactory theFactory;

    /**
     * Get the logger from factory
     */
    public static Logger getLogger(Class<?> aClass) {
        if (theFactory != null) {
            return theFactory.getLoggerImpl(aClass);
        } else {
            //return new Logger.nullLogger();
        	return new Logger.sysOutLogger();

        }
    }

    public static Logger getLogger(String name) {
        if (theFactory != null) {
            return theFactory.getLoggerImpl(name);
        } else {
            //return new Logger.nullLogger();
        	return new Logger.sysOutLogger();
        }
    }

    /**
     * Kinda abstract, but not since we allow null logging above
     */
    public Logger getLoggerImpl(Class<?> aClass) {
        return null;
    }

    /**
     * Kinda abstract, but not since we allow null logging above
     */
    public Logger getLoggerImpl(String name) {
        return null;
    }

    /**
     * Introduce an actual LoggerFactory that will create loggers.
     */
    public static void setLoggerFactory(LoggerFactory f) {
        theFactory = f;
    }
}
