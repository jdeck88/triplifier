package rest;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Represents a RDF resource created from a database table.
 * Can generate a D2RQ Mapping entry.
 */
public class Entity {
	public String table;
	public String idColumn;
	public String rdfClass;
	public Set<Attribute> attributes;
	
    /**
     * Generate D2RQ Mapping Language representation of the Entity with Attributes.
     *
     * @param pw PrintWriter used to write output to.
     */
	void printD2RQ(PrintWriter pw) {
		pw.println("map:" + classMap() + " a d2rq:ClassMap;");
		pw.println("\td2rq:dataStorage " + "map:database;");
		pw.println("\td2rq:uriColumn \"" + table + "." + idColumn + "\";");
	//	pw.println("\td2rq:uriPattern \"@@" + mapping.table + "." + mapping.idColumn + "@@\";");
	//	pw.println("\td2rq:uriPattern \"" + mapping.table + "/@@" + mapping.table + "." + mapping.idColumn + "|urlify@@\";");
		pw.println("\td2rq:class " + rdfClass + ";");
	//	pw.println("\td2rq:classDefinitionLabel \"" + mapping.table + "\";");
		pw.println("\t.");
		for (Attribute attribute : attributes)
			attribute.printD2RQ(pw, classMap(), table);
	}
	
	String classMap() {
		return table + "_" + idColumn;
	}
}
