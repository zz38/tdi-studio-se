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
package org.talend.designer.core.model.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.core.model.components.IComponent;
import org.talend.core.model.components.IMultipleComponentItem;
import org.talend.core.model.components.IMultipleComponentManager;
import org.talend.core.model.components.IMultipleComponentParameter;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.AbstractConnection;
import org.talend.core.model.process.AbstractNode;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IExternalNode;
import org.talend.core.model.process.INode;
import org.talend.designer.core.ui.editor.connections.EDesignerConnection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.model.ExternalNodesFactory;

/**
 * This class will create the list of nodes that will be used to generate the code.
 * 
 * $Id$
 * 
 */
public class DataProcess {

    private static final String HASH_COMPONENT_NAME = "tHash";

    private static Map<Node, INode> buildCheckMap = null;

    private static List<Node> checkRefList = null;

    private static Map<Node, INode> checkMultipleMap = null;

    private static List<INode> dataNodeList;

    private static void initialize() {
        buildCheckMap = new HashMap<Node, INode>();
        checkRefList = new ArrayList<Node>();
        checkMultipleMap = new HashMap<Node, INode>();
        dataNodeList = new ArrayList<INode>();
    }

    // should only be called by a starting node
    @SuppressWarnings("unchecked")
    private static INode buildfromNode(final Node graphicalNode) {
        if (buildCheckMap.containsKey(graphicalNode)) {
            return buildCheckMap.get(graphicalNode);
        }

        AbstractNode dataNode;

        if (graphicalNode.getExternalNode() == null) {
            dataNode = new DataNode();
        } else {
            // mapper
            dataNode = (AbstractNode) ExternalNodesFactory.getInstance(graphicalNode.getPluginFullName());
            Object externalData = graphicalNode.getExternalData();
            if (externalData != null) {
                ((IExternalNode) dataNode).setExternalData(externalData);
            }
        }
        buildCheckMap.put(graphicalNode, dataNode);
        dataNodeList.add(dataNode);
        dataNode.setActivate(graphicalNode.isActivate());
        dataNode.setStart(graphicalNode.isStart());
        dataNode.setComponentName(graphicalNode.getComponentName());
        dataNode.setMetadataList(graphicalNode.getMetadataList());
        dataNode.setPluginFullName(graphicalNode.getPluginFullName());
        dataNode.setElementParameters(graphicalNode.getElementParameters());
        dataNode.setUniqueName(graphicalNode.getUniqueName());
        dataNode.setSubProcessStart(graphicalNode.isSubProcessStart());
        dataNode.setMultipleMethods(graphicalNode.isMultipleMethods());
        dataNode.setProcess(graphicalNode.getProcess());
        dataNode.setComponent(graphicalNode.getComponent());

        List<IConnection> outgoingConnections = new ArrayList<IConnection>();
        List<IConnection> incomingConnections = new ArrayList<IConnection>();
        dataNode.setIncomingConnections(incomingConnections);
        dataNode.setOutgoingConnections(outgoingConnections);

        DataConnection dataConnec;
        for (IConnection connection : graphicalNode.getOutgoingConnections()) {
            dataConnec = new DataConnection();
            dataConnec.setActivate(connection.isActivate());
            dataConnec.setLineStyle(connection.getLineStyle());
            dataConnec.setMetadataTable(connection.getMetadataTable());
            dataConnec.setName(connection.getName());
            dataConnec.setSource(dataNode);
            dataConnec.setCondition(connection.getCondition());
            INode target = buildfromNode((Node) connection.getTarget());
            dataConnec.setTarget(target);
            incomingConnections = (List<IConnection>) target.getIncomingConnections();
            if (incomingConnections == null) {
                incomingConnections = new ArrayList<IConnection>();
            }
            outgoingConnections.add(dataConnec);
            incomingConnections.add(dataConnec);
        }

        return dataNode;
    }

    /**
     * DOC nrousseau Comment method "addMultipleNode".
     * 
     * @param graphicalNode
     * @param multipleComponentManager
     * @return
     */
    @SuppressWarnings("unchecked")
    private static AbstractNode addMultipleNode(Node graphicalNode, IMultipleComponentManager multipleComponentManager) {
        AbstractNode dataNode;
        // prepare all the nodes

        INode previousNode = buildCheckMap.get(graphicalNode);
        dataNodeList.remove(previousNode);

        Map<IMultipleComponentItem, AbstractNode> itemsMap = new HashMap<IMultipleComponentItem, AbstractNode>();

        prepareAllMultipleComponentNodes(itemsMap, multipleComponentManager, graphicalNode);
        setMultipleComponentParameters(multipleComponentManager, itemsMap, graphicalNode);

        // set the first one (input) with the properties of the graphical node.
        dataNode = itemsMap.get(multipleComponentManager.getInput());
        dataNode.setStart(graphicalNode.isStart());
        dataNode.setSubProcessStart(graphicalNode.isSubProcessStart());
        List<IConnection> incomingConnections = (List<IConnection>) dataNode.getIncomingConnections();
        for (IConnection connection : previousNode.getIncomingConnections()) {
            AbstractConnection asbractConnect = (AbstractConnection) connection;
            asbractConnect.setTarget(dataNode);
            incomingConnections.add(connection);
        }

        // set informations for the last node, so the outgoing connections.
        INode outputNode = itemsMap.get(multipleComponentManager.getOutput());
        List<IConnection> outgoingConnections = (List<IConnection>) outputNode.getOutgoingConnections();

        // RunBefore / RunAfter Links won't be linked to the output but on the first element of the subprocess.
        for (IConnection connection : previousNode.getOutgoingConnections()) {
            if (!connection.getLineStyle().equals(EConnectionType.RUN_BEFORE)
                    && !connection.getLineStyle().equals(EConnectionType.RUN_AFTER)) {
                AbstractConnection asbractConnect = (AbstractConnection) connection;
                asbractConnect.setSource(outputNode);
                outgoingConnections.add(connection);
            }
        }

        // adds all connections between these nodes
        addAllMultipleComponentConnections(itemsMap, multipleComponentManager, graphicalNode, dataNode, previousNode);

        // adds the RunBefore / RunAfter link that were on the output of the previousNode to the new "start".
        INode nodeSourceAfter, nodeSourceBefore;
        INode startNode = graphicalNode.getSubProcessStartNode(false);
        INode dataStartNode = buildCheckMap.get(startNode);
        if (dataStartNode != previousNode) {
            nodeSourceAfter = dataNode;
            nodeSourceBefore = dataNode;
        } else {
            INode newNodeSourceAfter = dataNode;
            INode newNodeSourceBefore = dataNode;
            INode nextNode = newNodeSourceAfter;
            boolean found;
            do {
                found = false;
                for (IConnection connection : newNodeSourceAfter.getIncomingConnections()) {
                    if (connection.getLineStyle().equals(EConnectionType.RUN_BEFORE)) {
                        if (itemsMap.containsValue(connection.getSource())) {
                            nextNode = connection.getSource();
                            found = true;
                        }
                    }
                }
                newNodeSourceAfter = nextNode;
            } while (found);
            nodeSourceAfter = newNodeSourceAfter;
            
            do {
                found = false;
                for (IConnection connection : newNodeSourceBefore.getIncomingConnections()) {
                    if (connection.getLineStyle().equals(EConnectionType.RUN_AFTER)) {
                        if (itemsMap.containsValue(connection.getSource())) {
                            nextNode = connection.getSource();
                            found = true;
                        }
                    }
                }
                newNodeSourceBefore = nextNode;
            } while (found);
            nodeSourceBefore = newNodeSourceBefore;
        }
        outgoingConnections = (List<IConnection>) nodeSourceAfter.getOutgoingConnections();
        for (IConnection connection : previousNode.getOutgoingConnections()) {
            if (connection.getLineStyle().equals(EConnectionType.RUN_AFTER)) {
                AbstractConnection asbractConnect = (AbstractConnection) connection;
                asbractConnect.setSource(nodeSourceAfter);
                outgoingConnections.add(connection);
            }
        }
        
        outgoingConnections = (List<IConnection>) nodeSourceBefore.getOutgoingConnections();
        for (IConnection connection : previousNode.getOutgoingConnections()) {
            if (connection.getLineStyle().equals(EConnectionType.RUN_BEFORE)) {
                AbstractConnection asbractConnect = (AbstractConnection) connection;
                asbractConnect.setSource(nodeSourceBefore);
                outgoingConnections.add(connection);
            }
        }

        return dataNode;
    }

    /**
     * DOC nrousseau Comment method "addAllMultipleComponentConnections".
     * 
     * @param itemsMap
     * @param multipleComponentManager
     * @param graphicalNode
     * @param dataNode
     */
    private static void addAllMultipleComponentConnections(Map<IMultipleComponentItem, AbstractNode> itemsMap,
            IMultipleComponentManager multipleComponentManager, Node graphicalNode, AbstractNode dataNode,
            INode previousNode) {
        List<IConnection> incomingConnections, outgoingConnections;

        for (IMultipleComponentItem curItem : multipleComponentManager.getItemList()) {
            if (curItem.isConnectionExist()) {
                AbstractNode nodeSource = itemsMap.get(curItem);
                AbstractNode nodeTarget;
                if (curItem.getLinkTo() == null) {
                    nodeTarget = dataNode;
                } else {
                    nodeTarget = itemsMap.get(curItem.getLinkTo());
                }
                DataConnection dataConnec = new DataConnection();
                dataConnec.setActivate(graphicalNode.isActivate());
                dataConnec.setLineStyle(curItem.getConnectionType());
                dataConnec.setMetadataTable(nodeSource.getMetadataList().get(0));

                EDesignerConnection designerConnection = EDesignerConnection.getConnection(curItem.getConnectionType());
                dataConnec.setName(designerConnection.getLinkName());

                switch (curItem.getConnectionType()) {
                case FLOW_MAIN:
                    dataConnec.setName("row_" + itemsMap.get(curItem).getUniqueName());
                    break;
                case RUN_BEFORE:
                case RUN_AFTER:
                case RUN_IF_OK:
                case RUN_IF_ERROR:
                    if (nodeTarget.equals(dataNode)) {
                        INode startNode = graphicalNode.getSubProcessStartNode(false);
                        INode dataStartNode = buildCheckMap.get(startNode);
                        if (dataStartNode != previousNode) {
                            nodeTarget = (AbstractNode) dataStartNode;
                        }
                    }
                    List<IConnection> connectionsToRemoveFromList = new ArrayList<IConnection>();
                    incomingConnections = (List<IConnection>) nodeTarget.getIncomingConnections();
                    for (IConnection connec : incomingConnections) {
                        switch (connec.getLineStyle()) {
                        case RUN_BEFORE:
                        case RUN_AFTER:
                        case RUN_IF_OK:
                        case RUN_IF_ERROR:
                        case RUN_IF:
                            connectionsToRemoveFromList.add(connec);
                            AbstractConnection connection = (AbstractConnection) connec;
                            connection.setTarget(nodeSource);
                            break;
                        default:
                            break;
                        }
                    }
                    incomingConnections.removeAll(connectionsToRemoveFromList);
                    incomingConnections = (List<IConnection>) nodeSource.getIncomingConnections();
                    incomingConnections.addAll(connectionsToRemoveFromList);
                    if (nodeTarget.isStart()) {
                        nodeTarget.setStart(false);
                        nodeSource.setStart(true);
                    }
                    nodeSource.setSubProcessStart(true);
                    break;
                default:
                    break;
                }
                dataConnec.setSource(nodeSource);
                dataConnec.setTarget(nodeTarget);
                dataConnec.setCondition("");
                outgoingConnections = (List<IConnection>) nodeSource.getOutgoingConnections();
                outgoingConnections.add(dataConnec);
                incomingConnections = (List<IConnection>) nodeTarget.getIncomingConnections();
                incomingConnections.add(dataConnec);
            }
        }
    }

    /**
     * DOC nrousseau Comment method "prepareAllMultipleComponentNodes".
     * 
     * @param itemsMap
     * @param multipleComponentManager
     * @param graphicalNode
     */
    private static void prepareAllMultipleComponentNodes(Map<IMultipleComponentItem, AbstractNode> itemsMap,
            IMultipleComponentManager multipleComponentManager, Node graphicalNode) {

        List<IMultipleComponentItem> itemList = multipleComponentManager.getItemList();

        for (IMultipleComponentItem curItem : itemList) {
            String uniqueName = graphicalNode.getUniqueName() + "_" + curItem.getName();
            IComponent component = ComponentsFactoryProvider.getInstance().get(curItem.getName());
            DataNode curNode = new DataNode(component, uniqueName);
            curNode.setActivate(graphicalNode.isActivate());
            IMetadataTable newMetadata = graphicalNode.getMetadataList().get(0).clone();
            newMetadata.setTableName(uniqueName);
            curNode.getMetadataList().remove(0);
            curNode.getMetadataList().add(newMetadata);
            List<IConnection> outgoingConnections = new ArrayList<IConnection>();
            List<IConnection> incomingConnections = new ArrayList<IConnection>();
            curNode.setIncomingConnections(incomingConnections);
            curNode.setOutgoingConnections(outgoingConnections);
            dataNodeList.add(curNode);
            itemsMap.put(curItem, curNode);
        }
    }

    /**
     * DOC nrousseau Comment method "setMultipleComponentParameters".
     * 
     * @param multipleComponentManager
     * @param itemsMap
     * @param graphicalNode
     */
    private static void setMultipleComponentParameters(IMultipleComponentManager multipleComponentManager,
            Map<IMultipleComponentItem, AbstractNode> itemsMap, Node graphicalNode) {

        List<IMultipleComponentItem> itemList = multipleComponentManager.getItemList();

        // set the specific parameters value.
        for (IMultipleComponentParameter param : multipleComponentManager.getParamList()) {
            INode sourceNode = null, targetNode = null;
            boolean sourceFound = false, targetFound = false;
            if (param.getSourceComponent().equals(graphicalNode.getComponentName())) {
                sourceNode = graphicalNode;
            } else {
                for (int i = 0; i < itemList.size() && !sourceFound; i++) {
                    if (itemList.get(i).getName().equals(param.getSourceComponent())) {
                        sourceNode = itemsMap.get(itemList.get(i));
                        sourceFound = true;
                    }
                }
            }
            for (int i = 0; i < itemList.size() && !targetFound; i++) {
                if (itemList.get(i).getName().equals(param.getTargetComponent())) {
                    targetNode = itemsMap.get(itemList.get(i));
                    targetFound = true;
                }
            }
            if ((sourceNode != null) && (targetNode != null)) {
                sourceFound = false;
                targetFound = false;
                IElementParameter paramSource = null, paramTarget = null;
                for (int i = 0; i < sourceNode.getElementParameters().size() && !sourceFound; i++) {
                    if (sourceNode.getElementParameters().get(i).getName().equals(param.getSourceValue())) {
                        paramSource = sourceNode.getElementParameters().get(i);
                        sourceFound = true;
                    }
                }

                for (int i = 0; i < targetNode.getElementParameters().size() && !targetFound; i++) {
                    if (targetNode.getElementParameters().get(i).getName().equals(param.getTargetValue())) {
                        paramTarget = targetNode.getElementParameters().get(i);
                        targetFound = true;
                    }
                }
                if ((paramSource != null) && (paramTarget != null)) {
                    paramTarget.setValue(paramSource.getValue());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkFlowRefLink(final Node graphicalNode) {
        if (checkRefList.contains(graphicalNode)) {
            return;
        }
        checkRefList.add(graphicalNode);
        for (IConnection connection : graphicalNode.getOutgoingConnections()) {
            if (connection.getLineStyle().equals(EConnectionType.FLOW_REF)) {
                INode refSource = buildCheckMap.get(graphicalNode);

                // retrieve the starts node of each current nodes to add a before link
                Node subNodeStartTarget = graphicalNode.getSubProcessStartNode(true);
                Node subNodeStartSource = ((Node) connection.getTarget()).getSubProcessStartNode(false);
                AbstractNode subDataNodeStartSource = (AbstractNode) buildCheckMap.get(subNodeStartSource);
                AbstractNode subDataNodeStartTarget = (AbstractNode) buildCheckMap.get(subNodeStartTarget);

                // create a link before between the two subprocess
                DataConnection dataConnec = new DataConnection();
                dataConnec.setActivate(connection.isActivate());
                dataConnec.setLineStyle(EConnectionType.RUN_AFTER);
                dataConnec.setMetadataTable(subDataNodeStartSource.getMetadataList().get(0));
                dataConnec.setName("after_" + subDataNodeStartSource.getUniqueName());
                dataConnec.setSource(subDataNodeStartSource);
                dataConnec.setTarget(subDataNodeStartTarget);
                List<IConnection> outgoingConnections = (List<IConnection>) subDataNodeStartSource
                        .getOutgoingConnections();
                outgoingConnections.add(dataConnec);
                List<IConnection> incomingConnections = (List<IConnection>) subDataNodeStartTarget
                        .getIncomingConnections();
                incomingConnections.add(dataConnec);

                // add a new hash node
                // (to replace by a Node maybe that will take the informations of an IComponent)
                String uniqueName = HASH_COMPONENT_NAME + "_" + connection.getName();
                IComponent component = ComponentsFactoryProvider.getInstance().get(HASH_COMPONENT_NAME);
                DataNode hashNode = new DataNode(component, uniqueName);
                hashNode.setActivate(connection.isActivate());
                hashNode.setStart(false);
                IMetadataTable newMetadata = refSource.getMetadataList().get(0).clone();
                newMetadata.setTableName(uniqueName);
                hashNode.getMetadataList().remove(0);
                hashNode.getMetadataList().add(newMetadata);
                hashNode.setSubProcessStart(false);
                outgoingConnections = new ArrayList<IConnection>();
                incomingConnections = new ArrayList<IConnection>();
                hashNode.setIncomingConnections(incomingConnections);
                hashNode.setOutgoingConnections(outgoingConnections);

                dataNodeList.add(hashNode);

                // create a link flow_main between the node that had ref and the hash file
                dataConnec = new DataConnection();
                dataConnec.setActivate(connection.isActivate());
                dataConnec.setLineStyle(EConnectionType.FLOW_MAIN);
                dataConnec.setMetadataTable(refSource.getMetadataList().get(0));
                dataConnec.setName(connection.getName());
                // dataConnec.setName(refSource.getUniqueName() + "_to_hash_" + connection.getName());
                dataConnec.setSource(refSource);
                dataConnec.setTarget(hashNode);
                outgoingConnections = (List<IConnection>) refSource.getOutgoingConnections();
                outgoingConnections.add(dataConnec);
                incomingConnections = (List<IConnection>) hashNode.getIncomingConnections();
                incomingConnections.add(dataConnec);
            }
            checkFlowRefLink((Node) connection.getTarget());
        }
    }

    /**
     * DOC nrousseau Comment method "replaceMultipleComponents".
     * 
     * @param node
     */
    private static INode replaceMultipleComponents(Node graphicalNode) {
        if (checkMultipleMap.containsKey(graphicalNode)) {
            return checkMultipleMap.get(graphicalNode);
        }
        AbstractNode dataNode;

        IMultipleComponentManager multipleComponentManager = graphicalNode.getComponent().getMultipleComponentManager();

        dataNode = (AbstractNode) buildCheckMap.get(graphicalNode);
        checkMultipleMap.put(graphicalNode, dataNode);
        if (multipleComponentManager != null) {
            dataNode = addMultipleNode(graphicalNode, multipleComponentManager);
        }

        for (IConnection connection : graphicalNode.getOutgoingConnections()) {
            replaceMultipleComponents((Node) connection.getTarget());
        }
        return dataNode;
    }

    public static void buildFromGraphicalProcess(List<Node> graphicalNodeList) {

        initialize();
        for (Node node : graphicalNodeList) {
            if (node.getIncomingConnections().size() == 0) {
                buildfromNode(node);
            }
        }
        for (Node node : graphicalNodeList) {
            if (node.getIncomingConnections().size() == 0) {
                checkFlowRefLink(node);
            }
        }
        for (Node node : graphicalNodeList) {
            if (node.getIncomingConnections().size() == 0) {
                replaceMultipleComponents(node);
            }
        }
    }

    public static List<INode> getNodeList() {
        return dataNodeList;
    }
}
