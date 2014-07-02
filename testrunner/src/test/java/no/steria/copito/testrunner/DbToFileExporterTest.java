package no.steria.copito.testrunner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.steria.copito.recorder.Recorder.COPITO_DATABASE_TABLE_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class DbToFileExporterTest {

    DbToFileExporter dbToFileExporter;

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    PreparedStatement preparedStatement;

    @Mock
    ResultSet resultSet;

    @Before
    public void setUp() throws Exception {
        dataSource = mock(DataSource.class, withSettings().verboseLogging());
        connection = mock(Connection.class, withSettings().verboseLogging());
        preparedStatement = mock(PreparedStatement.class, withSettings().verboseLogging());
        resultSet = mock(ResultSet.class, withSettings().verboseLogging());
        dbToFileExporter = new DbToFileExporter(COPITO_DATABASE_TABLE_PREFIX + "JAVA_LOGG", "table2", "table3", dataSource);
    }

    @Test
    public void shouldReadDatabaseInteractionRecordingsAndWriteToOutputStream() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.getResultSet()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn(COPITO_DATABASE_TABLE_PREFIX + "ID").thenReturn("SERVICE").thenReturn("THREAD");
        when(resultSet.getString(2)).thenReturn("CLIENT_IDENTIFIER").thenReturn("METHOD").thenReturn("METHOD");
        when(resultSet.getString(3)).thenReturn("SESSION_USER").thenReturn(null).thenReturn("PATH");
        when(resultSet.getString(4)).thenReturn(null).thenReturn("RESULT").thenReturn("DATA");
        when(resultSet.getString(5)).thenReturn("TABLE_NAME").thenReturn("CREATED").thenReturn("TIMEST");
        when(resultSet.getString(6)).thenReturn("ACTION").thenReturn("THREAD_ID");
        when(resultSet.getString(7)).thenReturn("DATAROW").thenReturn("TIMEST");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dbToFileExporter.exportTo(baos);
        assertEquals(" **DATABASE RECORDINGS** " + //
                "\"" + COPITO_DATABASE_TABLE_PREFIX + "ID\",\"CLIENT_IDENTIFIER\",\"SESSION_USER\",\"\",\"TABLE_NAME\",\"ACTION\",\"DATAROW\";" + //
                "\"SERVICE\",\"METHOD\",\"\",\"RESULT\",\"CREATED\",\"THREAD_ID\",\"TIMEST\"; " + //
                "**INTERFACE RECORDINGS** " + //
                "\"THREAD\",\"METHOD\",\"PATH\",\"DATA\",\"TIMEST\",\"THREAD_ID\",\"TIMEST\"; " + //
                "**HTTP RECORDINGS** " + //
                "\"THREAD\",\"METHOD\",\"PATH\",\"DATA\",\"TIMEST\";", baos.toString());
    }
}
