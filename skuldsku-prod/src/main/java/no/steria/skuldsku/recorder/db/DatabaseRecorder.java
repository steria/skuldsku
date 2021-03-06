package no.steria.skuldsku.recorder.db;

import java.io.PrintWriter;

/**
 * Service for monitoring database changes.
 */
public interface DatabaseRecorder {

    /**
     * Initializes the recorder. The initialization only need to be performed
     * once on each database unless {@link #tearDown()} is invoked to remove
     * the recorder completely from the database.
     */
    public void initialize();
    
    /**
     * Starts recording database changes.
     */
    public void start();
    
    /**
     * Stops recording database changes.
     */
    public void stop();
   
    /**
     * Exports recorded database changes.
     * 
     * @param out The output stream to which the database changes will
     *          be written.
     */
    public void exportTo(final PrintWriter out);

    /**
     * Exports recorded database changes and wipes the data from the database
     *
     * @param out The output stream to which the database changes will
     *          be written.
     */
    public void exportAndRemove(final PrintWriter out);
    
    /**
     * Removes the recorder for the database by removing any recording tables,
     * triggers and recorded data.
     */
    public void tearDown();
}