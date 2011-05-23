/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.ide.common.layout;

import static com.android.ide.common.layout.LayoutConstants.ANDROID_URI;
import static com.android.ide.common.layout.LayoutConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.ide.common.layout.LayoutConstants.ATTR_LAYOUT_WIDTH;
import static com.android.ide.common.layout.LayoutConstants.FQCN_LINEAR_LAYOUT;

import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for android.widget.ScrollView.
 */
public class ScrollViewRule extends FrameLayoutRule {

    @Override
    public void onChildInserted(INode child, INode parent, InsertType insertType) {
        super.onChildInserted(child, parent, insertType);

        // The child of the ScrollView should fill in both directions
        String fillParent = getFillParentValueName();
        child.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, fillParent);
        child.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, fillParent);
    }

    @Override
    public void onCreate(INode node, INode parent, InsertType insertType) {
        super.onCreate(node, parent, insertType);

        if (insertType.isCreate()) {
            // Insert a default linear layout (which will in turn be registered as
            // a child of this node and the create child method above will set its
            // fill parent attributes, its id, etc.
            node.appendChild(FQCN_LINEAR_LAYOUT);
        }
    }

}
