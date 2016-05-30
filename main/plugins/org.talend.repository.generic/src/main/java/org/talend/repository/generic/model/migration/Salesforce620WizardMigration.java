// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.generic.model.migration;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.runtime.model.components.IComponentConstants;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.service.ComponentService;
import org.talend.components.api.wizard.ComponentWizard;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.migration.AbstractItemMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.runtime.util.GenericTypeUtils;
import org.talend.core.utils.ReflectionUtils;
import org.talend.core.utils.TalendQuoteUtils;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.daikon.NamedThing;
import org.talend.daikon.properties.EnumProperty;
import org.talend.daikon.properties.Properties;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.PropertyValueEvaluator;
import org.talend.designer.core.generic.utils.ComponentsUtils;
import org.talend.repository.generic.model.genericMetadata.GenericConnection;
import org.talend.repository.generic.model.genericMetadata.GenericConnectionItem;
import org.talend.repository.generic.util.GenericWizardServiceFactory;
import orgomg.cwm.objectmodel.core.TaggedValue;

/**
 * created by nrousseau on May 30, 2016 Detailled comment
 *
 */
public class Salesforce620WizardMigration extends AbstractItemMigrationTask {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.migration.IMigrationTask#getOrder()
     */
    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2016, 5, 30, 12, 0, 0);
        return gc.getTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.migration.AbstractItemMigrationTask#execute(org.talend.core.model.properties.Item)
     */
    @Override
    public ExecutionResult execute(Item item) {
        if (GenericWizardServiceFactory.getGenericWizardService().isGenericItem(item)) {
            try {
                GenericConnectionItem connectionItem = (GenericConnectionItem) item;
                GenericConnection connection = (GenericConnection) connectionItem.getConnection();
                String serialized = connection.getCompProperties();
                ComponentService service = ComponentsUtils.getComponentService();
                ComponentWizard componentWizard = service.getComponentWizard(NewSalesforceWizardMigrationTask.TYPE_NAME, item
                        .getProperty().getId());
                ComponentProperties newProperties = (ComponentProperties) componentWizard.getForms().get(0).getProperties();
                newProperties.init();
                ComponentProperties properties = loadProperties(serialized, newProperties);
                updateSubProperties(properties, newProperties);
                connection.setCompProperties(properties.toSerialized());
                Set<MetadataTable> tables = ConnectionHelper.getTables(connection);
                for (MetadataTable table : tables) {
                    EList<TaggedValue> values = table.getTaggedValue();
                    for (TaggedValue value : values) {
                        if (IComponentConstants.COMPONENT_PROPERTIES_TAG.equals(value.getTag())) {
                            Object object = ReflectionUtils.newInstance(
                                    NewSalesforceWizardMigrationTask.REFLECTION_SALESFORCE_MODULE_PROPERTIES, newProperties
                                            .getClass().getClassLoader(), new Object[] { table.getName() });
                            if (object != null && object instanceof ComponentProperties) {
                                ComponentProperties newSalesforceModuleProperties = (ComponentProperties) object;
                                ComponentProperties moduleProperties = loadProperties(serialized, newSalesforceModuleProperties);
                                updateSubProperties(moduleProperties, newSalesforceModuleProperties);
                                connection.setCompProperties(moduleProperties.toSerialized());
                            }

                        }
                    }
                }
                ProxyRepositoryFactory.getInstance().save(connectionItem, true);
                return ExecutionResult.SUCCESS_NO_ALERT;
            } catch (Exception e) {
                ExceptionHandler.process(e);
                return ExecutionResult.FAILURE;

            }
        }
        return ExecutionResult.NOTHING_TO_DO;
    }

    /**
     * DOC nrousseau Comment method "loadProperties".
     * 
     * @param serialized
     * @param newProperties
     * @return
     */
    private ComponentProperties loadProperties(String serialized, ComponentProperties newProperties) {
        return ComponentProperties.fromSerialized(serialized, ComponentProperties.class,
                new Properties.PostSerializationSetup<ComponentProperties>() {

                    @Override
                    public void setup(ComponentProperties properties) {
                        properties.setValueEvaluator(new PropertyValueEvaluator() {

                            @Override
                            public Object evaluate(Property property, Object storedValue) {
                                if (storedValue instanceof String) {
                                    if (GenericTypeUtils.isEnumType(property)) {
                                        Property newProperty = (Property) newProperties.getProperty(property.getName());
                                        if (newProperty == null) {
                                            newProperty = (Property) newProperties.getProperty("connection.loginType");
                                        }
                                        if (newProperty != null) {
                                            List<?> propertyPossibleValues = ((Property<?>) newProperty).getPossibleValues();
                                            if (propertyPossibleValues != null) {
                                                for (Object possibleValue : propertyPossibleValues) {
                                                    if (possibleValue.toString().equals(storedValue)) {
                                                        property.setStoredValue(possibleValue);
                                                        return possibleValue;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                return storedValue;
                            }

                        });
                    }
                }).properties;
    }

    /**
     * DOC nrousseau Comment method "updateSubProperties".
     * 
     * @param properties
     * @param newProperties
     */
    private void updateSubProperties(ComponentProperties properties, ComponentProperties newProperties) {
        if (newProperties == null) {
            return;
        }
        for (NamedThing nt : properties.getProperties()) {
            if (nt instanceof Property) {
                Property property = (Property) nt;
                Object storedValue = property.getStoredValue();
                if (storedValue instanceof String) {
                    String stringValue = (String) storedValue;
                    if (ContextParameterUtils.isContainContextParam(stringValue)) {
                        continue;
                    }
                    Property newProperty = (Property) newProperties.getProperty(property.getName());
                    if (newProperty != null) {
                        if (GenericTypeUtils.isBooleanType(newProperty)) {
                            if (stringValue.isEmpty()) {
                                property.setValue(Boolean.FALSE);
                            } else {
                                property.setValue(new Boolean(stringValue));
                            }
                        } else if (GenericTypeUtils.isEnumType(newProperty) && (!(newProperty instanceof EnumProperty))) {
                            property.setStoredValue(TalendQuoteUtils.removeQuotes(stringValue));
                        } else if (GenericTypeUtils.isEnumType(property)) {
                            List<?> propertyPossibleValues = ((Property<?>) newProperty).getPossibleValues();
                            if (propertyPossibleValues != null) {
                                for (Object possibleValue : propertyPossibleValues) {
                                    if (possibleValue.toString().equals(storedValue)) {
                                        property.setStoredValue(possibleValue);
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }
            } else if (nt instanceof ComponentProperties) {
                updateSubProperties((ComponentProperties) nt, (ComponentProperties) newProperties.getProperty(nt.getName()));
            }
        }
    }

}
