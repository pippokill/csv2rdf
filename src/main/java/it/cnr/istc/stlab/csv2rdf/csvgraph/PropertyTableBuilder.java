/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.cnr.istc.stlab.csv2rdf.csvgraph;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.jena.atlas.csv.CSVParser;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.propertytable.PropertyTable;
import org.apache.jena.propertytable.Row;
import org.apache.jena.propertytable.impl.PropertyTableArrayImpl;
import org.apache.jena.propertytable.lang.LangCSV;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.system.IRIResolver;
import org.apache.jena.vocabulary.XSD;

/**
 * A tool for constructing PropertyTable from a file (e.g., a CSV file).
 *
 *
 */
public class PropertyTableBuilder {

    public static Node CSV_ROW_NODE = NodeFactory.createURI(LangCSV.CSV_ROW);

    private static String NAMESPACE;
    private static Properties MAPPING;

    public static PropertyTable buildPropetyTableHashMapImplFromCsv(String csvFilePath) {
        PropertyTable table = new PropertyTableHashMapImpl();
        return fillPropertyTable(table, csvFilePath);
    }

    public static PropertyTable buildPropetyTableArrayImplFromCsv(String csvFilePath) {
        PropertyTable table = createEmptyPropertyTableArrayImpl(csvFilePath);
        return fillPropertyTable(table, csvFilePath);
    }

    public static PropertyTable buildPropetyTableArrayImplFromCsv(String namespace, Properties mapping, String csvFilePath) {
        NAMESPACE = namespace;
        MAPPING = mapping;
        PropertyTable table = createEmptyPropertyTableArrayImpl(csvFilePath);
        return fillPropertyTable(table, csvFilePath);
    }

    private static PropertyTable createEmptyPropertyTableArrayImpl(String csvFilePath) {
        CSVParser parser = CSVParser.create(csvFilePath);
        List<String> rowLine = null;
        int rowNum = 0;
        int columnNum = 0;

        while ((rowLine = parser.parse1()) != null) {
            if (rowNum == 0) {
                columnNum = rowLine.size();
            }
            rowNum++;
        }
        if (rowNum != 0 && columnNum != 0) {
            return new PropertyTableArrayImpl(rowNum, columnNum + 1);
        } else {
            return null;
        }
    }

    protected static PropertyTable fillPropertyTable(PropertyTable table, String csvFilePath) {
        InputStream input = IO.openFile(csvFilePath);
        CSVParser iterator = CSVParser.create(input);
        return fillPropertyTable(table, iterator, csvFilePath);
    }

    protected static PropertyTable fillPropertyTable(PropertyTable table, CSVParser parser, String csvFilePath) {
        if (table == null) {
            return null;
        }
        ArrayList<Node> predicates = new ArrayList<Node>();
        int rowNum = 0;

        Iterator<List<String>> iter = parser.iterator();
        if (!iter.hasNext()) {
            return table;
        }
        List<String> row1 = iter.next();
        table.createColumn(CSV_ROW_NODE);

        String namespace = null;
        if (NAMESPACE == null) {
            namespace = csvFilePath;
        } else {
            namespace = NAMESPACE;
        }

        int colNum = 0;

        Map<Integer, String> columnDatatypes = new HashMap<Integer, String>();
        for (String column : row1) {
            colNum++;

            String rdfDatatype = null;
            String uri = null;
            if (MAPPING != null) {
                String value = MAPPING.getProperty(String.valueOf(colNum));
                if (value != null) {
                    String[] arr = value.split("\\>");
                    String uriTmp = arr[0].trim();
                    if (!uriTmp.isEmpty()) {
                        try {
                            new URI(uriTmp);
                            uri = uriTmp;
                        } catch (URISyntaxException e) {
                            uri = null;
                            System.out.println("The predicate URI " + uriTmp + " provided for column " + colNum + " is not a valid URI. Hence, it will be ignored and the default value will used for such a column.");
                        }
                        if (uri == null) {
                            uri = createColumnKeyURI(namespace, column);
                        }
                    }
                    if (arr.length > 1) {
                        String datatype = arr[1].trim();
                        if (!datatype.isEmpty()) {
                            try {
                                new URI(datatype);
                                rdfDatatype = datatype;
                            } catch (URISyntaxException e) {
                                rdfDatatype = null;
                                System.out.println("The type " + datatype + " provided for column " + colNum + " is not a valid URI. Hence, it will be ignored.");
                            }

                        }
                    }
                }
            } else {
                uri = createColumnKeyURI(namespace, column);
            }

            if (rdfDatatype != null) {
                columnDatatypes.put(colNum - 1, rdfDatatype);
            }

            Node p = NodeFactory.createURI(uri);
            predicates.add(p);
            table.createColumn(p);
        }

        rowNum++;
        while (iter.hasNext()) {
            List<String> rowLine = iter.next();
            Node subject = LangCSV.caculateSubject(rowNum, csvFilePath);
            Row row = table.createRow(subject);

            row.setValue(table.getColumn(CSV_ROW_NODE),
                    NodeFactory.createLiteral((rowNum + ""), XSDDatatype.XSDinteger));

            for (int col = 0; col < rowLine.size() && col < predicates.size(); col++) {

                String columnValue = rowLine.get(col).trim();
                if ("".equals(columnValue)) {
                    continue;
                }
                Node o;
                try {

                    String rdfDatatype = columnDatatypes.get(col);
                    if (rdfDatatype != null) {
                        RDFDatatype jenaRdfDtatype = TypeMapper.getInstance().getSafeTypeByName(rdfDatatype);
                        o = NodeFactory.createLiteral(columnValue, jenaRdfDtatype);
                    } else {
                        Class<?>[] clazzes = new Class<?>[]{Integer.class, Double.class, Boolean.class};

                        Object obj = null;
                        for (Class<?> clazz : clazzes) {
                            Method method = clazz.getDeclaredMethod("valueOf", String.class);

                            if (method != null) {
                                try {
                                    obj = method.invoke(null, columnValue);
                                    if (obj instanceof Boolean && !columnValue.equalsIgnoreCase("false")) {
                                        obj = null;
                                        continue;
                                    } else {
                                        break;
                                    }

                                } catch (InvocationTargetException ex) {
                                    obj = null;
                                }
                            }
                        }

                        if (obj == null) {
                            obj = columnValue;
                        }

                        o = ResourceFactory.createTypedLiteral(obj).asNode();
                    }
                } catch (Exception e) {
                    o = NodeFactory.createLiteral(columnValue);
                }
                row.setValue(table.getColumn(predicates.get(col)), o);
            }
            rowNum++;
        }
        return table;
    }

    protected static String createColumnKeyURI(String csvFilePath, String column) {
        String uri = IRIResolver.resolveString(csvFilePath) + "#" + LangCSV.toSafeLocalname(column);
        return uri;
    }
}
