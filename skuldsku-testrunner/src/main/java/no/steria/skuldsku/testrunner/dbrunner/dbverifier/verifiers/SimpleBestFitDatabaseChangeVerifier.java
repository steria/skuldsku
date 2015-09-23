package no.steria.skuldsku.testrunner.dbrunner.dbverifier.verifiers;

import no.steria.skuldsku.testrunner.dbrunner.dbchange.DatabaseChange;
import no.steria.skuldsku.testrunner.dbrunner.dbverifier.DatabaseChangeVerifier;
import no.steria.skuldsku.testrunner.dbrunner.dbverifier.DatabaseChangeVerifierOptions;
import no.steria.skuldsku.testrunner.dbrunner.dbverifier.DatabaseChangeVerifierResult;

import java.util.*;

public class SimpleBestFitDatabaseChangeVerifier implements DatabaseChangeVerifier {

    @Override
    public DatabaseChangeVerifierResult assertEquals(List<DatabaseChange> expected, List<DatabaseChange> actual, DatabaseChangeVerifierOptions databaseChangeVerifierOptions) {
        final DatabaseChangeVerifierResult databaseChangeVerifierResult = new DatabaseChangeVerifierResult();
        
        /*
         * Sort list so that expected data with best fit (ie the most number of fields match a given
         * actual data row) gets treated first.
         */
        final List<DatabaseChange> expectedSorted = createSortedListOnBestFit(expected, actual, databaseChangeVerifierOptions.getSkipFields());
        
        /*
         * Connect each expected data row to the actual data rows with the best fit. Note that already
         * matched data cannot be used.
         */
        final Set<DatabaseChange> candidates = new HashSet<DatabaseChange>(actual);
        for (DatabaseChange expectedDatabaseChange : expectedSorted) {
            final DatabaseChange actualDatabaseChange = determineBestFitFor(expectedDatabaseChange, candidates, databaseChangeVerifierOptions.getSkipFields());
            if (actualDatabaseChange == null) {
                databaseChangeVerifierResult.addMissingFromActual(expectedDatabaseChange);
            } else {
                candidates.remove(actualDatabaseChange);
                final boolean match = expectedDatabaseChange.equals(actualDatabaseChange, databaseChangeVerifierOptions.getSkipFields());
                if (!match) {
                    databaseChangeVerifierResult.addNotEquals(expectedDatabaseChange, actualDatabaseChange);
                }
            }
        }
        for (DatabaseChange unmatchedCandidate : candidates) {
            databaseChangeVerifierResult.addAdditionalInActual(unmatchedCandidate);
        }
        
        return databaseChangeVerifierResult;
    }

    private static List<DatabaseChange> createSortedListOnBestFit(final List<DatabaseChange> expected,
            final List<DatabaseChange> actual, final Set<String> skipFields) {
        final List<DatabaseChange> expectedSorted = new ArrayList<DatabaseChange>(expected);
        Collections.sort(expectedSorted, new Comparator<DatabaseChange>() {
            @Override
            public int compare(DatabaseChange dc1, DatabaseChange dc2) {
                final DatabaseChange bestFitForDc1 = determineBestFitFor(dc1, actual, skipFields);
                final int fieldsMatchDc1 = (bestFitForDc1 != null) ? dc1.fieldsMatched(bestFitForDc1, skipFields) : 0;
                
                final DatabaseChange bestFitForDc2 = determineBestFitFor(dc2, actual, skipFields);
                final int fieldsMatchDc2 = (bestFitForDc2 != null) ? dc2.fieldsMatched(bestFitForDc2, skipFields) : 0;
                
                return Integer.compare(fieldsMatchDc2, fieldsMatchDc1);
            }
        });
        return expectedSorted;
    }
    
    private static DatabaseChange determineBestFitFor(DatabaseChange d, Collection<DatabaseChange> candidates, Set<String> skipFields) {
        DatabaseChange bestFit = null;
        int bestFieldsMatched = -1;
        
        for (DatabaseChange candidate : candidates) {
            final int fieldsMatched = d.fieldsMatched(candidate, skipFields);
            if (fieldsMatched > bestFieldsMatched) {
                bestFit = candidate;
                bestFieldsMatched = fieldsMatched;
            }
        }
        return bestFit;
    }
}
