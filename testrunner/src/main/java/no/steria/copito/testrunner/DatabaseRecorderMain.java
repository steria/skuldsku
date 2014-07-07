/**
 * A command line interface for running the database recorder, and for rolling the database back.
 */
package no.steria.copito.testrunner;

import com.jolbox.bonecp.BoneCPDataSource;
import no.steria.copito.recorder.dbrecorder.DatabaseRecorder;
import no.steria.copito.recorder.dbrecorder.impl.oracle.OracleDatabaseRecorder;
import no.steria.copito.recorder.logging.RecorderLog;
import no.steria.copito.testrunner.dbrunner.dbchange.DatabaseChangeRollback;

import javax.sql.DataSource;
import java.io.*;

/**
 * Support for running the <code>DatbaseRecorder</code> from the command-line.
 *
 * @see no.steria.copito.recorder.dbrecorder.DatabaseRecorder
 */
public class DatabaseRecorderMain {

    static DataSource createDataSource(String jdbcUrl, String username, String password) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        final BoneCPDataSource dataSource = new BoneCPDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    static void printUsage() {
        System.out.println("Usage: dbrecorder JDBC_URL USERNAME PASSWORD setup|start|stop|export file database_recordings_table http_recordings_table" +
                " java_interface_recordings_table|tearDown|rollback FILE");
    }

    static void testMain (String[] args, DataSource dataSource) throws FileNotFoundException {
        switchCommand(args, dataSource);
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length < 4) {
            printUsage();
            System.exit(1);
        }

        final String jdbcUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final DataSource dataSource = createDataSource(jdbcUrl, username, password);
        switchCommand(args, dataSource);
    }

    private static void switchCommand(String[] args, DataSource dataSource) {
        final DatabaseRecorder databaseRecorder = new OracleDatabaseRecorder(dataSource);
        final DatabaseChangeRollback rollback = new DatabaseChangeRollback(dataSource);

        int i = 3;

        while (i < args.length) {
            if (args[i].equals("setup")) {
                databaseRecorder.setup();
                System.out.println("Database recorder initialized but not started. Invoke \"start\" to begin recording.");
            } else if (args[i].equals("start")) {
                databaseRecorder.start();
                System.out.println("Database recording started.");
            } else if (args[i].equals("stop")) {
                databaseRecorder.stop();
                System.out.println("Database recording stopped.");
            } else if (args[i].equals("export")) {
                i++;
                final String exportFile = args[i];
                try (OutputStream os = new FileOutputStream(exportFile)) {
                    DbToFileExporter.exportTo(os, args[++i], args[++i], args[++i], dataSource);
                } catch (IOException e) {
                    RecorderLog.error("Could not write to specified file.", e);
                }
                System.out.println("Data exported to: " + exportFile);
            } else if (args[i].equals("tearDown")) {
                databaseRecorder.tearDown();
                System.out.println("Database recording stopped and data cleared.");
            } else if (args[i].equals("rollback")) {
                i++;
                final File rollbackFile = new File(args[i]);
                rollback.rollback(rollbackFile);
                System.out.println("Database changes rolled back: " + args[i]);
            } else {
                System.err.println("Unknown command: " + args[i]);
                printUsage();
                System.exit(1);
            }
            i++;
        }
    }
}
