package no.steria.skuldsku.testrunner.dbrunner.dbverifier.verifiers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import no.steria.skuldsku.common.result.Results;
import no.steria.skuldsku.testrunner.dbrunner.dbchange.DatabaseChange;
import no.steria.skuldsku.testrunner.dbrunner.dbverifier.DatabaseChangeVerifier;
import no.steria.skuldsku.testrunner.dbrunner.dbverifier.DatabaseChangeVerifierOptions;

public class SortedDatabaseChangeVerifier implements DatabaseChangeVerifier {

    private final StrictOrderDatabaseChangeVerifier strictOrderDatabaseChangeVerifier = new StrictOrderDatabaseChangeVerifier();
    private final List<String> sortCriterias;
    
    
    public SortedDatabaseChangeVerifier(List<String> sortCriterias) {
        this.sortCriterias = sortCriterias;
    }
    
    
    @Override
    public Results assertEquals(final List<DatabaseChange> expected, final List<DatabaseChange> actual, final DatabaseChangeVerifierOptions databaseChangeVerifierOptions) {
        final Comparator<DatabaseChange> identifierSort = new Comparator<DatabaseChange>() {
            @Override
            public int compare(DatabaseChange change1, DatabaseChange change2) {
                for (String sortCriteria : sortCriterias) {
                    final String v1 = change1.getValue(sortCriteria);
                    final String v2 = change1.getValue(sortCriteria);
                    if (v1 == null && v2 != null) {
                        return -1;
                    } else if (v1 != null && v2 == null) {
                        return 1;
                    }
                    if (v1 == null && v2 == null) {
                        continue;
                    }
                    final int ret = v1.compareTo(v2);
                    if (ret != 0) {
                        return ret;
                    }
                }
                for (String key : change1.getData().keySet()) {
                    if (databaseChangeVerifierOptions.getSkipFields().contains(key)) {
                        continue;
                    }
                    final int ret = change1.getValue(key).compareTo(change2.getValue(key));
                    if (ret != 0) {
                        return ret;
                    }
                }
                return 0;
            }
        };
        Collections.sort(expected, identifierSort);
        Collections.sort(actual, identifierSort);
        
        return strictOrderDatabaseChangeVerifier.assertEquals(expected, actual, databaseChangeVerifierOptions);
    }
}
