package no.steria.copito.recorder.dbrecorder.impl.oracle;

import no.steria.copito.recorder.dbrecorder.DatabaseRecorder;
import no.steria.copito.recorder.logging.RecorderLog;
import no.steria.copito.utils.*;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static no.steria.copito.DatabaseTableNames.COPITO_DATABASE_TABLE_PREFIX;
import static no.steria.copito.DatabaseTableNames.DATABASE_RECORDINGS_TABLE;

public class OracleDatabaseRecorder implements DatabaseRecorder {

    private static final int MAX_TRIGGER_NAME_LENGTH = 30;


    private final TransactionManager transactionManager;

    private boolean initialized = false;

    public OracleDatabaseRecorder(DataSource dataSource) {
        this(new SimpleTransactionManager(dataSource));
    }

    OracleDatabaseRecorder(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void setup() {
        createRecorderTable();
        initialized = true;
    }

    @Override
    public void start() {
        if (initialized) {
            createTriggers();
        } else {
            // If initialize has not run, we risk obstructing the application under recording if running
            // this method anyway. This check causes a slight overhead, but I believe it is worth it for the extra security.
            RecorderLog.error("You cannot call start() on " + DatabaseRecorder.class.getSimpleName() +
                   " before you have called initialize(). Database calls are NOT being recorded.");
        }
    }

    @Override
    public void stop() {
        dropRecorderTriggers();
    }

    @Override
    public void tearDown() {
        dropRecorderTriggers();
        dropRecorderTable();
    }

    @Override
    public void exportTo(final PrintWriter out) {
            transactionManager.doInTransaction(new TransactionCallback<Object>() {
                @Override
                public Object callback(Jdbc jdbc) {
                jdbc.query("SELECT 'CLIENT_IDENTIFIER='||CLIENT_IDENTIFIER||';SESSION_USER='||SESSION_USER||';SESSIONID='|" +
                        "|SESSIONID||';TABLE_NAME='||TABLE_NAME||';ACTION='||ACTION||';'||DATAROW AS DATA FROM " +
                        DATABASE_RECORDINGS_TABLE, new ResultSetCallback() {
                    @Override
                    public void extractData(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            out.println(rs.getString(1));
                        }
                    }
                });
                return null;
            }
        });
    }

    public void exportAndRemove(final PrintWriter out) {
        final List<String> retrievedDataIds = new ArrayList<>();
        transactionManager.doInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object callback(Jdbc jdbc) {
                jdbc.query("SELECT " + COPITO_DATABASE_TABLE_PREFIX + "ID, 'CLIENT_IDENTIFIER='||CLIENT_IDENTIFIER||';SESSION_USER='||SESSION_USER||';SESSIONID='||SESSIONID||';TABLE_NAME='||TABLE_NAME||';ACTION='||ACTION||';'||DATAROW AS DATA FROM " + DATABASE_RECORDINGS_TABLE, new ResultSetCallback() {
                    @Override
                    public void extractData(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            retrievedDataIds.add(rs.getString(1));
                            out.println(rs.getString(2));
                        }
                    }
                });
                return null;
            }
        });
        out.flush();

        /*
         * Deleting after select in order to avoid forcing the database to maintain
         * two different versions of the data while the select is running. 
         */
        transactionManager.doInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object callback(Jdbc jdbc) {
                for (String id : retrievedDataIds) {
                    jdbc.execute("DELETE FROM " + DATABASE_RECORDINGS_TABLE + " WHERE " + COPITO_DATABASE_TABLE_PREFIX + "ID = " + id);
                }
                return null;
            }
        });
    }


    void dropRecorderTable() {
        transactionManager.doInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object callback(Jdbc jdbc) {
                try {
                    jdbc.execute("DROP TABLE " + DATABASE_RECORDINGS_TABLE);
                } catch (JdbcException e) {
                    RecorderLog.error("Could not drop table " + DATABASE_RECORDINGS_TABLE, e);
                }
                try {
                    jdbc.execute("DROP SEQUENCE " + COPITO_DATABASE_TABLE_PREFIX + "RECORDER_ID_SEQ");
                } catch (JdbcException e) {
                    RecorderLog.error("Could not drop sequence " + COPITO_DATABASE_TABLE_PREFIX + "RECORDER_ID_SEQ", e);
                }
                return null;
            }
        });
    }

    private void dropRecorderTriggers() {
        transactionManager.doInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object callback(Jdbc jdbc) {
                for (String triggerName : getTriggerNames(jdbc)) {
                    if (isRecorderResource(triggerName)) {
                        jdbc.execute("DROP TRIGGER " + triggerName);
                    }
                }
                return null;
            }
        });
    }

    List<String> getTriggerNames(Jdbc jdbc) {
        return jdbc.queryForList("SELECT TRIGGER_NAME FROM USER_TRIGGERS", String.class);
    }

    void createRecorderTable() {
        transactionManager.doInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object callback(Jdbc jdbc) {
                createRecorderTableIfNotExitst(jdbc);
                createDbSequenceIfNotExists(jdbc);
                return null;
            }
        });
    }

    private void createRecorderTableIfNotExitst(Jdbc jdbc) {
        List<String> recorderTable = jdbc.queryForList(
                "select table_name from all_tables where table_name='" + DATABASE_RECORDINGS_TABLE + "'", String.class);
        if (recorderTable.isEmpty()) {
            jdbc.execute("CREATE TABLE " + DATABASE_RECORDINGS_TABLE + " (\n" +
                    "    CPT_ID             NUMBER,\n" +
                    "    CLIENT_IDENTIFIER  VARCHAR2(256),\n" +
                    "    SESSION_USER       VARCHAR2(256),\n" +
                    "    SESSIONID          VARCHAR2(256),\n" +
                    "    TABLE_NAME         VARCHAR2(30),\n" +
                    "    ACTION             VARCHAR2(6),\n" +
                    "    DATAROW            CLOB    \n" +
                    ")");
        }
    }

    private void createDbSequenceIfNotExists(Jdbc jdbc) {
        List<String> recorderTrigger = jdbc.queryForList(
                "select sequence_name from all_sequences where sequence_name='" + COPITO_DATABASE_TABLE_PREFIX + "RECORDER_ID_SEQ'", String.class);

        if(recorderTrigger.isEmpty()) {
            jdbc.execute("CREATE SEQUENCE " + COPITO_DATABASE_TABLE_PREFIX + "RECORDER_ID_SEQ");
        }
    }

    void createTriggers() {
        transactionManager.doInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object callback(Jdbc jdbc) {
                for (String tableName : getTableNames(jdbc)) {
                    if (isRecorderResource(tableName)) {
                        continue;
                    }
                    final List<String> columnNames = getColumnNames(jdbc, tableName);
                    if (columnNames.isEmpty()) {
                        RecorderLog.debug("Ignoring table with no columns: " + tableName);
                        continue;
                    }
                    try {
                        createTrigger(jdbc, tableName, columnNames);
                    } catch (JdbcException e) {
                        RecorderLog.debug("Ignoring table: " + tableName + " (" + e.getMessage() + ")");
                    }
                }
                return null;
            }
        });
    }

    private void createTrigger(Jdbc jdbc, String tableName, final List<String> columnNames) {
        final String triggerSql = TriggerSqlGenerator.generateTriggerSql(
                reduceToMaxLength(COPITO_DATABASE_TABLE_PREFIX + tableName, MAX_TRIGGER_NAME_LENGTH),
                tableName,
                columnNames);

        jdbc.execute(triggerSql);
    }

    List<String> getTableNames(Jdbc jdbc) {
        return jdbc.queryForList("SELECT TABLE_NAME FROM USER_TABLES", String.class);
    }

    List<String> getColumnNames(Jdbc jdbc, String tableName) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM USER_TAB_COLS WHERE TABLE_NAME = ? ORDER BY COLUMN_NAME",
                String.class, tableName);
    }

    private boolean isRecorderResource(String resourceName) {
        return resourceName.startsWith(COPITO_DATABASE_TABLE_PREFIX);
    }

    String reduceToMaxLength(String s, int length) {
        return (s.length() <= length) ? s : s.substring(0, length);
    }
}
