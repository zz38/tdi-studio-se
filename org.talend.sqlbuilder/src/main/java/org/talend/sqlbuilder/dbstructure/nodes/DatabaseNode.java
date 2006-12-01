// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.sqlbuilder.dbstructure.nodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.sqlexplorer.SQLAlias;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;

import org.eclipse.swt.graphics.Image;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.ui.images.ImageProvider;
import org.talend.sqlbuilder.Messages;
import org.talend.sqlbuilder.SqlBuilderPlugin;
import org.talend.sqlbuilder.sessiontree.model.SessionTreeNode;
import org.talend.sqlbuilder.util.TextUtil;

/**
 * Root node for a database. ChildNodes can be filtered based on expressions in
 * the alias.
 * 
 * @author Davy Vanherbergen
 */
public class DatabaseNode extends AbstractNode {

    private SQLAlias palias;

    private List pchildNames = new ArrayList();

    private String pdatabaseProductName = "";

    private String[] pfilterExpressions;

    private boolean psupportsCatalogs = false;

    private boolean psupportsSchemas = false;

    /**
     * Create a new database node with the given name.
     * 
     * @param name name
     * @param session session
     */
    public DatabaseNode(String name, SessionTreeNode session) {

        pname = name;
        psessionNode = session;
        palias = (SQLAlias) psessionNode.getAlias();
        pimageKey = "Images.DatabaseIcon";
    }


    /**
     * @return List of catalog nodes
     */
    @SuppressWarnings("unchecked")
    public List getCatalogs() {

        ArrayList catalogs = new ArrayList();

        Iterator it = getChildIterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof CatalogNode) {
                catalogs.add(o);
            }
        }

        return catalogs;
    }

    /**
     * @return ChildNames.
     */
    @SuppressWarnings("unchecked")
    public String[] getChildNames() {

        if (pchildNames.size() == 0) {
            getChildNodes();
        }
        return (String[]) pchildNames.toArray(new String[] {});
    }

    /**
     * @return DatabaseProductName.
     */
    public String getDatabaseProductName() {

        return pdatabaseProductName;
    }

    /**
     * @return LabelText.
     */
    public String getLabelText() {
        return palias.getName();
    }


    /**
     * @return List of all database schemas
     */
    @SuppressWarnings("unchecked")
    public List getSchemas() {

        ArrayList schemas = new ArrayList();

        Iterator it = getChildIterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof SchemaNode) {
                schemas.add(o);
            }
        }

        return schemas;
    }


    /**
     * Returns "database" as the type for this node.
     * @return Type.
     * @see org.talend.sqlbuilder.dbstructure.nodes.INode#getType()
     */
    public String getType() {

        return "database";
    }

    /**
     * @return UniqueIdentifier.
     */
    public String getUniqueIdentifier() {

        return getQualifiedName();
    }


    /**
     * Checks if a node name should be filtered.
     * 
     * @param name to check for filtering
     * @return true if the name should be filtered
     */
    private boolean isExcludedByFilter(String name) {

        if (pfilterExpressions == null || pfilterExpressions.length == 0) {
            // no active filter
            return false;
        }

        for (int i = 0; i < pfilterExpressions.length; i++) {

            String regex = pfilterExpressions[i].trim();
            regex = TextUtil.replaceChar(regex, '?', ".");
            regex = TextUtil.replaceChar(regex, '*', ".*");

            if (regex.length() != 0 && name.matches(regex)) {
                // we have a match, exclude node..
                return false;
            }
        }

        // no match found
        return true;

    }


    /**
     * Loads childnodes, filtered to a subset of schemas/databases depending on
     * whether a comma separated list of regular expression filters has been
     * set.
     */
    @SuppressWarnings("unchecked")
    public void loadChildren() {

        if (psessionNode.getInteractiveConnection() == null) {
            addChildNode(new CatalogNode(this, "", psessionNode));
            return;
        }
        
        pchildNames = new ArrayList();

        String metaFilterExpression = palias.getSchemaFilterExpression();
        if (metaFilterExpression != null && metaFilterExpression.trim().length() != 0) {
            pfilterExpressions = metaFilterExpression.split(",");
        } else {
            pfilterExpressions = null;
        }
        
        SQLDatabaseMetaData metadata = psessionNode.getMetaData();
        
        try {

//            if (metadata.supportsCatalogs()) {
//
//                final String[] catalogs = metadata.getCatalogs();
//                for (int i = 0; i < catalogs.length; ++i) {
//                    pchildNames.add(catalogs[i]);
//                    if (!isExcludedByFilter(catalogs[i])) {
//                        addChildNode(new CatalogNode(this, catalogs[i], psessionNode));
//                    }
//                }
//
//            } else if (metadata.supportsSchemas()) {
//
//                final String[] schemas = metadata.getSchemas();
//                for (int i = 0; i < schemas.length; ++i) {
//                    pchildNames.add(schemas[i]);
//                    if (!isExcludedByFilter(schemas[i])) {
//                        addChildNode(new SchemaNode(this, schemas[i], psessionNode));
//                    }
//                }
//
//            } else {

                addChildNode(new CatalogNode(this, palias.getSchemaFilterExpression(), psessionNode));
//            }
            

        } catch (Exception e) {
            SqlBuilderPlugin.log("Error loading children", e);
        }

    }


    /**
     * @return true if this database supports catalogs
     */
    public boolean supportsCatalogs() {

        return psupportsCatalogs;
    }


    /**
     * @return true if this database supports schemas
     */
    public boolean supportsSchemas() {

        return psupportsSchemas;
    }
    
    /**
     * @return Image.
     */
    public Image getImage() {
        return ImageProvider.getImage(ERepositoryObjectType.METADATA_CONNECTIONS);
    }

    
}
