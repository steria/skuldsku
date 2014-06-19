package no.steria.copito.recorder;

import no.steria.copito.recorder.dbrecorder.DatabaseRecorder;

import java.sql.SQLException;
import java.util.List;

/**
 * Facade for starting and stopping all available recorders.
 */
public class RecorderFacade {
    private static boolean recordingOn = false;

    List<DatabaseRecorder> databaseRecorders;

    public RecorderFacade(List<DatabaseRecorder> databaseRecorders) {
        this.databaseRecorders = databaseRecorders;
    }

    public static boolean recordingIsOn() {
        return recordingOn;
    }

    public void start() throws SQLException {
        if (!recordingIsOn()) {
            for(DatabaseRecorder dbRecorder : databaseRecorders){
                dbRecorder.start();
            }
            recordingOn = true;
        }
    }

    public void stop() {
        if (recordingIsOn()) {
            for(DatabaseRecorder dbRecorder : databaseRecorders){
                dbRecorder.stop();
            }
            recordingOn = false;
        }
    }

}
