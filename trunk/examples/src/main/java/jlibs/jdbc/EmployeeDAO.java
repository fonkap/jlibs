/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.jdbc;

import jlibs.jdbc.annotations.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Santhosh Kumar T
 */
public abstract class EmployeeDAO extends DAO<Employee>{
    public EmployeeDAO(DataSource dataSource, TableMetaData table){
        super(dataSource, table);
    }

    @Select
    public abstract Employee findByID(long id);

    @Select
    public abstract List<Employee> findByAge(int age);

    @Select
    public abstract List<Employee> find(String firstName, String lastName);

    @Insert
    public abstract void insert1(long id, String firstName, String lastName) throws SQLException;

    @Insert
    public abstract Employee insert2(long id, String firstName, String lastName) throws SQLException;

    @Insert
    public abstract void insert(String firstName, int age);
    
    @Update
    public abstract int update(long id, int age, String where_firstName, String where_lastName);

    @Upsert
    public abstract void upsert1(long id, int age, String where_firstName, String where_lastName);

    @Upsert
    public abstract void upsert2(long id, int age, String where_firstName, String where_lastName);

    @Delete
    public abstract int delete(String firstName, String lastName) throws SQLException;

    @Delete
    public abstract int delete(String firstName, int age);

    @Delete("where #{age} between ${fromAge} and ${toAge} or #{lastName}=${lastN}")
    public abstract int delete(int fromAge, int toAge, String lastN);
}