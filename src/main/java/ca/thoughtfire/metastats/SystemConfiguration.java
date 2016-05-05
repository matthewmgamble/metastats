/*
 * 
Copyright (C) 2015 Thoughtfire Consultng Inc.
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package ca.thoughtfire.metastats;

/**
 *
 * @author mgamble
 */
public class SystemConfiguration {
    private String dbServer;
    private String dbUser;
    private String dbPass;
    private String dbName;
    private String logDir;
    private String logFileName = "metastats.log";
    private boolean dbSSL = false;
    private boolean debug = true;
    private int listenPort = 8080;

    /**
     * @return the dbServer
     */
    public String getDbServer() {
        return dbServer;
    }

    /**
     * @param dbServer the dbServer to set
     */
    public void setDbServer(String dbServer) {
        this.dbServer = dbServer;
    }

    /**
     * @return the dbUser
     */
    public String getDbUser() {
        return dbUser;
    }

    /**
     * @param dbUser the dbUser to set
     */
    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    /**
     * @return the dbPass
     */
    public String getDbPass() {
        return dbPass;
    }

    /**
     * @param dbPass the dbPass to set
     */
    public void setDbPass(String dbPass) {
        this.dbPass = dbPass;
    }

    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * @return the dbSSL
     */
    public boolean isDbSSL() {
        return dbSSL;
    }

    /**
     * @param dbSSL the dbSSL to set
     */
    public void setDbSSL(boolean dbSSL) {
        this.dbSSL = dbSSL;
    }

    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return the listenPort
     */
    public int getListenPort() {
        return listenPort;
    }

    /**
     * @param listenPort the listenPort to set
     */
    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    /**
     * @return the logDir
     */
    public String getLogDir() {
        return logDir;
    }

    /**
     * @param logDir the logDir to set
     */
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    /**
     * @return the logFileName
     */
    public String getLogFileName() {
        return logFileName;
    }

    /**
     * @param logFileName the logFileName to set
     */
    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }
    
}
