package no.steria.copito.recorder;

import no.steria.copito.recorder.dbrecorder.DatabaseRecorder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RecorderFacadeTest {

    @Mock
    private DatabaseRecorder databaseRecorder;

    private RecorderFacade recorderFacade;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        recorderFacade = new RecorderFacade(databaseRecorder);
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
    public void shouldNotTurnDbRecordingOffWhenAlreadyOff() throws SQLException {
        recorderFacade.stop();
        verifyNoMoreInteractions(databaseRecorder);
    }

    @Test
    public void shouldTurnDbRecordingOnWhenRecordingTurnedOn() throws SQLException {
        recorderFacade.start();
        verify(databaseRecorder, times(1)).start();
    }

    @Test
    public void shouldNotTurnDbRecordingOnWhenAlreadyOn() throws SQLException {
        recorderFacade.start();
        recorderFacade.start();
        verify(databaseRecorder, times(1)).start();
    }

    @Test
    public void shouldTurnDbRecordingOffWhenRecordingTurnedOff() throws SQLException {
        recorderFacade.start();
        recorderFacade.stop();
        verify(databaseRecorder, times(2)).stop(); // including the stop() that is executed in the setUp to "reset" the facade between tests.

    }

    @Test
    public void shouldNotThrowWhenDatabaseRecorderIsNull() throws SQLException {
        RecorderFacade recorderFacadeWithNull = new RecorderFacade(null);
        recorderFacadeWithNull.start();
        recorderFacadeWithNull.stop();
    }

}
