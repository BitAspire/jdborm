package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Builder for TRUNCATE TABLE statements.
 */
public class TruncateQuery implements Query {

    private final JdbORM jdborm;
    private final String table;

    /**
     * Creates a new TRUNCATE TABLE builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the table name to truncate
     */
    public TruncateQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
    }

    @Override
    public String toSql() {
        return "TRUNCATE TABLE " + table;
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }

    /**
     * Executes the TRUNCATE TABLE statement.
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
            throw new JdbOrmException("Failed to execute TRUNCATE: " + sql, e);
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
