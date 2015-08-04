package org.wg3.log;

/**
 * Generic logging wrapper access for our stuff so that any type of logging
 * system can be plugged-in, included pluggable logging like slf4j or the AWIPS2
 * logging system...
 *
 * @author Robert Toomey
 */
public interface Logger {

    // Lazy adding methods pretty much matching Slf4j.  You'll have to link
    // to other logging systems.
    public void debug(String string);

    public void error(String string);

    public void info(String string);

    public void warn(String string);

    public void info(String string, Object o);

    public void info(String string, Object o, Object o1);

    public void info(String string, Object[] os);

    public void trace(String string);

    public boolean isInfoEnabled();

    public boolean isDebugEnabled();

    /**
     * A logger that doesn't log.  I guess Logger 'could' just be a class
     */
    public static class nullLogger implements Logger {

        @Override
        public void debug(String string) {
        }

        @Override
        public void error(String string) {
        }

        @Override
        public void info(String string) {
        }

        @Override
        public void warn(String string) {
        }

        @Override
        public void info(String string, Object o) {
        }

        @Override
        public void info(String string, Object o, Object o1) {
        }

        @Override
        public void info(String string, Object[] os) {
        }

        @Override
        public void trace(String string) {
        }

        @Override
        public boolean isInfoEnabled() {
           return false;
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }
    }
    
    /**
     * A logger that dumps to system out
     */
    public static class sysOutLogger implements Logger {

        @Override
        public void debug(String string) {
        	System.out.println(string);
        }

        @Override
        public void error(String string) {
        	System.out.println(string);
        }

        @Override
        public void info(String string) {
        	System.out.println(string);
        }

        @Override
        public void warn(String string) {
        	System.out.println(string);
        }

        @Override
        public void info(String string, Object o) {
        	System.out.println(string);
        }

        @Override
        public void info(String string, Object o, Object o1) {
        	System.out.println(string);
        }

        @Override
        public void info(String string, Object[] os) {
        	System.out.println(string);
        }

        @Override
        public void trace(String string) {
        	System.out.println(string);
        }

        @Override
        public boolean isInfoEnabled() {
           return false;
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }
    }
}
