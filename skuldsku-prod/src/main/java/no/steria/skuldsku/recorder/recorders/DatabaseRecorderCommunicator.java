package no.steria.skuldsku.recorder.recorders;

import no.steria.skuldsku.DatabaseTableNames;
import no.steria.skuldsku.recorder.logging.RecorderLog;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseRecorderCommunicator extends AbstractRecorderCommunicator {
    private final DataSource dataSource;
    private static final String TABLENAME = DatabaseTableNames.SKULDSKU_DATABASE_TABLE_PREFIX + "RECORD";

    public DatabaseRecorderCommunicator(DataSource dataSource) {
        this.dataSource = dataSource;
        try (Connection conn = dataSource.getConnection()) {
            RecorderLog.debug("Got connection.");
            if (!tableExists(conn)) {
                createTable(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void createTable(Connection conn) throws SQLException {
        System.out.println("Creating table");
        String sql = "CREATE TABLE " + TABLENAME +
                "   (DATA CLOB null,  " +
                "CREATED TIMESTAMP (6) DEFAULT sysdate)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private boolean tableExists(Connection conn) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("select table_name from all_tables where table_name='" + TABLENAME + "'")) {
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }


    @Override
    protected void saveRecord(String res) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement("insert into " + TABLENAME + "(data) values (?)")) {
                statement.setString(1, res);
                statement.execute();
            }
        RecorderLog.debug("committing recording");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } 
    }
}
