/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.console.service;

import com.google.common.base.Optional;
import io.shardingjdbc.console.domain.SQLResponseResult;
import io.shardingjdbc.console.domain.SQLColumnInformation;
import io.shardingjdbc.console.domain.SQLRowData;
import io.shardingjdbc.console.domain.SQLResultData;
import io.shardingjdbc.console.domain.SessionRegistry;
import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL workbench.
 * 
 * @author zhangyonglun
 */
@Service
public class SQLWorkbench {
    
    /**
     * Handle https for sqls.
     * 
     * @param sql sql
     * @param userUUID user uuid
     * @return SQLResponseResult
     */
    public SQLResponseResult execute(final String sql, final String userUUID) {
        List<SQLColumnInformation> sqlColumnInformationList = new ArrayList<>();
        List<SQLRowData> sqlRowDataList = new ArrayList<>();
        SQLResultData sqlResultData = new SQLResultData(0, 0L, sql, sqlColumnInformationList, sqlRowDataList);
        SQLResponseResult result = new SQLResponseResult(-1, "", sqlResultData);
        Optional<Connection> connectionOptional = SessionRegistry.getInstance().findSession(userUUID);
    
        if (!connectionOptional.isPresent()) {
            result.setMessage("please login first.");
            return result;
        }
        Connection connection = connectionOptional.get();
        
        long startTime = System.currentTimeMillis();
        try (
                Statement statement = connection.createStatement()
        ) {
            if (statement.execute(sql)) {
                ResultSet resultSet = statement.getResultSet();
                return setsFormatResult(result, sqlResultData, resultSet, startTime);
            } else {
                return countsFormatResult(result, sqlResultData, statement, startTime);
            }
        } catch (final SQLException ex) {
            result.setMessage(ex.getMessage());
            return result;
        }
    }
    
    private SQLResponseResult countsFormatResult(final SQLResponseResult result, final SQLResultData sqlResultData, final Statement statement,
                                                 final long startTime) throws SQLException {
        sqlResultData.setAffectedRows(statement.getUpdateCount());
        result.setStatus(200);
        sqlResultData.setDurationMilliseconds(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private SQLResponseResult setsFormatResult(final SQLResponseResult result, final SQLResultData sqlResultData, final ResultSet resultSet,
                                               final long startTime) throws SQLException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        List<SQLColumnInformation> sqlColumnInformationList = sqlResultData.getSqlColumnInformationList();
    
        getColumnInfo(resultSetMetaData, columnCount, sqlColumnInformationList);
        getRowData(resultSetMetaData, result, sqlResultData, resultSet, startTime, columnCount);
        return result;
    }
    
    private void getColumnInfo(final ResultSetMetaData resultSetMetaData, final int columnCount, final List<SQLColumnInformation> sqlColumnInformationList) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            sqlColumnInformationList.add(new SQLColumnInformation(resultSetMetaData.getColumnName(i), resultSetMetaData.getColumnTypeName(i), resultSetMetaData.getColumnDisplaySize(i)));
        }
    }
    
    private void getRowData(final ResultSetMetaData resultSetMetaData, final SQLResponseResult sqlResponseResult, final SQLResultData sqlResultData,
                            final ResultSet resultSet, final long startTime, final int columnCount) throws SQLException {
        List<SQLRowData> sqlRowDataList = sqlResultData.getSqlRowDataList();
        Integer rowCount = 0;
        
        while (resultSet.next()) {
            rowCount++;
            SQLRowData sqlRowData = new SQLRowData();
            for (int i = 1; i <= columnCount; i++) {
                sqlRowData.getRowData().put(resultSetMetaData.getColumnName(i), resultSet.getString(i));
            }
            sqlRowDataList.add(sqlRowData);
        }
        sqlResultData.setAffectedRows(rowCount);
        sqlResponseResult.setStatus(200);
        sqlResultData.setDurationMilliseconds(System.currentTimeMillis() - startTime);
    }
}
