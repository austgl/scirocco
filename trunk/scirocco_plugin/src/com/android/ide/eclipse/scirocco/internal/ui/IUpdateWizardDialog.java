/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.scirocco.internal.ui;

import org.eclipse.jface.wizard.WizardDialog;

import com.android.ide.eclipse.scirocco.internal.ui.WizardDialogEx;


/**
 * An interface that enables a client to update {@link WizardDialog} after its creation.
 */
public interface IUpdateWizardDialog {
    /**
     * Invoked after {@link WizardDialog#create()} to let the caller update the dialog.
     */
    public void updateWizardDialog(WizardDialogEx dialog);
}
