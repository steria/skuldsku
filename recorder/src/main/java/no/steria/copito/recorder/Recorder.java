package no.steria.copito.recorder;

import no.steria.copito.recorder.dbrecorder.DatabaseRecorder;

import java.sql.SQLException;
import java.util.List;

/**
 * Facade for starting and stopping all available recorders.
 */
public class Recorder {
    private static boolean recordingOn = false;

    public static final String COPITO_DATABASE_TABLE_PREFIX = "CPT_";

    List<DatabaseRecorder> databaseRecorders;

    public Recorder(List<DatabaseRecorder> databaseRecorders) {
        this.databaseRecorders = databaseRecorders;
        initializeDbRecorders(databaseRecorders);
    }

    public static boolean recordingIsOn() {
        return recordingOn;
    }

    public void start() throws SQLException {
        if (!recordingIsOn()) {
            for (DatabaseRecorder dbRecorder : databaseRecorders) {
                dbRecorder.start();
            }
            recordingOn = true;
        }
    }

    public void stop() {
        if (recordingIsOn()) {
            for (DatabaseRecorder dbRecorder : databaseRecorders) {
                dbRecorder.stop();
            }
            recordingOn = false;
        }
    }

    private void initializeDbRecorders(List<DatabaseRecorder> databaseRecorders) {
        for(DatabaseRecorder databaseRecorder : databaseRecorders) {
            databaseRecorder.setup();
        }
    }
}
