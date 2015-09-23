// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.component.ui.wizard.ui;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.talend.component.core.utils.ComponentsUtils;
import org.talend.component.ui.wizard.model.FakeElement;
import org.talend.components.api.properties.presentation.Form;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.designer.core.ui.views.properties.MultipleThreadDynamicComposite;

/**
 * 
 * created by ycbai on 2015年9月21日 Detailled comment
 *
 */
public class GenericConnWizardPage extends WizardPage {

    private final ConnectionItem connectionItem;

    private final String[] existingNames;

    private final boolean isRepositoryObjectEditable;

    private Form form;

    public GenericConnWizardPage(ConnectionItem connectionItem, boolean isRepositoryObjectEditable, String[] existingNames,
            boolean creation, Form form) {
        super("GenericConnWizardPage"); //$NON-NLS-1$
        this.connectionItem = connectionItem;
        this.existingNames = existingNames;
        this.isRepositoryObjectEditable = isRepositoryObjectEditable;
        this.form = form;
    }

    @Override
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        container.setLayout(new FormLayout());
        setControl(container);

        Element element = new FakeElement(form.getName());
        element.setElementParameters(ComponentsUtils.getParametersFromForm(element, null, form, null));
        MultipleThreadDynamicComposite dynamicComposite = new MultipleThreadDynamicComposite(container, SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.NO_FOCUS, EComponentCategory.BASIC, element, true, container.getBackground());
        dynamicComposite.setLayoutData(createFormData());
    }

    protected FormData createFormData() {
        FormData data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        return data;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        setPageComplete(visible);
    }

}
