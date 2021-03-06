package no.steria.skuldsku.testrunner.dbrunner.dbchange;

import no.steria.skuldsku.recorder.logging.RecorderLog;
import no.steria.skuldsku.utils.*;

import javax.sql.DataSource;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

public class DatabaseChangeRollback {

    private static final String ROW = "ROW.";
    private static final String OLDROW = "OLDROW.";
    
    private final TransactionManager transactionManager;
    
    
    public DatabaseChangeRollback(DataSource dataSource) {
        this(new SimpleTransactionManager(dataSource));
    }
    
    DatabaseChangeRollback(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    
    
    public void rollback(File f) {
        final List<DatabaseChange> databaseChanges = DatabaseChange.readDatabaseChanges(f);
        rollback(databaseChanges);
    }
    
    public static String generateRollbackScript(final List<DatabaseChange> databaseChanges) {
        final StringBuilder sb = new StringBuilder();
        for (int i=databaseChanges.size()-1; i>=0; i--) {
            final String rollbackSql = DatabaseChangeRollback.generateRollbackSql(databaseChanges.get(i));
            sb.append(rollbackSql + ";\n");
        }
        return sb.toString();
    }
    
    public void rollback(final List<DatabaseChange> databaseChanges) {
        transactionManager.doInTransaction(jdbc -> {
            // TODO: Test at disable + reenable virker
            final List<String> triggerNames = getEnabledTriggerNames(jdbc);
            disableTriggers(jdbc, triggerNames);

            try {
                for (int i=databaseChanges.size()-1; i>=0; i--) {
                    final String rollbackSql = DatabaseChangeRollback.generateRollbackSql(databaseChanges.get(i));
                    jdbc.execute(rollbackSql);
                }
            } finally {
                enableTriggers(jdbc, triggerNames);
            }
            return null;
        });
    }
    
    List<String> getEnabledTriggerNames(Jdbc jdbc) {
        return jdbc.queryForList("SELECT TRIGGER_NAME FROM USER_TRIGGERS WHERE STATUS = 'ENABLED'", String.class);
    }
    
    void enableTrigger(Jdbc jdbc, String triggerName) {
        jdbc.execute("ALTER TRIGGER \"" + triggerName + "\" ENABLE");
    }
    
    void enableTriggers(Jdbc jdbc, final List<String> triggerNames) {
        for (String triggerName : triggerNames) {
            try {
                enableTrigger(jdbc, triggerName);
            } catch (JdbcException e) {
                RecorderLog.debug("Reenabling of trigger \"" + triggerName + "\" failed.");
            }
        }
    }
    
    void disableTrigger(Jdbc jdbc, String triggerName) {
        jdbc.execute("ALTER TRIGGER \"" + triggerName + "\" DISABLE");
    }
    
    void disableTriggers(Jdbc jdbc, final List<String> triggerNames) {
        RecorderLog.debug("Disabling triggers. Run these SQLs in case later reenabling fails:");
        for (String triggerName : triggerNames) {
            RecorderLog.debug("    ALTER TRIGGER \"" + triggerName + "\" ENABLE;");
            disableTrigger(jdbc, triggerName);
        }
        RecorderLog.debug("");
    }
    
    public static String generateRollbackSql(DatabaseChange databaseChange) {
        final String action = databaseChange.getAction();

        if (action.equals("DELETE")) {
            return generateDeleteRollback(databaseChange);
        } else if (action.equals("INSERT")) {
            return generateInsertRollback(databaseChange);
        } else if (action.equals("UPDATE")) {
            return generateUpdateRollback(databaseChange);
        } else {
            throw new IllegalStateException("Unknown action: " + action);
        }
    }

    private static String generateUpdateRollback(DatabaseChange databaseChange) {
        final String setValues = generateSetValuesUsingOldValues(databaseChange);
        final String whereConditions = generateWhereConditionsUsingNewValues(databaseChange);
        
        return "UPDATE " + databaseChange.getTableName() + " SET " + setValues + " WHERE " + whereConditions;
    }


    private static String generateSetValuesUsingOldValues(DatabaseChange databaseChange) {
        final StringBuilder values = new StringBuilder();
        boolean append = false;
        for (Entry<String, String> entry :databaseChange. getFields(OLDROW)) {
            if (append) {
                values.append(", ");
            }
            final String name = stripQualifier(entry.getKey());
            
            String value = entry.getValue();
            if (value == null) {
                value = "null";
            } else {
                value = "'" + value + "'";
            }
            values.append(name).append("=").append(value).append("");
            append = true;
        }
        return values.toString();
    }

    private static String generateInsertRollback(DatabaseChange databaseChange) {
        final String whereConditions = generateWhereConditionsUsingNewValues(databaseChange);      
        
        return "DELETE FROM " + databaseChange.getTableName() + " WHERE " + whereConditions;
    }


    private static String generateWhereConditionsUsingNewValues(DatabaseChange databaseChange) {
        final StringBuilder where = new StringBuilder();
        boolean append = false;
        for (Entry<String, String> entry : databaseChange.getFields(ROW)) {
            if (append) {
                where.append(" AND ");
            }
            final String name = stripQualifier(entry.getKey());
            final String value = entry.getValue();
            if (value != null) {
                where.append("TO_CHAR(").append(name).append(")='").append(value).append("'");
            } else {
                where.append(name).append(" IS NULL");
            }
            append = true;
        }
        return where.toString();
    }

    private static String generateDeleteRollback(DatabaseChange databaseChange) {
        final StringBuilder names = new StringBuilder();
        final StringBuilder values = new StringBuilder();
        
        boolean append = false;
        for (Entry<String, String> entry : databaseChange.getFields(ROW)) {
            if (append) {
                names.append(", ");
                values.append(", ");
            }
            names.append(stripQualifier(entry.getKey()));
            
            final String value = entry.getValue();
            if (value != null) {
                values.append("'").append(value).append("'");
            } else {
                values.append("null");
            }
            
            append = true;
        }
        
        return "INSERT INTO " + databaseChange.getTableName() + " (" + names.toString() + ") VALUES (" + values.toString() + ")";
    }
    
    private static String stripQualifier(String s) {
        return s.replaceFirst("^ROW\\.", "").replaceFirst("^OLDROW\\.", "");
    }
}
