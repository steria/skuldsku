package no.steria.skuldsku.testrunner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.steria.skuldsku.DatabaseTableNames.SKULDSKU_DATABASE_TABLE_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

public class DbToFileExporterTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReadDatabaseInteractionRecordingsAndWriteToOutputStream() throws SQLException, IOException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.getResultSet()).thenReturn(resultSet);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn(SKULDSKU_DATABASE_TABLE_PREFIX + "ID").thenReturn("SERVICE").thenReturn("THREAD");
        when(resultSet.getString(2)).thenReturn("CLIENT_IDENTIFIER").thenReturn("METHOD").thenReturn("METHOD");
        when(resultSet.getString(3)).thenReturn("SESSION_USER").thenReturn(null).thenReturn("PATH");
        when(resultSet.getString(4)).thenReturn(null).thenReturn("RESULT").thenReturn("DATA");
        when(resultSet.getString(5)).thenReturn("TABLE_NAME").thenReturn("CREATED").thenReturn("TIMEST");
        when(resultSet.getString(6)).thenReturn("ACTION").thenReturn("THREAD_ID");
        when(resultSet.getString(7)).thenReturn("DATAROW").thenReturn("fine\",\" data");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DbToFileExporter.exportTo(baos, dataSource);
        assertEquals("\n" +
                " **DATABASE RECORDINGS** \n\n" +
                        "\"SKS_ID\",\"CLIENT_IDENTIFIER\",\"SESSION_USER\",,\"TABLE_NAME\",\"ACTION\",\"DATAROW\"\n" +
                        "\"SERVICE\",\"METHOD\",,\"RESULT\",\"CREATED\",\"THREAD_ID\",\"fine\"\",\"\" data\"\n\n" +
                        " **JAVA INTERFACE RECORDINGS** \n\n" +
                        "\"THREAD\",\"METHOD\",\"PATH\",\"DATA\",\"TIMEST\",\"THREAD_ID\",\"fine\"\",\"\" data\"\n\n" +
                        " **HTTP RECORDINGS** \n\n" +
                        "\"THREAD\",\"METHOD\",\"PATH\",\"DATA\",\"TIMEST\",\"THREAD_ID\"\n", baos.toString());
    }
}
