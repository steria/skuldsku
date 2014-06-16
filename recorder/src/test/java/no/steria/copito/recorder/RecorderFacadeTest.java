package no.steria.copito.recorder;

import no.steria.copito.recorder.dbrecorder.DatabaseRecorder;
import no.steria.copito.recorder.httprecorder.CallReporter;
import no.steria.copito.recorder.httprecorder.testjetty.InMemoryReporter;
import no.steria.copito.recorder.httprecorder.testjetty.TestFilter;
import no.steria.copito.recorder.javainterfacerecorder.interfacerecorder.*;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RecorderFacadeTest {

    @Mock
    private DatabaseRecorder databaseRecorder1;

    @Mock
    private DatabaseRecorder databaseRecorder2;

    @Mock
    private FilterConfig filterConfig;

    private RecorderFacade recorderFacade;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private HttpServletRequest request;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private HttpServletResponse response;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private FilterChain chain;

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        recorderFacade = new RecorderFacade(Arrays.asList(databaseRecorder1, databaseRecorder2));
        recorderFacade.stop();
    }

    @Test
    public void shouldTurnOnWhenStartIsCalledAndOffWhenStopIsCalled() throws SQLException {
        recorderFacade.start();
        assertTrue(RecorderFacade.recordingIsOn());
        recorderFacade.stop();
        assertFalse(RecorderFacade.recordingIsOn());
        recorderFacade.start();
        assertTrue(RecorderFacade.recordingIsOn());
        recorderFacade.stop();
        assertFalse(RecorderFacade.recordingIsOn());
    }

    @Test
    public void shouldAlsoTurnOnAndOffWhenNoDatabaseRecorder() throws SQLException {
        RecorderFacade recorderFacadeNoRecorder = new RecorderFacade(new ArrayList<DatabaseRecorder>(0));
        assertFalse(RecorderFacade.recordingIsOn());
        recorderFacadeNoRecorder.start();
        assertTrue(RecorderFacade.recordingIsOn());
        recorderFacadeNoRecorder.stop();
        Assert.assertFalse(RecorderFacade.recordingIsOn());
    }

    @Test
    public void shouldNotTurnDbRecordingOffWhenAlreadyOff() {
        verify(databaseRecorder1, atMost(1)).stop(); // could be called from setUp()
        verify(databaseRecorder2, atMost(1)).stop(); // could be called from setUp()
        recorderFacade.stop();
        verifyNoMoreInteractions(databaseRecorder1);
        verifyNoMoreInteractions(databaseRecorder2);
    }

    @Test
    public void shouldTurnDbRecordingOnWhenRecordingTurnedOn() throws SQLException {
        recorderFacade.start();
        verify(databaseRecorder1, times(1)).start();
        verify(databaseRecorder2, times(1)).start();
    }

    @Test
    public void shouldNotTurnDbRecordingOnWhenAlreadyOn() throws SQLException {
        recorderFacade.start();
        recorderFacade.start();
        verify(databaseRecorder1, times(1)).start();
        verify(databaseRecorder2, times(1)).start();
    }

    @Test
    public void shouldTurnDbRecordingOffWhenRecordingTurnedOff() throws SQLException {
        recorderFacade.start();
        recorderFacade.stop();
        verify(databaseRecorder1, times(2)).stop(); // including the stop() that is executed in the setUp to "reset" the facade between tests.
        verify(databaseRecorder2, times(2)).stop(); // including the stop() that is executed in the setUp to "reset" the facade between tests.
    }

    @Test
    public void shouldExportDbInteractionsToFile() throws IOException {
        File file = prepareFile();
        recorderFacade.exportTo(file);
        verify(databaseRecorder1, times(1)).exportTo(any(PrintWriter.class));
        verify(databaseRecorder2, times(1)).exportTo(any(PrintWriter.class));
    }

    @Test
    public void shouldExportHttpInteractionsToFile() throws ServletException, IOException, SQLException {
        @SuppressWarnings("unchecked")
        Enumeration<String> headerNames = new IteratorEnumeration(new ArrayList<String>().iterator());
        recorderFacade.resetFilterRegister();

        when(request.getHeaderNames()).thenReturn(headerNames);
        recorderFacade.start();
        CallReporter callReporter = new InMemoryReporter();
        TestFilter testFilter = new TestFilter(callReporter);
        testFilter.init(filterConfig);
        testFilter.doFilter(request, response, chain);
        File exportFile = prepareFile();
        recorderFacade.exportTo(exportFile);
        Scanner scanner = new Scanner(exportFile);
        String content = scanner.useDelimiter("\\Z").next();
        scanner.close();
        recorderFacade.resetFilterRegister();
        assertEquals("<no.steria.copito.recorder.httprecorder.ReportObject;readInputStream=<null>" +
                ";parameters=<map>;method=;path=;output=<null>;headers=<map>>", content);
    }

    @Test
    public void shouldExportJavaApiInteractionsToFile() throws IOException, SQLException {
        recorderFacade.start();
        prepareDataMock();
        ServiceInterface serviceClass = prepareRecordingProxy();

        serviceClass.doSimpleService("MyName");
        File exportFile = prepareFile();
        recorderFacade.exportTo(exportFile);

        Scanner scanner = new Scanner(exportFile);
        String content = scanner.useDelimiter("\\Z").next();
        scanner.close();
        MockRegistration.removeMock(ServiceInterface.class);
        assertEquals("<no.steria.copito.recorder.javainterfacerecorder.interfacerecorder.RecordedDataMock;recorded=<list>>", content);
    }

    private ServiceInterface prepareRecordingProxy() {
        ReportCallback reportCallback = new DummyReportCallback();
        return InterfaceRecorderWrapper.newInstance(new ServiceClass(), ServiceInterface.class, reportCallback, InterfaceRecorderConfig.factory().withAsyncMode(AsyncMode.ALL_SYNC).create());
    }

    private void prepareDataMock() {
        List<RecordObject> recorded = new ArrayList<>();
        RecordedDataMock recordedDataMock = new RecordedDataMock(recorded);
        MockRegistration.registerMock(ServiceInterface.class, recordedDataMock);
    }


    private File prepareFile() throws IOException {
        URL resource = this.getClass().getResource("/dbrecorder");
        File file = new File(resource.getPath() + "/testdata.txt");
        if (file.exists()) {
            boolean delete = file.delete();
            assertTrue("Old file could not be deleted: " + file.getAbsolutePath(), delete);
        }
        boolean newFile = file.createNewFile();
        assertTrue("New file could not be created", newFile);
        return file;
    }
}