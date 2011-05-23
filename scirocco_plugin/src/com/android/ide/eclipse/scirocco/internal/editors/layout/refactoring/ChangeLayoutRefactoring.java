/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.ide.eclipse.scirocco.internal.editors.layout.refactoring;

import static com.android.ide.common.layout.LayoutConstants.ANDROID_URI;
import static com.android.ide.common.layout.LayoutConstants.ANDROID_WIDGET_PREFIX;
import static com.android.ide.common.layout.LayoutConstants.ATTR_BASELINE_ALIGNED;
import static com.android.ide.common.layout.LayoutConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.ide.common.layout.LayoutConstants.ATTR_LAYOUT_BELOW;
import static com.android.ide.common.layout.LayoutConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.ide.common.layout.LayoutConstants.ATTR_ORIENTATION;
import static com.android.ide.common.layout.LayoutConstants.FQCN_GESTURE_OVERLAY_VIEW;
import static com.android.ide.common.layout.LayoutConstants.FQCN_LINEAR_LAYOUT;
import static com.android.ide.common.layout.LayoutConstants.FQCN_RELATIVE_LAYOUT;
import static com.android.ide.common.layout.LayoutConstants.FQCN_TABLE_LAYOUT;
import static com.android.ide.common.layout.LayoutConstants.GESTURE_OVERLAY_VIEW;
import static com.android.ide.common.layout.LayoutConstants.LINEAR_LAYOUT;
import static com.android.ide.common.layout.LayoutConstants.TABLE_ROW;
import static com.android.ide.common.layout.LayoutConstants.VALUE_FALSE;
import static com.android.ide.common.layout.LayoutConstants.VALUE_VERTICAL;
import static com.android.ide.eclipse.scirocco.AdtConstants.EXT_XML;

import com.android.annotations.VisibleForTesting;
import com.android.ide.eclipse.scirocco.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.scirocco.internal.editors.layout.LayoutEditor;
import com.android.ide.eclipse.scirocco.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.scirocco.internal.editors.layout.gle2.CanvasViewInfo;
import com.android.ide.eclipse.scirocco.internal.editors.layout.gle2.LayoutCanvas;
import com.android.ide.eclipse.scirocco.internal.editors.layout.gle2.ViewHierarchy;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the selected layout into a layout of a different type.
 */
@SuppressWarnings("restriction") // XML model
public class ChangeLayoutRefactoring extends VisualRefactoring {
    private static final String KEY_TYPE = "type";       //$NON-NLS-1$
    private static final String KEY_FLATTEN = "flatten"; //$NON-NLS-1$

    private String mTypeFqcn;
    private boolean mFlatten;

    /**
     * This constructor is solely used by {@link Descriptor},
     * to replay a previous refactoring.
     * @param arguments argument map created by #createArgumentMap.
     */
    ChangeLayoutRefactoring(Map<String, String> arguments) {
        super(arguments);
        mTypeFqcn = arguments.get(KEY_TYPE);
        mFlatten = Boolean.parseBoolean(arguments.get(KEY_FLATTEN));
    }

    @VisibleForTesting
    ChangeLayoutRefactoring(List<Element> selectedElements, LayoutEditor editor) {
        super(selectedElements, editor);
    }

    public ChangeLayoutRefactoring(IFile file, LayoutEditor editor, ITextSelection selection,
            ITreeSelection treeSelection) {
        super(file, editor, selection, treeSelection);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        try {
            pm.beginTask("Checking preconditions...", 2);

            if (mSelectionStart == -1 || mSelectionEnd == -1) {
                status.addFatalError("No selection to convert");
                return status;
            }

            if (mElements.size() != 1) {
                status.addFatalError("Select precisely one layout to convert");
                return status;
            }

            pm.worked(1);
            return status;

        } finally {
            pm.done();
        }
    }

    @Override
    protected VisualRefactoringDescriptor createDescriptor() {
        String comment = getName();
        return new Descriptor(
                mProject.getName(), //project
                comment, //description
                comment, //comment
                createArgumentMap());
    }

    @Override
    protected Map<String, String> createArgumentMap() {
        Map<String, String> args = super.createArgumentMap();
        args.put(KEY_TYPE, mTypeFqcn);
        args.put(KEY_FLATTEN, Boolean.toString(mFlatten));

        return args;
    }

    @Override
    public String getName() {
        return "Change Layout";
    }

    void setType(String typeFqcn) {
        mTypeFqcn = typeFqcn;
    }

    void setFlatten(boolean flatten) {
        mFlatten = flatten;
    }

    @Override
    protected List<Element> initElements() {
        List<Element> elements = super.initElements();

        // Don't convert a root GestureOverlayView; convert its child. This looks for
        // gesture overlays, and if found, it generates a new child list where the gesture
        // overlay children are replaced by their first element children
        for (Element element : elements) {
            String tagName = element.getTagName();
            if (tagName.equals(GESTURE_OVERLAY_VIEW)
                    || tagName.equals(FQCN_GESTURE_OVERLAY_VIEW)) {
                List<Element> replacement = new ArrayList<Element>(elements.size());
                for (Element e : elements) {
                    tagName = e.getTagName();
                    if (tagName.equals(GESTURE_OVERLAY_VIEW)
                            || tagName.equals(FQCN_GESTURE_OVERLAY_VIEW)) {
                        NodeList children = e.getChildNodes();
                        Element first = null;
                        for (int i = 0, n = children.getLength(); i < n; i++) {
                            Node node = children.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                first = (Element) node;
                                break;
                            }
                        }
                        if (first != null) {
                            e = first;
                        }
                    }
                    replacement.add(e);
                }
                return replacement;
            }
        }

        return elements;
    }

    @Override
    protected List<Change> computeChanges() {
        String name = getViewClass(mTypeFqcn);

        IFile file = mEditor.getInputFile();
        List<Change> changes = new ArrayList<Change>();
        TextFileChange change = new TextFileChange(file.getName(), file);
        MultiTextEdit rootEdit = new MultiTextEdit();
        change.setEdit(rootEdit);
        change.setTextType(EXT_XML);
        changes.add(change);

        String text = getText(mSelectionStart, mSelectionEnd);
        Element layout = getPrimaryElement();
        String oldName = layout.getNodeName();
        int open = text.indexOf(oldName);
        int close = text.lastIndexOf(oldName);

        if (open != -1 && close != -1) {
            int oldLength = oldName.length();
            rootEdit.addChild(new ReplaceEdit(mSelectionStart + open, oldLength, name));
            if (close != open) { // Gracefully handle <FooLayout/>
                rootEdit.addChild(new ReplaceEdit(mSelectionStart + close, oldLength, name));
            }
        }

        String oldId = getId(layout);
        String newId = ensureIdMatchesType(layout, mTypeFqcn, rootEdit);
        // Update any layout references to the old id with the new id
        if (oldId != null && newId != null) {
            IStructuredModel model = mEditor.getModelForRead();
            try {
                IStructuredDocument doc = model.getStructuredDocument();
                if (doc != null) {
                    List<TextEdit> replaceIds = replaceIds(getAndroidNamespacePrefix(), doc,
                            mSelectionStart,
                            mSelectionEnd, oldId, newId);
                    for (TextEdit edit : replaceIds) {
                        rootEdit.addChild(edit);
                    }
                }
            } finally {
                model.releaseFromRead();
            }
        }

        String oldType = getOldType();
        String newType = mTypeFqcn;
        if (newType.equals(FQCN_RELATIVE_LAYOUT)) {
            if (oldType.equals(FQCN_LINEAR_LAYOUT) && !mFlatten) {
                // Hand-coded conversion specifically tailored for linear to relative, provided
                // there is no hierarchy flattening
                // TODO: use the RelativeLayoutConversionHelper for this; it does a better job
                // analyzing gravities etc.
                convertLinearToRelative(rootEdit);
                removeUndefinedLayoutAttrs(rootEdit, layout);
            } else {
                // Generic conversion to relative - can also flatten the hierarchy
                convertAnyToRelative(rootEdit);
                // This already handles removing undefined layout attributes -- right?
                //removeUndefinedLayoutAttrs(rootEdit, layout);
            }
        } else if (oldType.equals(FQCN_RELATIVE_LAYOUT) && newType.equals(FQCN_LINEAR_LAYOUT)) {
            convertRelativeToLinear(rootEdit);
            removeUndefinedLayoutAttrs(rootEdit, layout);
        } else if (oldType.equals(FQCN_LINEAR_LAYOUT) && newType.equals(FQCN_TABLE_LAYOUT)) {
            convertLinearToTable(rootEdit);
            removeUndefinedLayoutAttrs(rootEdit, layout);
        } else {
            convertGeneric(rootEdit, oldType, newType);
            removeUndefinedLayoutAttrs(rootEdit, layout);
        }

        return changes;
    }

    /** Hand coded conversion from a LinearLayout to a TableLayout */
    private void convertLinearToTable(MultiTextEdit rootEdit) {
        // This is pretty easy; just switch the root tag (already done by the initial generic
        // conversion) and then convert all the children into <TableRow> elements.
        // Finally, get rid of the orientation attribute, if any.
        Element layout = getPrimaryElement();
        removeOrientationAttribute(rootEdit, layout);

        NodeList children = layout.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                if (node instanceof IndexedRegion) {
                    IndexedRegion region = (IndexedRegion) node;
                    int start = region.getStartOffset();
                    int end = region.getEndOffset();
                    String text = getText(start, end);
                    String oldName = child.getNodeName();
                    if (oldName.equals(LINEAR_LAYOUT)) {
                        removeOrientationAttribute(rootEdit, child);
                        int open = text.indexOf(oldName);
                        int close = text.lastIndexOf(oldName);

                        if (open != -1 && close != -1) {
                            int oldLength = oldName.length();
                            rootEdit.addChild(new ReplaceEdit(mSelectionStart + open, oldLength,
                                    TABLE_ROW));
                            if (close != open) { // Gracefully handle <FooLayout/>
                                rootEdit.addChild(new ReplaceEdit(mSelectionStart + close,
                                        oldLength, TABLE_ROW));
                            }
                        }
                    } // else: WRAP in TableLayout!
                }
            }
        }
    }

     /** Hand coded conversion from a LinearLayout to a RelativeLayout */
    private void convertLinearToRelative(MultiTextEdit rootEdit) {
        // This can be done accurately.
        Element layout = getPrimaryElement();
        // Horizontal is the default, so if no value is specified it is horizontal.
        boolean isVertical = VALUE_VERTICAL.equals(layout.getAttributeNS(ANDROID_URI,
                ATTR_ORIENTATION));

        removeOrientationAttribute(rootEdit, layout);

        String attributePrefix = getAndroidNamespacePrefix();

        // TODO: Consider gravity of each element
        // TODO: Consider weight of each element
        // Right now it simply makes a single attachment to keep the order.

        if (isVertical) {
            // Align each child to the bottom and left of its parent
            NodeList children = layout.getChildNodes();
            String prevId = null;
            for (int i = 0, n = children.getLength(); i < n; i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) node;
                    String id = ensureHasId(rootEdit, child, null);
                    if (prevId != null) {
                        setAttribute(rootEdit, child, ANDROID_URI, attributePrefix,
                                ATTR_LAYOUT_BELOW, prevId);
                    }
                    prevId = id;
                }
            }
        } else {
            // Align each child to the left
            NodeList children = layout.getChildNodes();
            boolean isBaselineAligned =
                !VALUE_FALSE.equals(layout.getAttributeNS(ANDROID_URI, ATTR_BASELINE_ALIGNED));

            String prevId = null;
            for (int i = 0, n = children.getLength(); i < n; i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) node;
                    String id = ensureHasId(rootEdit, child, null);
                    if (prevId != null) {
                        setAttribute(rootEdit, child, ANDROID_URI, attributePrefix,
                                ATTR_LAYOUT_TO_RIGHT_OF, prevId);
                        if (isBaselineAligned) {
                            setAttribute(rootEdit, child, ANDROID_URI, attributePrefix,
                                    ATTR_LAYOUT_ALIGN_BASELINE, prevId);
                        }
                    }
                    prevId = id;
                }
            }
        }
    }

    /** Strips out the android:orientation attribute from the given linear layout element */
    private void removeOrientationAttribute(MultiTextEdit rootEdit, Element layout) {
        assert layout.getTagName().equals(LINEAR_LAYOUT);
        removeAttribute(rootEdit, layout, ANDROID_URI, ATTR_ORIENTATION);
    }

    /**
     * Hand coded conversion from a RelativeLayout to a LinearLayout
     *
     * @param rootEdit the root multi text edit to add edits to
     */
    private void convertRelativeToLinear(MultiTextEdit rootEdit) {
        // This is going to be lossy...
        // TODO: Attempt to "order" the items based on their visual positions
        // and insert them in that order in the LinearLayout.
        // TODO: Possibly use nesting if necessary, by spatial subdivision,
        // to accomplish roughly the same layout as the relative layout specifies.
    }

    /**
     * Hand coded -generic- conversion from one layout to another. This is not going to be
     * an accurate layout transformation; instead it simply migrates the layout attributes
     * that are supported, and adds defaults for any new required layout attributes. In
     * addition, it attempts to order the children visually based on where they fit in a
     * rendering. (Unsupported layout attributes will be removed by the caller at the
     * end.)
     * <ul>
     * <li>Try to handle nesting. Converting a *hierarchy* of layouts into a flatter
     * layout for powerful layouts that support it, like RelativeLayout.
     * <li>Try to do automatic "inference" about the layout. I can render it and look at
     * the ViewInfo positions and sizes. I can render it multiple times, at different
     * sizes, to infer "stretchiness" and "weight" properties of the children.
     * <li>Try to do indirect transformations. E.g. if I can go from A to B, and B to C,
     * then an attempt to go from A to C should perform conversions A to B and then B to
     * C.
     * </ul>
     *
     * @param rootEdit the root multi text edit to add edits to
     * @param oldType the fully qualified class name of the layout type we are converting
     *            from
     * @param newType the fully qualified class name of the layout type we are converting
     *            to
     */
    private void convertGeneric(MultiTextEdit rootEdit, String oldType, String newType) {
        // TODO: Add hooks for 3rd party conversions getting registered through the
        // IViewRule interface.

        // For now we simply go with the default behavior, which is to just strip the
        // layout attributes that aren't supported.
    }

    /** Removes all the unused attributes after a conversion */
    private void removeUndefinedLayoutAttrs(MultiTextEdit rootEdit, Element layout) {
        ViewElementDescriptor descriptor = getElementDescriptor(mTypeFqcn);
        if (descriptor == null) {
            return;
        }

        Set<String> defined = new HashSet<String>();
        AttributeDescriptor[] layoutAttributes = descriptor.getLayoutAttributes();
        for (AttributeDescriptor attribute : layoutAttributes) {
            defined.add(attribute.getXmlLocalName());
        }

        NodeList children = layout.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;

                List<Attr> attributes = findLayoutAttributes(child);
                for (Attr attribute : attributes) {
                    String name = attribute.getLocalName();
                    if (!defined.contains(name)) {
                        // Remove it
                        removeAttribute(rootEdit, child, attribute.getNamespaceURI(), name);
                    }
                }
            }
        }
    }

    /** Hand coded conversion from any layout to a RelativeLayout */
    private void convertAnyToRelative(MultiTextEdit rootEdit) {
        // To perform a conversion from any other layout type, including nested conversion,
        Element layout = getPrimaryElement();
        CanvasViewInfo rootView = mRootView;
        if (rootView == null) {
            LayoutCanvas canvas = mEditor.getGraphicalEditor().getCanvasControl();
            ViewHierarchy viewHierarchy = canvas.getViewHierarchy();
            rootView = viewHierarchy.getRoot();
        }

        RelativeLayoutConversionHelper helper =
            new RelativeLayoutConversionHelper(this, layout, mFlatten, rootEdit, rootView);
        helper.convertToRelative();
    }

    public static class Descriptor extends VisualRefactoringDescriptor {
        public Descriptor(String project, String description, String comment,
                Map<String, String> arguments) {
            super("com.android.ide.eclipse.scirocco.refactoring.convert", //$NON-NLS-1$
                    project, description, comment, arguments);
        }

        @Override
        protected Refactoring createRefactoring(Map<String, String> args) {
            return new ChangeLayoutRefactoring(args);
        }
    }

    String getOldType() {
        Element primary = getPrimaryElement();
        if (primary != null) {
            String oldType = primary.getTagName();
            if (oldType.indexOf('.') == -1) {
                oldType = ANDROID_WIDGET_PREFIX + oldType;
            }
            return oldType;
        }

        return null;
    }

    @VisibleForTesting
    protected CanvasViewInfo mRootView;

    @VisibleForTesting
    public void setRootView(CanvasViewInfo rootView) {
        mRootView = rootView;
    }

    @Override
    VisualRefactoringWizard createWizard() {
        return new ChangeLayoutWizard(this, mEditor);
    }
}
