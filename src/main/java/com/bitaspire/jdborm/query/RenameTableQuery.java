package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Builder for RENAME TABLE statements.
 */
public class RenameTableQuery implements Query {

    private final JdbORM jdborm;
    private final String oldName;
    private final String newName;

    /**
     * Creates a new RENAME TABLE builder.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param oldName the current table name
     * @param newName the new table name
     */
    public RenameTableQuery(JdbORM jdborm, String oldName, String newName) {
        this.jdborm = jdborm;
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public String toSql() {
        return "RENAME TABLE " + oldName + " TO " + newName;
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }

    /**
     * Executes the RENAME TABLE statement.
     *
     * @throws JdbOrmException if SQL execution fails
     */
    public void execute() {
        String sql = toSql();
        Connection conn = jdborm.getConnection();
        try {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute RENAME TABLE: " + sql, e);
        } finally {
            if (jdborm.isUseDataSource() && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
