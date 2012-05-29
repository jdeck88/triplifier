package rest;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Performs two major tasks: 
 * - constructor inspects SQL database and generates schema,
 * - printD2RQ translates connection, entities and relations
 *   into D2RQ Mapping Language.
 */
public class Mapping { 
	public String dateTime;
	public Connection connection;
	public Set<DBtable> schema;
	public Set<Join> joins;
	public Set<Entity> entities;
	public Set<Relation> relations;

	/**
	 * For construction from JSON.
	 */
	Mapping() {}

    /**
     * Create Mapping with dateTime, connection and schema only
     * (joins, entities and relations are empty). Schema is based
     * on inspection of given database connection.
     *
     * @param connection SQL database connection parameters.
     */
	Mapping(Connection connection) {
		dateTime = DateFormat.getDateTimeInstance().format(new Date());
		this.connection = connection;
		schema = new TreeSet<DBtable>();
		joins = new HashSet<Join>();
		entities = new HashSet<Entity>();
		relations = new HashSet<Relation>();

		Database database = connection.getD2RQdatabase();
		DatabaseSchemaInspector schemaInspector = database.connectedDB().schemaInspector();
//		System.out.println("tables: " + schemaInspector.listTableNames(null));
		DBtable table;
		for (RelationName relationName : schemaInspector.listTableNames(null)) {
			table = new DBtable(relationName.tableName(), new TreeSet<String>(), new HashSet<String>());
			schema.add(table);
			for (Attribute attribute : schemaInspector.listColumns(relationName)) 
				table.columns.add(attribute.attributeName());
			for (Attribute attribute : schemaInspector.primaryKeyColumns(relationName)) 
				table.pkColumns.add(attribute.attributeName());
		}
		database.connectedDB().close();
	}

    /**
     * Generate D2RQ Mapping Language representation of connection, entities and relations.
     *
     * @param pw PrintWriter used to write output to.
     */
	void printD2RQ(PrintWriter pw) throws SQLException {
		printPrefixes(pw);
		connection.printD2RQ(pw);
		for (Entity entity : entities)
			entity.printD2RQ(pw);
		for (Relation relation : relations)
			relation.printD2RQ(pw, this);
	}

    /**
     * Find Join between table1 and table2.
     *
     * @param table1 Table name.
     * @param table2 Table name.
     * @return Matching Join or null if not found.
     */
	Join findJoin(String table1, String table2) {
		for (Join join : joins) 
			if (table1.equals(join.foreignTable) && table2.equals(join.primaryTable) || 
					table1.equals(join.primaryTable) && table2.equals(join.foreignTable)) 
				return join;
		return null;
	}

    /**
     * Find Entity defined by given table and idColumn.
     *
     * @param table Table name.
     * @param idColumn IdColumn name.
     * @return Matching Entity or null if not found.
     */
	Entity findEntity(String table, String idColumn) {
		for (Entity entity : entities) 
			if (table.equals(entity.table) && idColumn.equals(entity.idColumn)) 
				return entity;
		return null;
	}

    /**
     * Generate all possible RDF prefixes.
     *
     * @param pw PrintWriter used to write output to.
     */
	private void printPrefixes(PrintWriter pw) {
		pw.println("@prefix map: <" + "" + "> .");
		//	out.println("@prefix db: <" + "" + "> .");
		//	out.println("@prefix vocab: <" + "vocab" + "> .");
		pw.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		pw.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
		pw.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		pw.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
		pw.println("@prefix jdbc: <http://d2rq.org/terms/jdbc/> .");
		pw.println("@prefix dwc: <http://rs.tdwg.org/dwc/terms/index.htm#> .");
		pw.println("@prefix bsc: <http://biscicol.org/biscicol.rdf#> .");
		pw.println("@prefix dcterms: <http://purl.org/dc/terms/> .");
		pw.println("@prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> .");
		pw.println("@prefix ma: <http://www.w3.org/ns/ma-ont#> .");
		pw.println();
	}
}
