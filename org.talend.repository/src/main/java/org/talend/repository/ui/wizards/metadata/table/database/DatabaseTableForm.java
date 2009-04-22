// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.metadata.table.database;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.commons.ui.swt.formtools.Form;
import org.talend.commons.ui.swt.formtools.LabelledCombo;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.commons.ui.swt.formtools.UtilsButton;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreator;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreator.LAYOUT_MODE;
import org.talend.commons.utils.data.list.IListenableListListener;
import org.talend.commons.utils.data.list.ListenableListEvent;
import org.talend.commons.utils.data.text.IndiceHelper;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.metadata.IMetadataConnection;
import org.talend.core.model.metadata.MetadataTalendType;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.TableHelper;
import org.talend.core.model.metadata.builder.database.ExtractMetaDataFromDataBase;
import org.talend.core.model.metadata.editor.MetadataEmfTableEditor;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.ui.metadata.editor.MetadataEmfTableEditorView;
import org.talend.core.utils.CsvArray;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.ui.swt.utils.AbstractForm;
import org.talend.repository.ui.utils.ManagerConnection;
import org.talend.repository.ui.wizards.metadata.connection.GuessSchemaUtil;

/**
 * @author ocarbone
 * 
 */
/**
 * DOC Administrator class global comment. Detailled comment <br/>
 * 
 */
public class DatabaseTableForm extends AbstractForm {

    protected static Logger log = Logger.getLogger(DatabaseTableForm.class);

    protected static final String PID = RepositoryPlugin.PLUGIN_ID;

    /**
     * FormTable Settings.
     */
    private static final int WIDTH_GRIDDATA_PIXEL = 650;

    private static final boolean STREAM_DETACH_IS_VISIBLE = false;

    private static final String GUESS_SCHEMA_TOOLTIP = Messages.getString("DatabaseTableForm.getRoughSchema"); //$NON-NLS-1$

    private static final String RETRIEVE_SCHEMA_TOOLTIP = Messages.getString("DatabaseTableForm.getDetailedSchema"); //$NON-NLS-1$

    /**
     * FormTable Var.
     */
    private ManagerConnection managerConnection;

    private List<String> itemTableName;

    private IMetadataConnection iMetadataConnection = null;

    private MetadataTable metadataTable;

    private MetadataEmfTableEditor metadataEditor;

    private MetadataEmfTableEditorView tableEditorView;

    /**
     * Buttons.
     */
    private UtilsButton retreiveSchemaButton;

    private UtilsButton checkConnectionButton;

    private UtilsButton guessSchemaButton; // hyWang add

    /**
     * Anothers Fields.
     */
    private String tableString;

    private Label tableSettingsInfoLabel;

    private Label typeText;

    /**
     * Main Fields.
     */
    private LabelledText nameText;

    private LabelledText commentText;

    private LabelledCombo tableCombo;

    private Button streamDetachCheckbox;

    /**
     * Flag.
     */

    boolean readOnly;

    private ConnectionItem connectionItem;

    private TableViewerCreator tableViewerCreator;

    private Table tableNavigator;

    private UtilsButton addTableButton;

    private IWizardPage parentWizardPage;

    /**
     * TableForm Constructor to use by RCP Wizard.
     * 
     * @param parent
     * @param connection
     * @param page
     * @param metadataTable
     * @param page
     * @param managerConnection2
     */
    public DatabaseTableForm(Composite parent, ConnectionItem connectionItem, MetadataTable metadataTable,
            ManagerConnection managerConnection, IWizardPage page) {
        super(parent, SWT.NONE);
        this.managerConnection = managerConnection;
        this.metadataTable = metadataTable;
        this.connectionItem = connectionItem;
        this.parentWizardPage = page;
        setupForm();
    }

    /**
     * DOC ocarbone Comment method "initExistingNames".
     * 
     * @param connection
     * @param metadataTable
     */
    private void initExistingNames() {
        String[] exisNames;
        if (metadataTable != null) {
            exisNames = TableHelper.getTableNames(getConnection(), metadataTable.getLabel());
        } else {
            exisNames = TableHelper.getTableNames(getConnection());
        }
        this.existingNames = existingNames == null ? Collections.EMPTY_LIST : Arrays.asList(exisNames);
    }

    /**
     * 
     * Initialize value, forceFocus first field for right Click (new Table).
     * 
     */
    public void initialize() {

    }

    /**
     * 
     * Initialize value, forceFocus first field for right Click (new Table).
     * 
     */
    public void initializeForm() {

        // init the nodes of the left tree navigator
        initTreeNavigatorNodes();
        initMetadataForm();
    }

    /**
     * DOC ocarbone Comment method "initTreeNodes".
     */
    private void initTreeNavigatorNodes() {

        if (metadataTable == null) {

            if (getConnection().getTables() != null && !getConnection().getTables().isEmpty()) {
                boolean isAllDeleted = true;
                for (int i = 0; i < getConnection().getTables().size(); i++) {
                    if (!TableHelper.isDeleted((MetadataTable) getConnection().getTables().get(i))) {
                        metadataTable = (MetadataTable) getConnection().getTables().get(i);
                        isAllDeleted = false;
                    }
                }
                if (isAllDeleted) {
                    addMetadataTable();
                }
            } else {
                addMetadataTable();
            }
        }

        tableNavigator.removeAll();

        String[] allTableLabel = TableHelper.getTableNames(getConnection());
        Arrays.sort(allTableLabel);

        for (int i = 0; i < allTableLabel.length; i++) {
            if (allTableLabel[i].equals(metadataTable.getLabel())) {
                TableItem subItem = new TableItem(tableNavigator, SWT.NONE);
                subItem.setText(allTableLabel[i]);
                tableNavigator.setSelection(subItem);
            } else if (!TableHelper.isDeleted(TableHelper.findByLabel(getConnection(), allTableLabel[i]))) {
                TableItem subItem = new TableItem(tableNavigator, SWT.NULL);
                subItem.setText(allTableLabel[i]);
            }
        }
    }

    /**
     * DOC ocarbone Comment method "initMetadataForm".
     */
    private void initMetadataForm() {
        // init the metadata Table

        metadataEditor.setMetadataTable(metadataTable);
        removeDoubleQuotes(metadataEditor.getMetadataColumnList());

        tableEditorView.setMetadataEditor(metadataEditor);
        tableEditorView.getTableViewerCreator().layout();

        // add listener to tableMetadata (listen the event of the toolbars)
        metadataEditor.addAfterOperationListListener(new IListenableListListener() {

            public void handleEvent(ListenableListEvent event) {
                changeTableNavigatorStatus(checkFieldsValue());
            }
        });

        // init the fields
        nameText.setText(metadataTable.getLabel());
        commentText.setText(metadataTable.getComment());
        if (metadataTable.getTableType() != null) {
            typeText.setText(Messages.getString("DatabaseTableForm.type", metadataTable.getTableType())); //$NON-NLS-1$
        } else {
            typeText.setText(Messages.getString("DatabaseTableForm.typeTable")); //$NON-NLS-1$
        }
        tableCombo.setText(metadataTable.getSourceName());
        nameText.forceFocus();
    }

    /**
     * DOC bqian Comment method "removeDoubleQuotes".
     * 
     * @param metadataColumnList see bug 3738
     */
    private void removeDoubleQuotes(List<MetadataColumn> metadataColumnList) {
        for (MetadataColumn metadataColumn : metadataColumnList) {
            handleDefaultValue(metadataColumn);
        }

    }

    /**
     * Adds double quotes if Talend type is Date or String.
     * 
     * @param bean
     * @param value
     * @return
     */
    private void handleDefaultValue(MetadataColumn bean) {
        // Checks if Talend type is String or Date.
        switch (LanguageManager.getCurrentLanguage()) {
        case JAVA:
            String returnValue = bean.getDefaultValue();
            if (bean.getTalendType().equals("id_String") || bean.getTalendType().equals("id_Date")) { //$NON-NLS-1$ //$NON-NLS-2$
                if (returnValue == null || returnValue.length() == 0) {
                    returnValue = null;
                } else if (returnValue.equalsIgnoreCase("null")) { //$NON-NLS-1$
                    returnValue = "null"; //$NON-NLS-1$
                } else {
                    returnValue = returnValue.replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    returnValue = returnValue.replaceAll("\'", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    returnValue = "\"" + returnValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$
                }
                bean.setDefaultValue(returnValue);
            }
        default:
            // if (bean.getTalendType() != null && bean.getTalendType().equals("string")
            // || bean.getTalendType().equals("date")) {
            // if (returnValue == null) {
            // returnValue = "null";
            // } else if (returnValue.length() == 0) {
            // returnValue = "\"" + "\"";
            // } else if (returnValue.equalsIgnoreCase("null")) {
            // returnValue = "null";
            // } else {
            // returnValue = returnValue.replaceAll("\"", "");
            // returnValue = returnValue.replaceAll("\'", "");
            // returnValue = "\"" + returnValue + "\"";
            // }
            // }
        }
    }

    protected void addFields() {
        int leftCompositeWidth = 125;
        int rightCompositeWidth = WIDTH_GRIDDATA_PIXEL - leftCompositeWidth;
        int headerCompositeHeight = 80;
        int tableSettingsCompositeHeight = 15;
        int tableCompositeHeight = 200;

        int height = headerCompositeHeight + tableSettingsCompositeHeight + tableCompositeHeight;

        // Main Composite : 2 columns
        Composite mainComposite = Form.startNewDimensionnedGridLayout(this, 2, leftCompositeWidth + rightCompositeWidth, height);
        mainComposite.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.FILL_BOTH);
        mainComposite.setLayoutData(gridData);

        Composite leftComposite = Form.startNewDimensionnedGridLayout(mainComposite, 1, leftCompositeWidth, height);
        Composite rightComposite = Form.startNewDimensionnedGridLayout(mainComposite, 1, rightCompositeWidth, height);

        addTreeNavigator(leftComposite, leftCompositeWidth, height);

        gridData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        gridData.widthHint = rightCompositeWidth;
        gridData.horizontalSpan = 3;

        // Header Fields
        Composite composite1 = Form.startNewDimensionnedGridLayout(rightComposite, 3, rightCompositeWidth, headerCompositeHeight);
        nameText = new LabelledText(composite1, Messages.getString("DatabaseTableForm.name"), 2); //$NON-NLS-1$
        commentText = new LabelledText(composite1, Messages.getString("DatabaseTableForm.comment"), 2); //$NON-NLS-1$

        typeText = new Label(composite1, SWT.NONE);
        typeText.setLayoutData(gridData);

        // Combo Table
        tableCombo = new LabelledCombo(composite1, Messages.getString("DatabaseTableForm.table"), Messages //$NON-NLS-1$
                .getString("DatabaseTableForm.tableTip"), itemTableName, true); //$NON-NLS-1$

        // Button retreiveSchema
        Composite compositeRetreiveSchemaButton = Form.startNewGridLayout(composite1, 3, false, SWT.CENTER, SWT.TOP);
        retreiveSchemaButton = new UtilsButton(compositeRetreiveSchemaButton, Messages
                .getString("DatabaseTableForm.retreiveSchema"), WIDTH_BUTTON_PIXEL, HEIGHT_BUTTON_PIXEL); //$NON-NLS-1$
        retreiveSchemaButton.setToolTipText(RETRIEVE_SCHEMA_TOOLTIP);
        // Button Check Connection
        checkConnectionButton = new UtilsButton(compositeRetreiveSchemaButton, "" //$NON-NLS-1$
                /*
                 * Messages.getString( "DatabaseTableForm.checkConnection" )
                 */, false); //$NON-NLS-1$

        tableSettingsInfoLabel = new Label(composite1, SWT.NONE);
        tableSettingsInfoLabel.setLayoutData(gridData);

        // Button guessSchema

        guessSchemaButton = new UtilsButton(compositeRetreiveSchemaButton,
                Messages.getString("DatabaseTableForm.guessSchema"), WIDTH_BUTTON_PIXEL, HEIGHT_BUTTON_PIXEL); //$NON-NLS-1$
        guessSchemaButton.setToolTipText(GUESS_SCHEMA_TOOLTIP);
        if (LanguageManager.getCurrentLanguage() == ECodeLanguage.JAVA) {
            guessSchemaButton.setVisible(true);
        } else {
            guessSchemaButton.setVisible(false);
        }

        // Checkbox streamDetach
        streamDetachCheckbox = new Button(composite1, SWT.CHECK);
        streamDetachCheckbox.setText(Messages.getString("DatabaseTableForm.streamDetach")); //$NON-NLS-1$
        streamDetachCheckbox.setAlignment(SWT.LEFT);
        streamDetachCheckbox.setVisible(STREAM_DETACH_IS_VISIBLE);

        // Group MetaData
        Group groupMetaData = Form.createGroup(rightComposite, 1, Messages.getString("DatabaseTableForm.groupMetaData"), //$NON-NLS-1$
                tableCompositeHeight);
        Composite compositeMetaData = Form.startNewGridLayout(groupMetaData, 1);

        Composite compositeTable = Form.startNewDimensionnedGridLayout(compositeMetaData, 1, rightCompositeWidth,
                tableCompositeHeight);
        compositeTable.setLayout(new FillLayout());
        metadataEditor = new MetadataEmfTableEditor(""); //$NON-NLS-1$
        tableEditorView = new MetadataEmfTableEditorView(compositeTable, SWT.NONE, false);
        tableEditorView.setShowDbTypeColumn(true, true, false);
        tableEditorView.setShowDbColumnName(true, false);
        final DatabaseConnection databaseConnection = (DatabaseConnection) connectionItem.getConnection();
        tableEditorView.setCurrentDbms(databaseConnection.getDbmsId());
        tableEditorView.initGraphicComponents();

        metadataEditor.setDefaultLabel(Messages.getString("DatabaseTableForm.metadataDefaultNewLabel")); //$NON-NLS-1$
        addUtilsButtonListeners();
    }

    /**
     * DOC ocarbone Comment method "addTreeNavigator".
     * 
     * @param parent
     * @param width
     * @param height
     */
    private void addTreeNavigator(Composite parent, int width, int height) {
        // Group
        Group group = Form.createGroup(parent, 1, Messages.getString("DatabaseTableForm.navigatorTree"), height); //$NON-NLS-1$

        // ScrolledComposite
        ScrolledComposite scrolledCompositeFileViewer = new ScrolledComposite(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NONE);
        scrolledCompositeFileViewer.setExpandHorizontal(true);
        scrolledCompositeFileViewer.setExpandVertical(true);
        GridData gridData1 = new GridData(GridData.FILL_BOTH);
        gridData1.widthHint = width + 12;
        gridData1.heightHint = height;
        gridData1.horizontalSpan = 2;
        scrolledCompositeFileViewer.setLayoutData(gridData1);

        tableViewerCreator = new TableViewerCreator(scrolledCompositeFileViewer);
        tableViewerCreator.setHeaderVisible(false);
        tableViewerCreator.setColumnsResizableByDefault(false);
        tableViewerCreator.setBorderVisible(false);
        tableViewerCreator.setLinesVisible(false);
        tableViewerCreator.setLayoutMode(LAYOUT_MODE.NONE);
        tableViewerCreator.setCheckboxInFirstColumn(false);
        tableViewerCreator.setFirstColumnMasked(false);

        tableNavigator = tableViewerCreator.createTable();
        tableNavigator.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableColumn tableColumn = new TableColumn(tableNavigator, SWT.NONE);
        tableColumn.setText(Messages.getString("DatabaseTableForm.tableColumnText.talbe")); //$NON-NLS-1$
        tableColumn.setWidth(width + 12);

        scrolledCompositeFileViewer.setContent(tableNavigator);
        scrolledCompositeFileViewer.setSize(width + 12, height);

        // Button Add metadata Table
        Composite button = Form.startNewGridLayout(group, HEIGHT_BUTTON_PIXEL, false, SWT.CENTER, SWT.CENTER);
        addTableButton = new UtilsButton(button, Messages.getString("DatabaseTableForm.AddTable"), width, HEIGHT_BUTTON_PIXEL); //$NON-NLS-1$
    }

    /**
     * addButtonControls.
     * 
     */
    protected void addUtilsButtonListeners() {

        // Event retreiveSchemaButton
        retreiveSchemaButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (retreiveSchemaButton.getEnabled()) {
                    pressRetreiveSchemaButton();
                    metadataTable.setSourceName(tableCombo.getText());
                }
            }
        });

        // Event guessSchemaButton
        guessSchemaButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (guessSchemaButton.getEnabled()) {
                    pressGuessSchemaButton();
                    metadataTable.setSourceName(tableCombo.getText());
                }
            }
        });

        // Event CheckConnection Button
        checkConnectionButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (!checkConnectionButton.getEnabled()) {
                    checkConnectionButton.setEnabled(true);
                    checkConnection(true);
                } else {
                    checkConnectionButton.setEnabled(false);
                }
            }
        });

        // Event addTable Button
        addTableButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (!addTableButton.getEnabled()) {
                    addTableButton.setEnabled(true);
                    addMetadataTable();
                } else {
                    addTableButton.setEnabled(false);
                }
            }
        });

    }

    /**
     * DOC ocarbone Comment method "addMetadataTable".
     */
    protected void addMetadataTable() {
        // Create a new metadata and Add it on the connection
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        metadataTable = ConnectionFactory.eINSTANCE.createMetadataTable();
        getConnection().getTables().add(metadataTable);
        metadataTable.setId(factory.getNextId());

        // initExistingNames();
        metadataTable.setLabel(IndiceHelper.getIndexedLabel(metadataTable.getLabel(), existingNames));
        // init TreeNavigator
        initTreeNavigatorNodes();
        // init The Form
        initMetadataForm();
    }

    /**
     * checkConnection and adapt the form.
     */
    private void adaptFormToCheckConnection() {
        // tableCombo.setEnabled(false);
        // checkConnectionButton.setVisible(true);

        retreiveSchemaButton.setEnabled(true);
        tableSettingsInfoLabel.setText(""); //$NON-NLS-1$
        tableCombo.setReadOnly(true);
        checkConnectionButton.setVisible(false);
        addTableButton.setEnabled(true);
        if (isReadOnly()) {
            retreiveSchemaButton.setEnabled(false);
            addTableButton.setEnabled(false);
        } else if (!managerConnection.getIsValide()) {
            // Connection failure
            tableSettingsInfoLabel.setText(Messages.getString("DatabaseTableForm.connectionFailure")); //$NON-NLS-1$

        } else if (tableCombo.getItemCount() <= 0) {
            // Connection is done but no table exist
            tableSettingsInfoLabel.setText(Messages.getString("DatabaseTableForm.tableNoExist")); //$NON-NLS-1$
        }
        // else {
        // // Connection is done and table(s) exist
        // tableSettingsInfoLabel.setText(""); //$NON-NLS-1$
        // tableCombo.setEnabled(true);
        // tableSettingsInfoLabel.setText(Messages.getString("DatabaseTableForm.retreiveButtonAlert")); //$NON-NLS-1$
        // checkConnectionButton.setVisible(false);
        // }
    }

    /**
     * checkConnectionButton.
     * 
     * @param displayMessageBox
     */
    protected void checkConnection(final boolean displayMessageBox) {

        if (tableCombo.getItemCount() > 0) {
            tableCombo.removeAll();
        }

        try {
            parentWizardPage.getWizard().getContainer().run(true, true, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.getString("CreateTableAction.action.createTitle"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                    managerConnection.check(getIMetadataConnection());

                    if (managerConnection.getIsValide()) {
                        itemTableName = ExtractMetaDataFromDataBase.returnTablesFormConnection(iMetadataConnection);

                        if (itemTableName.size() <= 0) {
                            // connection is done but any table exist
                            if (displayMessageBox) {
                                SelectorTableForm.openInfoDialogInUIThread(getShell(), Messages
                                        .getString("DatabaseTableForm.checkConnection"), Messages //$NON-NLS-1$
                                        .getString("DatabaseTableForm.tableNoExist"), true); //$NON-NLS-1$
                            }
                        } else {
                            Display.getDefault().asyncExec(new Runnable() {

                                public void run() {
                                    // connection is done and
                                    // tables exist
                                    String[] items = null;
                                    if (itemTableName != null && !itemTableName.isEmpty()) {
                                        items = new String[itemTableName.size()];
                                        tableCombo.setVisibleItemCount(itemTableName.size());
                                        // fill the combo
                                        for (int i = 0; i < itemTableName.size(); i++) {
                                            tableCombo.add(itemTableName.get(i));
                                            if (itemTableName.get(i).equals(metadataTable.getSourceName())) {
                                                tableCombo.select(i);
                                            }
                                        }
                                    }
                                    if (displayMessageBox) {
                                        String msg = Messages.getString("DatabaseTableForm.connectionIsDone"); //$NON-NLS-1$
                                        if (!isReadOnly()) {
                                            msg = msg + Messages.getString("DatabaseTableForm.retreiveButtonIsAccessible"); //$NON-NLS-1$
                                        }
                                        SelectorTableForm.openInfoDialogInUIThread(getShell(), Messages
                                                .getString("DatabaseTableForm.checkConnection"), msg, false); //$NON-NLS-1$
                                    }
                                }
                            });
                        }
                    } else if (displayMessageBox) {
                        // connection failure
                        getShell().getDisplay().asyncExec(new Runnable() {

                            public void run() {
                                new ErrorDialogWidthDetailArea(getShell(), PID, Messages
                                        .getString("DatabaseTableForm.connectionFailureTip"), //$NON-NLS-1$
                                        managerConnection.getMessageException());
                            }
                        });
                    }
                    monitor.done();
                }
            });
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        updateRetreiveSchemaButton();
        adaptFormToCheckConnection();
    }

    /**
     * Main Fields addControls.
     */
    protected void addFieldsListeners() {

        // Navigation : when the user select a table
        tableNavigator.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                String schemaLabel = tableNavigator.getSelection()[0].getText();
                metadataTable = TableHelper.findByLabel(getConnection(), schemaLabel);
                // initExistingNames();
                initMetadataForm();
                if (isReadOnly()) {
                    addTableButton.setEnabled(false);
                }
            }

        });

        // nameText : Event modifyText
        nameText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                String labelText = nameText.getText();
                changeTableNavigatorStatus(labelText);
                metadataTable.setLabel(labelText);
                if (tableNavigator.getSelection().length > 0) {
                    tableNavigator.getSelection()[0].setText(labelText);
                }
                changeTableNavigatorStatus(checkFieldsValue());
            }

        });
        // nameText : Event KeyListener
        nameText.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if ((!Character.isIdentifierIgnorable(e.character))
                        && (!Pattern.matches(RepositoryConstants.REPOSITORY_ITEM_PATTERN, "" + e.character))) { //$NON-NLS-1$
                    e.doit = false;
                }
            }
        });

        // commentText : Event modifyText
        commentText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                metadataTable.setComment(commentText.getText());
            }
        });

        // Event tableCombo
        tableCombo.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                updateRetreiveSchemaButton();
            }
        });

    }

    /**
     * Ensures that fields are set. Update checkEnable / use to checkTableSetting().
     */
    protected boolean checkFieldsValue() {

        updateRetreiveSchemaButton();

        if (!checkAllTablesIsCorrect()) {
            return false;
        }

        updateStatus(IStatus.OK, null);
        return true;
    }

    /**
     * DOC ocarbone Comment method "allTableHaveItems".
     * 
     * @return
     */
    private boolean checkAllTablesIsCorrect() {
        EList tables = getConnection().getTables();
        for (int i = 0; i < tables.size(); i++) {
            MetadataTable table = (MetadataTable) tables.get(i);

            String[] exisNames = TableHelper.getTableNames(getConnection(), table.getLabel());
            List existNames = existingNames == null ? Collections.EMPTY_LIST : Arrays.asList(exisNames);

            if (table.getLabel().equals("")) { //$NON-NLS-1$
                updateStatus(IStatus.ERROR, Messages.getString("DatabaseTableForm.nameAlert")); //$NON-NLS-1$
                return false;

                // Comment this condition because table name was allowed including illegal characters such as "&" or
                // "#".
                /**
                 * } else if (!Pattern.matches(RepositoryConstants.REPOSITORY_ITEM_PATTERN, table.getLabel())) {
                 * updateStatus(IStatus.ERROR, Messages.getString("DatabaseTableForm.nameAlertIllegalChar") + " \""
                 * //$NON-NLS-1$ //$NON-NLS-2$ + table.getLabel() + "\""); //$NON-NLS-1$ return false;
                 */
            } else if (existNames.contains(table.getLabel())) {
                updateStatus(IStatus.ERROR, Messages.getString("CommonWizard.nameAlreadyExist") + " \"" + table.getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                return false;
            }

            if (table.getColumns().size() == 0) {
                updateStatus(IStatus.ERROR, Messages.getString("FileStep3.itemAlert") + " \"" + table.getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                return false;
            }
        }
        return true;
    }

    private void updateRetreiveSchemaButton() {
        if (isReadOnly()) {
            retreiveSchemaButton.setEnabled(false);
        } else {
            retreiveSchemaButton.setEnabled(tableCombo.getSelectionIndex() >= 0);
        }
        streamDetachCheckbox.setEnabled(tableCombo.getSelectionIndex() >= 0);
        // manage infoLabel
        if (tableCombo.getItemCount() > 0) {
            if (tableCombo.getSelectionIndex() < 0) {
                tableSettingsInfoLabel.setText(Messages.getString("DatabaseTableForm.retreiveButtonAlert")); //$NON-NLS-1$
            } else if (tableEditorView.getMetadataEditor().getBeanCount() <= 0) {
                tableSettingsInfoLabel.setText(Messages.getString("DatabaseTableForm.retreiveButtonTip")); //$NON-NLS-1$
            } else {
                tableSettingsInfoLabel.setText(Messages.getString("DatabaseTableForm.retreiveButtonUse")); //$NON-NLS-1$
            }
        } else {
            tableSettingsInfoLabel.setText(""); //$NON-NLS-1$
        }
    }

    /**
     * RetreiveShema connection width value of nameText, serverText, loginText, passwordText, tableCombo.
     */
    private void pressRetreiveSchemaButton() {

        boolean checkConnectionIsDone = managerConnection.check(getIMetadataConnection());

        if (!checkConnectionIsDone) {
            adaptFormToCheckConnection();
            updateStatus(IStatus.WARNING, Messages.getString("DatabaseTableForm.connectionFailure")); //$NON-NLS-1$
            new ErrorDialogWidthDetailArea(getShell(), PID, Messages.getString("DatabaseTableForm.connectionFailure"), //$NON-NLS-1$
                    managerConnection.getMessageException());
        } else {
            boolean doit = true;
            if (tableEditorView.getMetadataEditor().getBeanCount() > 0) {
                doit = MessageDialog.openConfirm(getShell(), Messages.getString("DatabaseTableForm.retreiveButtonConfirmation"), //$NON-NLS-1$
                        Messages.getString("DatabaseTableForm.retreiveButtonConfirmationMessage")); //$NON-NLS-1$
            }
            if (doit) {
                tableString = tableCombo.getItem(tableCombo.getSelectionIndex());

                List<MetadataColumn> metadataColumns = new ArrayList<MetadataColumn>();
                metadataColumns = ExtractMetaDataFromDataBase.returnMetadataColumnsFormTable(iMetadataConnection, tableString);

                tableEditorView.getMetadataEditor().removeAll();

                List<MetadataColumn> metadataColumnsValid = new ArrayList<MetadataColumn>();
                Iterator iterate = metadataColumns.iterator();
                while (iterate.hasNext()) {
                    MetadataColumn metadataColumn = (MetadataColumn) iterate.next();

                    String columnLabel = metadataColumn.getLabel();
                    // Check the label and add it to the table
                    metadataColumn.setLabel(tableEditorView.getMetadataEditor().getNextGeneratedColumnName(columnLabel));
                    metadataColumnsValid.add(metadataColumn);
                }
                // see bug 3738
                removeDoubleQuotes(metadataColumnsValid);
                tableEditorView.getMetadataEditor().addAll(metadataColumnsValid);
            }
        }

        updateRetreiveSchemaButton();
        changeTableNavigatorStatus(checkFieldsValue());
    }

    // made by hyWang
    private void pressGuessSchemaButton() {
        IDesignerCoreService designerService = (IDesignerCoreService) GlobalServiceRegister.getDefault().getService(
                IDesignerCoreService.class);
        String tableName = tableCombo.getText();
        CsvArray array;
        try {
            array = designerService.convertNode(connectionItem, tableName);
            tableEditorView.getMetadataEditor().removeAll();

            List<MetadataColumn> columns = new ArrayList<MetadataColumn>();

            columns = GuessSchemaUtil.guessSchemaFromArray(array, true, tableEditorView, 5);

            List<String[]> schemaContent = array.getRows();
            int numbOfColumn = schemaContent.get(0).length;
            for (int i = 1; i <= numbOfColumn; i++) {
                MetadataColumn oneColum = columns.get(i - 1);
                // get the column name from the temp file genenrated by GuessSchemaProcess.java
                String labelName = (schemaContent.get(0))[i - 1];
                // oneColum.setLabel(labelName);
                if (!"".equals(labelName)) { //$NON-NLS-1$
                    oneColum.setOriginalField(labelName);
                }
                if (!"".equals(schemaContent.get(2)[i - 1])) { //$NON-NLS-1$
                    oneColum.setPrecision(Integer.parseInt(schemaContent.get(2)[i - 1]));
                }
                if (!"".equals(schemaContent.get(3)[i - 1])) { //$NON-NLS-1$
                    oneColum.setLength(Integer.parseInt(schemaContent.get(3)[i - 1]));
                }
                if (!"".equals(schemaContent.get(4)[i - 1])) { //$NON-NLS-1$
                    oneColum.setSourceType(schemaContent.get(4)[i - 1]);
                    String talendType = MetadataTalendType.getMappingTypeRetriever(tableEditorView.getCurrentDbms())
                            .getDefaultSelectedTalendType(schemaContent.get(4)[i - 1]);
                    oneColum.setTalendType(talendType);
                }
                // get if a column is nullable from the temp file genenrated by
                // GuessSchemaProcess.java
                if (!"".equals(schemaContent.get(1)[i - 1])) { //$NON-NLS-1$
                    oneColum.setNullable((schemaContent.get(1))[i - 1].equals(Boolean.TRUE.toString()) ? true : false);
                }
                // String talendType = null;
                // // to see if the language is java or perl
                // try {
                // if (LanguageManager.getCurrentLanguage() == ECodeLanguage.JAVA) {
                // talendType = JavaDataTypeHelper.getTalendTypeOfValue(schemaContent.get(5)[i - 1]);
                // } else {
                // talendType = PerlDataTypeHelper.getNewTalendTypeOfValue(schemaContent.get(5)[i - 1]);
                // }
                // oneColum.setTalendType(talendType);
                // columns.add((MetadataColumn) oneColum);
                // } catch (Exception e) {
                // /*
                // * the table have no data at all ,to do nothing
                // */
                // }
            }

            tableEditorView.getMetadataEditor().addAll(columns);
        } catch (ProcessorException e) {
            ExceptionHandler.process(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.AbstractForm#adaptFormToReadOnly()
     */
    protected void adaptFormToReadOnly() {
        readOnly = isReadOnly();

        nameText.setReadOnly(isReadOnly());
        commentText.setReadOnly(isReadOnly());
        tableEditorView.setReadOnly(isReadOnly());
        addTableButton.setEnabled(!isReadOnly());
        retreiveSchemaButton.setEnabled(!isReadOnly());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Control#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            initializeForm();
            checkConnection(false);
        }
        if (isReadOnly() != readOnly) {
            adaptFormToReadOnly();
        }
    }

    protected DatabaseConnection getConnection() {
        return (DatabaseConnection) connectionItem.getConnection();
    }

    /**
     * 
     * for featrue 2449
     */

    public IMetadataConnection getIMetadataConnection() {
        return this.iMetadataConnection;
    }

    public void setIMetadataConnection(IMetadataConnection metadataConnection) {
        this.iMetadataConnection = metadataConnection;
    }

    /**
     * Comment method "changeTableNavigatorStatus".
     * 
     * @param schemaLabel
     */
    private void changeTableNavigatorStatus(String schemaLabel) {
        Composite leftGroup = tableNavigator.getParent().getParent().getParent();
        Control[] children = leftGroup.getChildren();
        if (schemaLabel == null || schemaLabel.length() == 0) {
            leftGroup.setEnabled(false);
            changeControlStatus(children, false);
        } else {
            leftGroup.setEnabled(true);
            changeControlStatus(children, true);
        }
    }

    /**
     * DOC Administrator Comment method "changeControlStatus".
     * 
     * @param children
     * @param status
     */
    private void changeControlStatus(Control[] children, boolean status) {
        for (Control control : children) {
            control.setEnabled(status);
            if (control instanceof Composite) {
                Control[] subChildren = ((Composite) control).getChildren();
                changeControlStatus(subChildren, status);
            }
        }
    }

    /**
     * Comment method "changeTableNavigatorStatus".
     * 
     * @param isEnabled
     */
    private void changeTableNavigatorStatus(boolean isEnabled) {
        Composite leftGroup = tableNavigator.getParent().getParent().getParent();
        Control[] children = leftGroup.getChildren();
        leftGroup.setEnabled(isEnabled);
        changeControlStatus(children, isEnabled);
    }

}
