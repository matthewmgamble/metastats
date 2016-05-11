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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import io.prometheus.client.Gauge;
import java.io.File;
import java.io.IOException;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author mgamble
 */
public class StatsServer {
    
    static Version version = new Version();
    static Logger logger = Logger.getLogger("ca.thoughtfire.metastats");
    static BoneCP connectionPool;
    static SystemConfiguration config = new SystemConfiguration();
    static Timer timer;
    static DBTask dbTask = new DBTask();
    
    public static SystemConfiguration getConfiguration() {
        return config;
    }
    
    public static Logger getLogInterface() {
        return logger;
    }
    
    public static void appendLog(String logMessage) {
        if (config.isDebug()) {
            System.out.println(logMessage);
        }
        logger.info(logMessage);
    }
    
    public static BoneCP getConnectionPool() {
        return connectionPool;
    }
    
    public static void main(final String[] args) throws Exception {
        
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        
        System.out.println("metaStats Core Server Version " + version.getBuildNumber() + " (" + version.getBuildName() + ") - Code By " + version.getAuthor());
        System.out.println("");
        OptionParser parser = new OptionParser("c:");
        parser.accepts("config").withRequiredArg();
        
        try {
            OptionSet options = parser.parse(args);
            
            String configFileName = options.valueOf("config").toString();
            File file = new File(configFileName);
            if ((!file.isFile()) || (!file.canRead())) {
                System.out.println("Error - cannot read configuration \"" + file + "\" - aborting.");
                System.exit(1);
            }
            try {
                String jsonInput = new String(readAllBytes(get(configFileName)));
                config = gson.fromJson(jsonInput, SystemConfiguration.class);
                
            } catch (IOException | JsonSyntaxException ex) {
                System.out.println("Error - cannot parse configuration \"" + file + "\" - error is \"" + ex + "\" - aborting.");
                System.exit(1);
            }
            
        } catch (Exception ex) {
            System.out.println("Error processing command line arguments: " + ex);
            System.exit(1);
        }
        try {
            logger.addAppender(new DailyRollingFileAppender(new PatternLayout("%d{ISO8601} [%-5p] %m%n"), config.getLogDir() + "/" + config.getLogFileName(), "'.'yyyy-MM-dd"));
        } catch (Exception ex) {
            System.out.println("FATAL - COULD NOT SETUP LOG FILE APPENDER - CHECK CONFIGURATION.");
            logger.fatal("Could not setup appender?");
        }
        logger.setAdditivity(false);
        logger.setLevel(Level.DEBUG);
        
        logger.info("metaStats Core Server Version " + version.getBuildNumber() + " (" + version.getBuildName() + ") - Code By " + version.getAuthor());

        /* Light up database */
        Class.forName("org.postgresql.Driver"); 	// load the DB driver
        BoneCPConfig boneCPconfig = new BoneCPConfig();	// create a new configuration object
        boneCPconfig.setJdbcUrl("jdbc:postgresql://" + config.getDbServer() + "/" + config.getDbName());	// set the JDBC url
        boneCPconfig.setUsername(config.getDbUser());			// set the username
        boneCPconfig.setPassword(config.getDbPass());				// set the password

        logger.info("Database Connection Online");
        connectionPool = new BoneCP(boneCPconfig); 	// setup the connection pool

        Server server = new Server(config.getListenPort());
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        
        context.addServlet(new ServletHolder(
                new MetricsServlet()), "/metrics");
        DefaultExports.initialize();
        
        logger.info("Creating timer thread to poll Metaswitch.");
        timer = new Timer();
        
        timer.schedule(dbTask, 300, 300 * 1000); // Run every 5 minutes
        logger.info("Timer created - ready to rock");

        // Put your application setup code here.
        server.start();
        server.join();
        logger.info("Listening on port " + config.getListenPort());
        
    }
    
    static class DBTask extends TimerTask {
        
        Connection connection;
        final Gauge call_atps = Gauge.build().name("call_atps").help("Call Attemps").register();
        final Gauge succful_call_atps = Gauge.build().name("succful_call_atps").help("Successful Call Attemps").register();
        final Gauge inc_call_atps = Gauge.build().name("inc_call_atps").help("Incoming Call Attemps").register();
        final Gauge inc_succful_call_atps = Gauge.build().name("inc_succful_call_atps").help("Successful Incoming Call Attemps").register();
        final Gauge out_call_atps = Gauge.build().name("out_call_atps").help("Outbound Call Attemps").register();
        final Gauge out_succful_call_atps = Gauge.build().name("out_succful_call_atps").help("Successful Outbound Call Attemps").register();
        
        static final Gauge sipTrunkAppsInUse = Gauge.build().name("call_apps_current_in_use").help("Call Appereances Curretnly In Use").labelNames("carrier").register();
        static final Gauge sipTrunkAppsInbound = Gauge.build().name("call_apps_current_in_use_incoming").help("Incoming Call Appereances Curretnly In Use").labelNames("carrier").register();
        static final Gauge sipTrunkAppsOutbound = Gauge.build().name("call_apps_current_in_use_outbound").help("Outbound Call Appereances Curretnly In Use").labelNames("carrier").register();
        static final Gauge sipTrunkAppsUtilization = Gauge.build().name("percentage_utilization").help("Utilization Percentage").labelNames("carrier").register();
        static final Gauge sipTrunkAppsConfigured = Gauge.build().name("apps_currently_configured").help("Appereances Currently Configured").labelNames("carrier").register();
        
        static final Gauge pbxTrunkAppsInUse = Gauge.build().name("lines_chans_in_use").help("Call Appereances Curretnly In Use").labelNames("carrier").register();
        static final Gauge pbxTrunkAppsInbound = Gauge.build().name("inc_lines_chans_in_use").help("Incoming Call Appereances Currently In Use").labelNames("carrier").register();
        static final Gauge pbxTrunkAppsOutbound = Gauge.build().name("out_lines_chans_in_use").help("Outbound Call Appereances Currently In Use").labelNames("carrier").register();
        static final Gauge pbxTrunkAppsConfigured = Gauge.build().name("lines_chans_conf").help("Appearances Currently Configured").labelNames("carrier").register();
        static final Gauge pbxTrunkAppsBurst = Gauge.build().name("burst_lines_chans_in_use").help("Appearances Currently Configured").labelNames("carrier").register();
        
        @Override
        public void run() {
            try {
                StatsServer.getLogInterface().info(("MetaDB running...."));
                connection = StatsServer.connectionPool.getConnection();
                logger.debug("Getting switch overview stats");
                PreparedStatement pstmt = connection.prepareStatement("SELECT statistictimestamp, call_atps, succful_call_atps, inc_call_atps, inc_succful_call_atps, out_call_atps, out_succful_call_atps FROM sip_fiveminutes ORDER BY statistictimestamp DESC LIMIT 1;");
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    throw new Exception("Could not query DB!");
                } else {
                    call_atps.set(rs.getInt("call_atps"));
                    succful_call_atps.set(rs.getInt("succful_call_atps"));
                    inc_call_atps.set(rs.getInt("inc_call_atps"));
                    inc_succful_call_atps.set(rs.getInt("inc_succful_call_atps"));
                    out_call_atps.set(rs.getInt("out_call_atps"));
                    out_succful_call_atps.set(rs.getInt("out_succful_call_atps"));
                }
                logger.debug("Getting SIP overview stats");
                pstmt = connection.prepareStatement("SELECT sip_trunk_name, call_apps_currently_in_use, call_apps_curr_in_use_incoming, call_apps_curr_in_use_outgoing, percentage_utilization, call_apps_currently_configured FROM sip_trunk_fiveminutes WHERE statistictimestamp > CURRENT_TIMESTAMP - INTERVAL '9 minutes'");
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    logger.debug("Building SIP stats for trunk: " + rs.getString("sip_trunk_name"));
                    sipTrunkAppsInUse.labels(rs.getString("sip_trunk_name")).set(rs.getInt("call_apps_currently_in_use"));
                    sipTrunkAppsInbound.labels(rs.getString("sip_trunk_name")).set(rs.getInt("call_apps_curr_in_use_incoming"));
                    sipTrunkAppsOutbound.labels(rs.getString("sip_trunk_name")).set(rs.getInt("call_apps_curr_in_use_outgoing"));
                    sipTrunkAppsUtilization.labels(rs.getString("sip_trunk_name")).set(rs.getInt("percentage_utilization"));
                    sipTrunkAppsConfigured.labels(rs.getString("sip_trunk_name")).set(rs.getInt("call_apps_currently_configured"));
                    
                }
                logger.debug("Getting PBX overview stats");
                pstmt = connection.prepareStatement("SELECT directory_number, lines_chans_conf, lines_chans_in_use, inc_lines_chans_in_use, out_lines_chans_in_use, burst_lines_chans_in_use FROM pbx_fiveminutes WHERE statistictimestamp > CURRENT_TIMESTAMP - INTERVAL '9 minutes'");
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    logger.debug("Building PBX stats for: " + rs.getString("directory_number"));
                    pbxTrunkAppsInUse.labels(rs.getString("directory_number")).set(rs.getInt("lines_chans_in_use"));
                    pbxTrunkAppsInbound.labels(rs.getString("directory_number")).set(rs.getInt("inc_lines_chans_in_use"));
                    pbxTrunkAppsOutbound.labels(rs.getString("directory_number")).set(rs.getInt("out_lines_chans_in_use"));
                    pbxTrunkAppsBurst.labels(rs.getString("directory_number")).set(rs.getInt("burst_lines_chans_in_use"));
                    pbxTrunkAppsConfigured.labels(rs.getString("directory_number")).set(rs.getInt("lines_chans_conf"));
                    
                }
                rs.close();
                pstmt.close();
                
            } catch (Exception ex) {
                logger.fatal("Error running stats: " + ex, ex);
            } finally {
                try {
                    
                    connection.close();
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(StatsServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }
            
        }
    }
    
}
