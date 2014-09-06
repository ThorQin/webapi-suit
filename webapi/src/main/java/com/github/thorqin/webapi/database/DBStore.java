package com.github.thorqin.webapi.database;

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.monitor.MonitorService;
import com.github.thorqin.webapi.monitor.StatementInfo;
import com.jolbox.bonecp.BoneCPDataSource;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**********************************************************
 * DBStore implementation
 * @author nuo.qin
 *
 **********************************************************/
public class DBStore {
	private static final Logger logger = Logger.getLogger(DBStore.class.getName());
	private static final Map<Class<?>, Integer> typeMapping;
	
	static {
		typeMapping = new HashMap<>();
		typeMapping.put(List.class, java.sql.Types.ARRAY);
		typeMapping.put(byte[].class, java.sql.Types.VARBINARY);
		typeMapping.put(Void.class, java.sql.Types.JAVA_OBJECT);
		typeMapping.put(void.class, java.sql.Types.JAVA_OBJECT);
		typeMapping.put(Date.class, java.sql.Types.TIMESTAMP);
		typeMapping.put(Calendar.class, java.sql.Types.TIMESTAMP);
		typeMapping.put(Byte.class, java.sql.Types.TINYINT);
		typeMapping.put(byte.class, java.sql.Types.TINYINT);
		typeMapping.put(Short.class, java.sql.Types.SMALLINT);
		typeMapping.put(short.class, java.sql.Types.SMALLINT);
		typeMapping.put(Integer.class, java.sql.Types.INTEGER);
		typeMapping.put(int.class, java.sql.Types.INTEGER);
		typeMapping.put(Long.class, java.sql.Types.BIGINT);
		typeMapping.put(long.class, java.sql.Types.BIGINT);
		typeMapping.put(Float.class, java.sql.Types.FLOAT);
		typeMapping.put(float.class, java.sql.Types.FLOAT);
		typeMapping.put(Double.class, java.sql.Types.DOUBLE);
		typeMapping.put(double.class, java.sql.Types.DOUBLE);
		typeMapping.put(Boolean.class, java.sql.Types.BIT);
		typeMapping.put(boolean.class, java.sql.Types.BIT);
		typeMapping.put(String.class, java.sql.Types.VARCHAR);
		typeMapping.put(DBCursor.class, java.sql.Types.OTHER);
		typeMapping.put(DBTable.class, java.sql.Types.OTHER);
		typeMapping.put(BigDecimal.class, java.sql.Types.NUMERIC);
	}
	
	private static int toSqlType(Class<?> type) {
		Integer sqlType = typeMapping.get(type);
		if (sqlType == null) {
			if (type.isArray())
				return java.sql.Types.ARRAY; 
			else if (type.isAnnotationPresent(UDT.class))
				return java.sql.Types.STRUCT;
			else
				return java.sql.Types.OTHER;
		} else
			return sqlType;
	}
	
	private static java.sql.Timestamp toSqlDate(java.util.Date date) {
		return new java.sql.Timestamp(date.getTime());
	}
	private static java.sql.Timestamp toSqlDate(java.util.Calendar calendar) {
		return new java.sql.Timestamp(calendar.getTimeInMillis());
	}
	
	private static java.util.Date fromSqlDate(java.sql.Time time) {
		return new java.util.Date(time.getTime());
	}
	private static java.util.Date fromSqlDate(java.sql.Date time) {
		return new java.util.Date(time.getTime());
	}
	private static java.util.Date fromSqlDate(java.sql.Timestamp time) {
		return new java.util.Date(time.getTime());
	}

	private static Object fromSqlObject(Object obj, Class<?> destType, Map<String, Class<?>> udtMapping) throws SQLException {
		if (obj == null)
			return null;
		Class<?> type = obj.getClass();
		if (destType.equals(java.util.Date.class) && type.equals(java.sql.Timestamp.class)) {
			return fromSqlDate((java.sql.Timestamp)obj);
		} else if (destType.equals(java.util.Date.class) && type.equals(java.sql.Date.class)) {
			return fromSqlDate((java.sql.Date)obj);
		} else if (destType.equals(java.util.Date.class) && type.equals(java.sql.Time.class)) {
			return fromSqlDate((java.sql.Time)obj);
		} else if (destType.equals(java.util.List.class) && type.equals(java.sql.Array.class)) {
			Array array = (Array)obj;
			List<Object> list = new LinkedList<>();
			try (ResultSet arrayResult = array.getResultSet()) {
				while (arrayResult.next()) {
					list.add(fromSqlObject(arrayResult.getObject(2), udtMapping));
				}
				return list;
			}
		} else if (destType.equals(DBCursor.class) && obj instanceof java.sql.ResultSet) {
			return new DBCursor((ResultSet)obj);
		} else if (destType.equals(Object[].class) && obj instanceof java.sql.Array) {
			Array array = (Array)obj;
			List<Object> list = new LinkedList<>();
			try (ResultSet arrayResult = array.getResultSet()){
				while (arrayResult.next()) {
					list.add(fromSqlObject(arrayResult.getObject(2), udtMapping));
				}
				return list.toArray();
			} 
		} else if (type.equals(java.sql.Struct.class)) {
			UDT udt = destType.getAnnotation(UDT.class);
			if (udt == null)
				throw new SQLException("Cannot map UDT object");
			Struct struct = (Struct)obj;
			if (!udt.udtName().equals(struct.getSQLTypeName())) {
				throw new SQLException("Cannot convert UDT object from " 
						+ struct.getSQLTypeName() + " to " + udt.udtName());
			}
			Object[] attributes = struct.getAttributes();
			Object instance = null;
			try {
				instance = destType.newInstance();
			} catch (InstantiationException
					| IllegalAccessException e) {
				throw new SQLException("Parse java.sql.Struct failed.", e);
			}
			Field[] fields = destType.getFields();
			for (int i = 0, j = 0; i < fields.length && j < attributes.length; i++) {
				if (!fields[i].isAnnotationPresent(UDTSkip.class)) {
					try {
						fields[i].set(instance, fromSqlObject(attributes[j], fields[i].getType(), udtMapping));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new SQLException("Parse java.sql.Struct failed.", e);
					}
					j++;
				}
			}
			return instance;
		} else {
			return obj;
		}
	}

	private static Object fromSqlObject(Object obj, Map<String, Class<?>> udtMapping) throws SQLException {
		if (obj == null)
			return null;
		Class<?> type = obj.getClass();
		if (type.equals(java.sql.Timestamp.class)) {
			return fromSqlDate((java.sql.Timestamp)obj);
		} else if (type.equals(java.sql.Date.class)) {
			return fromSqlDate((java.sql.Date)obj);
		} else if (type.equals(java.sql.Time.class)) {
			return fromSqlDate((java.sql.Time)obj);
		} else if (obj instanceof java.sql.ResultSet) {
			return new DBCursor((ResultSet)obj);
		} else if (obj instanceof java.sql.Array) {
			Array array = (Array)obj;
			List<Object> list = new LinkedList<>();
			try (ResultSet arrayResult = array.getResultSet()) {
				while (arrayResult.next()) {
					list.add(fromSqlObject(arrayResult.getObject(2), udtMapping));
				}
				return list;
			} 
		} else if (obj instanceof java.sql.Struct) {
			Struct struct = (Struct)obj;
			Class<?> clazz = udtMapping.get(struct.getSQLTypeName());
			if (clazz != null) {
				Object[] attributes = struct.getAttributes();
				Object instance = null;
				try {
					instance = clazz.newInstance();
				} catch (InstantiationException
						| IllegalAccessException e) {
					throw new SQLException("Parse java.sql.Struct failed.", e);
				}
				Field[] fields = clazz.getFields();
				for (int i = 0, j = 0; i < fields.length && j < attributes.length; i++) {
					if (!fields[i].isAnnotationPresent(UDTSkip.class)) {
						try {
							fields[i].set(instance, fromSqlObject(attributes[j], fields[i].getType(), udtMapping));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new SQLException("Parse java.sql.Struct failed.", e);
						}
						j++;
					}
				}
				return instance;
			} else {
				List<Object> list = new LinkedList<>();
				for (Object attribute : struct.getAttributes()) {
					list.add(fromSqlObject(attribute, udtMapping));
				}
				return list;
			}
		} else {
			return obj;
		}
	}
			
	
	
	@Override
	protected void finalize() throws Throwable {
		close(false);
		super.finalize();
	}
	
	public static class DBTable {
		public String[] head;
		public List<Object[]> data;
		public Integer length;
		private Map<String, Integer> headMapping = null;
		private void buildHeadMapping() {
			if (headMapping == null) {
				headMapping = new HashMap<>();
				for(int i = 0; i < head.length; i++) {
					String colName = head[i];
					headMapping.put(colName, i);
				}
			}
		}
		public Object getValue(Object[] row, String column) {
			buildHeadMapping();
			Integer pos = headMapping.get(column);
			if (pos != null) {
				return row[pos];
			} else
				throw new InvalidParameterException("Column '" + column + "' doesn't exist!");
		}
		public void setValue(Object[] row, String column, Object value) {
			buildHeadMapping();
			Integer pos = headMapping.get(column);
			if (pos != null) {
				row[pos] = value;
			} else
				throw new InvalidParameterException("Column '" + column + "' doesn't exist!");
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface UDT {
		String udtName() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface UDTSkip {
	}

	public static class DBOut<T> {
		protected T value;
		protected Class<T> type;

		public DBOut(Class<T> type) {
			this.type = type;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}
		public Class<T> getType() {
			return type;
		}
	}

	public static class DBRef<T> extends DBOut<T> {
		public DBRef(T value, Class<T> type) {
			super(type);
			this.value = value;
		}
	}
	
	public static class DBOutString extends DBOut<String> {
		public DBOutString() {
			super(String.class);
		}
	}
	public static class DBOutInteger extends DBOut<Integer> {
		public DBOutInteger() {
			super(Integer.class);
		}
	}
	public static class DBOutShort extends DBOut<Short> {
		public DBOutShort() {
			super(Short.class);
		}
	}
	public static class DBOutLong extends DBOut<Long> {
		public DBOutLong() {
			super(Long.class);
		}
	}
	public static class DBOutByte extends DBOut<Byte> {
		public DBOutByte() {
			super(Byte.class);
		}
	}
	public static class DBOutFloat extends DBOut<Float> {
		public DBOutFloat() {
			super(Float.class);
		}
	}
	public static class DBOutDouble extends DBOut<Double> {
		public DBOutDouble() {
			super(Double.class);
		}
	}
	public static class DBOutDate extends DBOut<Date> {
		public DBOutDate() {
			super(Date.class);
		}
	}
	public static class DBOutBoolean extends DBOut<Boolean> {
		public DBOutBoolean() {
			super(Boolean.class);
		}
	}
	public static class DBOutTable extends DBOut<DBTable> {
		public DBOutTable() {
			super(DBTable.class);
		}
	}
	public static class DBOutCursor extends DBOut<DBCursor> {
		public DBOutCursor() {
			super(DBCursor.class);
		}
	}
	public static class DBOutArray extends DBOut<Object[]> {
		public DBOutArray() {
			super(Object[].class);
		}
	}
	public static class DBOutBinary extends DBOut<byte[]> {
		public DBOutBinary() {
			super(byte[].class);
		}
	}
	
	public static class DBRefString extends DBRef<String> {
		public DBRefString(String value) {
			super(value, String.class);
		}
		public DBRefString() {
			super(null, String.class);
		}
	}
	public static class DBRefInteger extends DBRef<Integer> {
		public DBRefInteger(Integer value) {
			super(value, Integer.class);
		}
		public DBRefInteger() {
			super(null, Integer.class);
		}
	}
	public static class DBRefShort extends DBRef<Short> {
		public DBRefShort(Short value) {
			super(value, Short.class);
		}
		public DBRefShort() {
			super(null, Short.class);
		}
	}
	public static class DBRefLong extends DBRef<Long> {
		public DBRefLong(Long value) {
			super(value, Long.class);
		}
		public DBRefLong() {
			super(null, Long.class);
		}
	}
	public static class DBRefByte extends DBRef<Byte> {
		public DBRefByte(Byte value) {
			super(value, Byte.class);
		}
		public DBRefByte() {
			super(null, Byte.class);
		}
	}
	public static class DBRefFloat extends DBRef<Float> {
		public DBRefFloat(Float value) {
			super(value, Float.class);
		}
		public DBRefFloat() {
			super(null, Float.class);
		}
	}
	public static class DBRefDouble extends DBRef<Double> {
		public DBRefDouble(Double value) {
			super(value, Double.class);
		}
		public DBRefDouble() {
			super(null, Double.class);
		}
	}
	public static class DBRefDate extends DBRef<Date> {
		public DBRefDate(Date value) {
			super(value, Date.class);
		}
		public DBRefDate() {
			super(null, Date.class);
		}
	}
	public static class DBRefBoolean extends DBRef<Boolean> {
		public DBRefBoolean(Boolean value) {
			super(value, Boolean.class);
		}
		public DBRefBoolean() {
			super(null, Boolean.class);
		}
	}
	public static class DBRefTable extends DBRef<DBTable> {
		public DBRefTable(DBTable value) {
			super(value, DBTable.class);
		}
		public DBRefTable() {
			super(null, DBTable.class);
		}
	}
	public static class DBRefCursor extends DBRef<DBCursor> {
		public DBRefCursor(DBCursor value) {
			super(value, DBCursor.class);
		}
		public DBRefCursor() {
			super(null, DBCursor.class);
		}
	}
	public static class DBRefArray extends DBRef<Object[]> {
		public DBRefArray(Object[] value) {
			super(value, Object[].class);
		}
		public DBRefArray() {
			super(null, Object[].class);
		}
	}
	public static class DBRefBinary extends DBRef<byte[]> {
		public DBRefBinary(byte[] value) {
			super(value, byte[].class);
		}
		public DBRefBinary() {
			super(null, byte[].class);
		}
	}

	public static interface DBResultHanlder {
		void handle(ResultSet result) throws Exception;
	}
	
	public static class DBCursor implements AutoCloseable {
		private ResultSet resultSet;
		
		public DBCursor() {
			resultSet = null;
		}
		public DBCursor(ResultSet resultSet) {
			this.resultSet = resultSet;
		}
		public ResultSet getResultSet() {
			return resultSet;
		}
		public void setResultSet(ResultSet resultSet) {
			this.resultSet = resultSet;
		}
		public void perform(DBResultHanlder handler) throws Exception {
			if (resultSet != null)
				handler.handle(resultSet);
		}
		public DBTable getTable() throws SQLException {
			return getTable(null);
		}
		public DBTable getTable(Map<String, Class<?>> udtMapping) throws SQLException {
			if (resultSet == null)
				return null;
			ResultSetMetaData mataData = resultSet.getMetaData();
			int columns = resultSet.getMetaData().getColumnCount();
			DBTable table = new DBTable();
			String[] head = new String[columns];
			for (int i = 1; i <= columns; i++) {
				head[i - 1] = mataData.getColumnLabel(i);
			}
			table.head = head;
			LinkedList<Object[]> list = new LinkedList<>();
			while (resultSet.next()) {
				Object[] line = new Object[columns];
				for (int i = 1; i <= columns; i++) {
					line[i - 1] = fromSqlObject(resultSet.getObject(i), udtMapping);
				}
				list.add(line);
			}
			table.data = list;
			table.length = list.size();
			return table;
		}

		@Override
		public void close() {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException ex) {
				}
			}
		}
	}
	

	public static interface DBWork {
		public void doWork(DBSession session) throws Exception;
	}

	public static interface DBWorkWithInput {
		public void doWork(DBSession session, Object[] params) throws Exception;
	}

	public static interface DBWorkWithOutput {
		public Object doWork(DBSession session) throws Exception;
	}

	public static interface DBWorkWithInputAndOutput {
		public Object doWork(DBSession session, Object[] params) throws Exception;
	}
	
	final private String profileName;
	final private BoneCPDataSource boneCP = new BoneCPDataSource();
	final private String dbDriver;
	final private String dbURI;
	final private boolean enableTrace;
	
	public DBStore(WebApplication application, String profileName) throws SQLException, IOException, RuntimeException, URISyntaxException {
		this.profileName = profileName;
		DBConfig dbConfig = new DBConfig(application, profileName);
		dbDriver = dbConfig.getDBDriver();
		this.enableTrace = dbConfig.enableTrace();
		if (dbDriver != null) {
			boneCP.setDriverClass(dbDriver);
		}
		dbURI = dbConfig.getDBUri();
		if (dbURI != null)
			boneCP.setJdbcUrl(dbURI);
		if (dbConfig.getDBUser() != null)
			boneCP.setUsername(dbConfig.getDBUser());
		if (dbConfig.getDBPassword() != null)
			boneCP.setPassword(dbConfig.getDBPassword());
		boneCP.setMinConnectionsPerPartition(dbConfig.getMinConnectionsPerPartition());
		boneCP.setMaxConnectionsPerPartition(dbConfig.getMaxConnectionsPerPartition());
		boneCP.setPartitionCount(dbConfig.getPartitionCount());
	}
	
	public void close() {
		close(false);
	}
	public void close(boolean unregisterDriver) {
		try {
			boneCP.close();
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Shutdown boneCP failed!", ex);
		}
		if (unregisterDriver && dbURI != null && dbDriver != null) {
			try {
				DriverManager.deregisterDriver(DriverManager.getDriver(dbURI));
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "Unregister DB driver failed!", ex);
			}
		}
	}
	
	public String getProfileName() {
		return profileName;
	}
	
	public static class DBSession implements AutoCloseable {
		private final Connection conn;
		private final boolean enableTrace;
		public DBSession(Connection conn, boolean enableTrace) {
			this.conn = conn;
			this.enableTrace = enableTrace;
		}
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			conn.setAutoCommit(autoCommit);
		}
		public boolean getAutoCommit() throws SQLException {
			return conn.getAutoCommit();
		}
		public void commit() throws SQLException {
			conn.commit();				
		}
		public void rollback() throws SQLException {
			conn.rollback();
		}
		@Override
		public void close()	{
			try {
				conn.close();
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, "Close connection failed!", ex);
			}
		}

		private java.sql.Struct toUdt(Object obj) throws SQLException {
			if (obj == null)
				return null;
			Class<?> clazz = obj.getClass();
			UDT udt = clazz.getAnnotation(UDT.class);
			if (udt == null) {
				return null;
			}
			ArrayList<Object> attributes = new ArrayList<>(clazz.getFields().length);
			for (Field field : clazz.getFields()) {
				if (!field.isAnnotationPresent(UDTSkip.class)) {
					try {
						attributes.add(toSqlObj(field.get(obj)));
					} catch (IllegalArgumentException
							| IllegalAccessException e) {
						throw new SQLException("Convert object to java.sql.Struct failed.", e);
					}
				}
			}
			return conn.createStruct(udt.udtName(), attributes.toArray());
		}
		
		private Object toSqlObj(Object obj) throws SQLException {
			java.sql.Struct udt;
			if (obj == null) {
				return null;
			} else if (obj.getClass().equals(Date.class)) {
				return toSqlDate((Date)obj);
			} else if (obj.getClass().equals(Calendar.class)) {
				return toSqlDate((Calendar)obj);
			} else if (obj.getClass().equals(DBCursor.class)) {
				return ((DBCursor)obj).getResultSet();
			} else if ((udt = toUdt(obj)) != null) {
				return udt;
			} else {
				return obj;
			}
		}

		@SuppressWarnings("rawtypes")
		private void bindParameter(PreparedStatement stmt, Object[] args, int offset) throws SQLException {
			if (args == null)
				return;
			java.sql.Struct udt;
			for (Object obj : args) {
				if (obj == null) {
					stmt.setNull(offset++, java.sql.Types.NULL);
				} else {
					Class<?> paramType = obj.getClass();
					if (DBRef.class.isAssignableFrom(paramType)) {
						obj = ((DBRef)obj).getValue();
						if (obj == null)
							stmt.setNull(offset++, java.sql.Types.NULL);
						else if (obj.getClass().equals(Date.class)) {
							stmt.setTimestamp(offset++, toSqlDate((Date)obj));
						} else if (obj.getClass().equals(Calendar.class)) {
							stmt.setTimestamp(offset++, toSqlDate((Calendar)obj));
						} else if (obj.getClass().equals(DBCursor.class)) {
							stmt.setObject(offset++, ((DBCursor)obj).getResultSet());
						} else if ((udt = toUdt(obj)) != null) {
							stmt.setObject(offset++, udt, java.sql.Types.STRUCT);
						} else {
							stmt.setObject(offset++, obj);
						}
					} else if (DBOut.class.isAssignableFrom(paramType)) {
						stmt.setNull(offset++, java.sql.Types.NULL);
					} else if (paramType.equals(Date.class)) {
						stmt.setTimestamp(offset++, toSqlDate((Date)obj));
					} else if (paramType.equals(Calendar.class)) {
						stmt.setTimestamp(offset++, toSqlDate((Calendar)obj));
					} else if ((udt = toUdt(obj)) != null) {
						stmt.setObject(offset++, udt, java.sql.Types.STRUCT);
					} else if (paramType.equals(DBCursor.class)) {
						stmt.setObject(offset++, ((DBCursor)obj).getResultSet());
					} else {
						stmt.setObject(offset++, obj);
					}
				}
			}
		}
		
		@SuppressWarnings("rawtypes")
		private void bindParameter(CallableStatement stmt, Object[] args, int offset) throws SQLException {
			if (args == null)
				return;
			java.sql.Struct udt;
			for (Object obj : args) {
				if (obj == null) {
					stmt.setNull(offset++, java.sql.Types.NULL);
				} else {
					Class<?> paramType = obj.getClass();
					if (DBRef.class.isAssignableFrom(paramType)) {
						stmt.registerOutParameter(offset, toSqlType(((DBOut)obj).getType()));
						obj = ((DBOut)obj).getValue();
						if (obj == null)
							stmt.setNull(offset++, java.sql.Types.NULL);
						else if (obj.getClass().equals(Date.class)) {
							stmt.setTimestamp(offset++, toSqlDate((Date)obj));
						} else if (obj.getClass().equals(Calendar.class)) {
							stmt.setTimestamp(offset++, toSqlDate((Calendar)obj));
						} else if (obj.getClass().equals(DBCursor.class)) {
							stmt.setObject(offset++, ((DBCursor)obj).getResultSet());
						} else if ((udt = toUdt(obj)) != null) {
							stmt.setObject(offset++, udt, java.sql.Types.STRUCT);
						} else {
							stmt.setObject(offset++, obj);
						}
					} else if (DBOut.class.isAssignableFrom(paramType)) {
						stmt.registerOutParameter(offset++, toSqlType(((DBOut)obj).getType()));
					} else if (paramType.equals(Date.class)) {
						stmt.setTimestamp(offset++, toSqlDate((Date)obj));
					} else if (paramType.equals(Calendar.class)) {
						stmt.setTimestamp(offset++, toSqlDate((Calendar)obj));
					} else if (paramType.equals(DBCursor.class)) {
						stmt.setObject(offset++, ((DBCursor)obj).getResultSet());
					} else if ((udt = toUdt(obj)) != null) {
						stmt.setObject(offset++, udt, java.sql.Types.STRUCT);
					} else {
						stmt.setObject(offset++, obj);
					}
				}
			}
		}

		public int execute(String queryString) throws SQLException {
			return execute(queryString, null);
		}
		public int execute(String queryString, Object[] args) throws SQLException {
			long beginTime = System.currentTimeMillis();
			boolean success = true;
			try (PreparedStatement stmt = conn.prepareStatement(queryString)){
				bindParameter(stmt, args, 1);
				return stmt.executeUpdate();
			} catch (Exception ex) {
				success = false;
				throw ex;
			} finally {
				if (enableTrace) {
					StatementInfo sqlInfo = new StatementInfo();
					sqlInfo.execType = "execute";
					sqlInfo.statement = queryString;
					sqlInfo.success = success;
					sqlInfo.startTime = beginTime;
					sqlInfo.runningTime = System.currentTimeMillis() - beginTime;
					MonitorService.record(sqlInfo);
				}
			}
		}
		public DBCursor query(String queryString) throws SQLException {
			return query(queryString, (Map<String, Class<?>>)null, null);
		}
		
		public DBCursor query(String queryString,
				Object[] args) throws SQLException {
			return query(queryString, (Map<String, Class<?>>)null, args);
		}
		
		public DBCursor query(String queryString,
				Map<String, Class<?>> udtMapping,
				Object[] args) throws SQLException {
			long beginTime = System.currentTimeMillis();
			boolean success = true;
			try (PreparedStatement stmt = conn.prepareStatement(queryString)) {
				bindParameter(stmt, args, 1);
				ResultSet rs = stmt.executeQuery();
				return new DBCursor(rs);
			} catch (Exception ex) {
				success = false;
				throw ex;
			} finally {
				if (enableTrace) {
					StatementInfo sqlInfo = new StatementInfo();
					sqlInfo.execType = "query";
					sqlInfo.statement = queryString;
					sqlInfo.success = success;
					sqlInfo.startTime = beginTime;
					sqlInfo.runningTime = System.currentTimeMillis() - beginTime;
					MonitorService.record(sqlInfo);
				}
			}
		}
		public void query(String queryString, DBResultHanlder handler)
				throws Exception {
			query(queryString, handler, null);
		}

		public void query(String queryString, DBResultHanlder handler, Object[] args) throws Exception {
			long beginTime = System.currentTimeMillis();
			boolean success = true;			
			try (PreparedStatement stmt = conn.prepareStatement(queryString)) {
				bindParameter(stmt, args, 1);
				try (ResultSet rs = stmt.executeQuery()) {
					if (handler != null)
						handler.handle(rs);
				}
			} catch (Exception ex) {
				success = false;
				throw ex;
			} finally {
				if (enableTrace) {
					StatementInfo sqlInfo = new StatementInfo();
					sqlInfo.execType = "query";
					sqlInfo.statement = queryString;
					sqlInfo.success = success;
					sqlInfo.startTime = beginTime;
					sqlInfo.runningTime = System.currentTimeMillis() - beginTime;
					MonitorService.record(sqlInfo);
				}
			}
		}
		public <T> T invoke(String procName, Class<T> returnType) throws SQLException {
			return invoke(procName, returnType, null, null);
		}
		public <T> T invoke(String procName, Class<T> returnType, Object[] args)
				throws SQLException {
			return invoke(procName, returnType, null, args);
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T> T invoke(String procName, Class<T> returnType, Map<String, Class<?>> udtMapping,
				Object[] args) throws SQLException {
			long beginTime = System.currentTimeMillis();
			boolean success = true;		
			StringBuilder sqlString = new StringBuilder();
			sqlString.append("{?=call ").append(procName).append("(");
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					if (i == 0)
						sqlString.append("?");
					else
						sqlString.append(",?");
				}
			}
			sqlString.append(")}");
			try (CallableStatement stmt = conn.prepareCall(sqlString.toString())){
				bindParameter(stmt, args, 2);
				stmt.registerOutParameter(1, toSqlType(returnType));
				stmt.execute();
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						if (args[i] instanceof DBOut) {
							DBOut param = (DBOut)args[i];
							if (param.getType().equals(DBTable.class)) {
								DBCursor cursor = (DBCursor)fromSqlObject(stmt.getObject(i + 2), udtMapping);
								param.setValue(cursor.getTable());
							} else {
								param.setValue(fromSqlObject(stmt.getObject(i + 2), udtMapping));
							}
						}
					}
				}
				if (returnType.equals(DBTable.class)) {
					DBCursor cursor = (DBCursor)fromSqlObject(stmt.getObject(1), udtMapping);
					return (T)cursor.getTable();
				} else
					return (T)fromSqlObject(stmt.getObject(1), udtMapping);
			} catch (Exception ex) {
				success = false;
				throw ex;
			} finally {
				if (enableTrace) {
					StatementInfo sqlInfo = new StatementInfo();
					sqlInfo.execType = "invoke";
					sqlInfo.statement = sqlString.toString();
					sqlInfo.success = success;
					sqlInfo.startTime = beginTime;
					sqlInfo.runningTime = System.currentTimeMillis() - beginTime;
					MonitorService.record(sqlInfo);
				}
			}
		}
		public void perform(String procName) throws SQLException {
			perform(procName, null, null);
		}
		public void perform(String procName, Object[] args) throws SQLException {
			perform(procName, null, args);
		}
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void perform(String procName, Map<String, Class<?>> udtMapping, Object[] args) 
				throws SQLException {
			long beginTime = System.currentTimeMillis();
			boolean success = true;			
			StringBuilder sqlString = new StringBuilder();
			sqlString.append("{call ").append(procName).append("(");
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					if (i == 0)
						sqlString.append("?");
					else
						sqlString.append(",?");
				}
			}
			sqlString.append(")}");
			try (CallableStatement stmt = conn.prepareCall(sqlString.toString())){
				bindParameter(stmt, args, 1);
				stmt.execute();
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						if (args[i] instanceof DBOut) {
							DBOut param = (DBOut)args[i];
							if (param.getType().equals(DBTable.class)) {
								DBCursor cursor = (DBCursor)fromSqlObject(stmt.getObject(i + 1), udtMapping);
								param.setValue(cursor.getTable());
							} else {
								param.setValue(fromSqlObject(stmt.getObject(i + 1), udtMapping));
							}
						}
					}
				}
			} catch (Exception ex) {
				success = false;
				throw ex;
			} finally {
				if (enableTrace) {
					StatementInfo sqlInfo = new StatementInfo();
					sqlInfo.execType = "perform";
					sqlInfo.statement = sqlString.toString();
					sqlInfo.success = success;
					sqlInfo.startTime = beginTime;
					sqlInfo.runningTime = System.currentTimeMillis() - beginTime;
					MonitorService.record(sqlInfo);
				}
			}
		}
	}

	public void doWork(DBWork work) throws Exception {
		if (work != null) {
			try (DBStore.DBSession session = getSession()) {
				work.doWork(session);
			}
		}
	}
	public void doWork(DBWorkWithInput work, Object... params) throws Exception {
		if (work != null) {
			try (DBStore.DBSession session = getSession()) {
				work.doWork(session, params);
			}
		}
	}
	public Object doWork(DBWorkWithOutput work) throws Exception {
		if (work != null) {
			try (DBStore.DBSession session = getSession()) {
				return work.doWork(session);
			}
		} else
			return null;
	}
	public Object doWork(DBWorkWithInputAndOutput work, Object... params)
			throws Exception {
		if (work != null) {
			try (DBStore.DBSession session = getSession()) {
				return work.doWork(session, params);
			}
		} else
			return null;
	}
	public DBSession getSession() throws SQLException {
		return new DBSession(getConnection(), enableTrace);
	}
	public Connection getConnection() throws SQLException {
		return boneCP.getConnection();
	}
}