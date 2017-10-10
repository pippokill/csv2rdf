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

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.propertytable.Column;
import org.apache.jena.propertytable.PropertyTable;

/**
 * The simple implementation of Column
 *
 */
public class ColumnImpl implements Column {

    private final PropertyTable table;
    private Node p;

    ColumnImpl(PropertyTable table, Node p) {
        this.table = table;
        this.p = p;
    }

    @Override
    public PropertyTable getTable() {
        return table;
    }

    @Override
    public Node getColumnKey() {
        return p;
    }

    @Override
    public List<Node> getValues() {
        return table.getColumnValues(this);
    }
}
