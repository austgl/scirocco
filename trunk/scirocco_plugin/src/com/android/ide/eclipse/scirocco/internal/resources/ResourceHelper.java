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

package com.android.ide.eclipse.scirocco.internal.resources;

import static com.android.AndroidConstants.FD_RES_VALUES;
import static com.android.ide.eclipse.scirocco.AdtConstants.ANDROID_PKG;
import static com.android.ide.eclipse.scirocco.AdtConstants.EXT_XML;
import static com.android.ide.eclipse.scirocco.AdtConstants.WS_SEP;
import static com.android.ide.eclipse.scirocco.internal.editors.resources.descriptors.ResourcesDescriptors.NAME_ATTR;
import static com.android.sdklib.SdkConstants.FD_RESOURCES;

import com.android.ide.common.resources.ResourceDeltaKind;
import com.android.ide.common.resources.configuration.CountryCodeQualifier;
import com.android.ide.common.resources.configuration.DockModeQualifier;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.KeyboardStateQualifier;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.NavigationMethodQualifier;
import com.android.ide.common.resources.configuration.NavigationStateQualifier;
import com.android.ide.common.resources.configuration.NetworkCodeQualifier;
import com.android.ide.common.resources.configuration.NightModeQualifier;
import com.android.ide.common.resources.configuration.PixelDensityQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.ide.common.resources.configuration.ScreenDimensionQualifier;
import com.android.ide.common.resources.configuration.ScreenOrientationQualifier;
import com.android.ide.common.resources.configuration.ScreenRatioQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.ide.common.resources.configuration.TextInputMethodQualifier;
import com.android.ide.common.resources.configuration.TouchScreenQualifier;
import com.android.ide.common.resources.configuration.VersionQualifier;
import com.android.ide.eclipse.scirocco.AdtPlugin;
import com.android.ide.eclipse.scirocco.internal.editors.AndroidXmlEditor;
import com.android.ide.eclipse.scirocco.internal.editors.IconFactory;
import com.android.ide.eclipse.scirocco.internal.editors.layout.refactoring.VisualRefactoring;
import com.android.ide.eclipse.scirocco.internal.editors.resources.descriptors.ResourcesDescriptors;
import com.android.ide.eclipse.scirocco.internal.editors.xml.Hyperlinks;
import com.android.ide.eclipse.scirocco.internal.wizards.newxmlfile.NewXmlFileWizard;
import com.android.resources.FolderTypeRelationship;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.util.Pair;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to deal with SWT specifics for the resources.
 */
@SuppressWarnings("restriction") // XML model
public class ResourceHelper {

    private final static Map<Class<?>, Image> sIconMap = new HashMap<Class<?>, Image>(
            FolderConfiguration.getQualifierCount());

    static {
        IconFactory factory = IconFactory.getInstance();
        sIconMap.put(CountryCodeQualifier.class,       factory.getIcon("mcc")); //$NON-NLS-1$
        sIconMap.put(NetworkCodeQualifier.class,       factory.getIcon("mnc")); //$NON-NLS-1$
        sIconMap.put(LanguageQualifier.class,          factory.getIcon("language")); //$NON-NLS-1$
        sIconMap.put(RegionQualifier.class,            factory.getIcon("region")); //$NON-NLS-1$
        sIconMap.put(ScreenSizeQualifier.class,        factory.getIcon("size")); //$NON-NLS-1$
        sIconMap.put(ScreenRatioQualifier.class,       factory.getIcon("ratio")); //$NON-NLS-1$
        sIconMap.put(ScreenOrientationQualifier.class, factory.getIcon("orientation")); //$NON-NLS-1$
        sIconMap.put(DockModeQualifier.class,          factory.getIcon("dockmode")); //$NON-NLS-1$
        sIconMap.put(NightModeQualifier.class,         factory.getIcon("nightmode")); //$NON-NLS-1$
        sIconMap.put(PixelDensityQualifier.class,      factory.getIcon("dpi")); //$NON-NLS-1$
        sIconMap.put(TouchScreenQualifier.class,       factory.getIcon("touch")); //$NON-NLS-1$
        sIconMap.put(KeyboardStateQualifier.class,     factory.getIcon("keyboard")); //$NON-NLS-1$
        sIconMap.put(TextInputMethodQualifier.class,   factory.getIcon("text_input")); //$NON-NLS-1$
        sIconMap.put(NavigationStateQualifier.class,   factory.getIcon("navpad")); //$NON-NLS-1$
        sIconMap.put(NavigationMethodQualifier.class,  factory.getIcon("navpad")); //$NON-NLS-1$
        sIconMap.put(ScreenDimensionQualifier.class,   factory.getIcon("dimension")); //$NON-NLS-1$
        sIconMap.put(VersionQualifier.class,           factory.getIcon("version")); //$NON-NLS-1$
    }

    /**
     * Returns the icon for the qualifier.
     */
    public static Image getIcon(Class<? extends ResourceQualifier> theClass) {
        return sIconMap.get(theClass);
    }

    /**
     * Returns a {@link ResourceDeltaKind} from an {@link IResourceDelta} value.
     * @param kind a {@link IResourceDelta} integer constant.
     * @return a matching {@link ResourceDeltaKind} or null.
     *
     * @see IResourceDelta#ADDED
     * @see IResourceDelta#REMOVED
     * @see IResourceDelta#CHANGED
     */
    public static ResourceDeltaKind getResourceDeltaKind(int kind) {
        switch (kind) {
            case IResourceDelta.ADDED:
                return ResourceDeltaKind.ADDED;
            case IResourceDelta.REMOVED:
                return ResourceDeltaKind.REMOVED;
            case IResourceDelta.CHANGED:
                return ResourceDeltaKind.CHANGED;
        }

        return null;
    }

    /**
     * Return the resource type of the given url, and the resource name
     *
     * @param url the resource url to be parsed
     * @return a pair of the resource type and the resource name
     */
    public static Pair<ResourceType,String> parseResource(String url) {
        if (!url.startsWith("@")) { //$NON-NLS-1$
            return null;
        }
        int typeEnd = url.indexOf('/', 1);
        if (typeEnd == -1) {
            return null;
        }
        int nameBegin = typeEnd + 1;

        // Skip @ and @+
        int typeBegin = url.startsWith("@+") ? 2 : 1; //$NON-NLS-1$

        int colon = url.lastIndexOf(':', typeEnd);
        if (colon != -1) {
            typeBegin = colon + 1;
        }
        String typeName = url.substring(typeBegin, typeEnd);
        ResourceType type = ResourceType.getEnum(typeName);
        if (type == null) {
            return null;
        }
        String name = url.substring(nameBegin);

        return Pair.of(type, name);
    }

    /**
     * Is this a resource that can be defined in any file within the "values" folder?
     * <p>
     * Some resource types can be defined <b>both</b> as a separate XML file as well
     * as defined within a value XML file. This method will return true for these types
     * as well. In other words, a ResourceType can return true for both
     * {@link #isValueBasedResourceType} and {@link #isFileBasedResourceType}.
     *
     * @param type the resource type to check
     * @return true if the given resource type can be represented as a value under the
     *         values/ folder
     */
    public static boolean isValueBasedResourceType(ResourceType type) {
        List<ResourceFolderType> folderTypes = FolderTypeRelationship.getRelatedFolders(type);
        for (ResourceFolderType folderType : folderTypes) {
            if (folderType == ResourceFolderType.VALUES) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is this a resource that is defined in a file named by the resource plus the XML
     * extension?
     * <p>
     * Some resource types can be defined <b>both</b> as a separate XML file as well as
     * defined within a value XML file along with other properties. This method will
     * return true for these resource types as well. In other words, a ResourceType can
     * return true for both {@link #isValueBasedResourceType} and
     * {@link #isFileBasedResourceType}.
     *
     * @param type the resource type to check
     * @return true if the given resource type is stored in a file named by the resource
     */
    public static boolean isFileBasedResourceType(ResourceType type) {
        List<ResourceFolderType> folderTypes = FolderTypeRelationship.getRelatedFolders(type);
        for (ResourceFolderType folderType : folderTypes) {
            if (folderType != ResourceFolderType.VALUES) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this class can create the given resource
     *
     * @param resource the resource to be created
     * @return true if the {@link #createResource} method can create this resource
     */
    public static boolean canCreateResource(String resource) {
        // Cannot create framework resources
        if (resource.startsWith('@' + ANDROID_PKG + ':')) {
            return false;
        }

        Pair<ResourceType,String> parsed = parseResource(resource);
        if (parsed != null) {
            ResourceType type = parsed.getFirst();
            String name = parsed.getSecond();

            // Make sure the name is valid
            ResourceNameValidator validator =
                ResourceNameValidator.create(false, (Set<String>) null /* existing */, type);
            if (validator.isValid(name) != null) {
                return false;
            }

            // We can create all value types
            if (isValueBasedResourceType(type)) {
                return true;
            }

            // We can create -some- file-based types - those supported by the New XML wizard:
            for (ResourceFolderType folderType : FolderTypeRelationship.getRelatedFolders(type)) {
                if (NewXmlFileWizard.canCreateXmlFile(folderType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Creates a file-based resource, like a layout. Used by {@link #createResource} */
    private static Pair<IFile,IRegion> createFileResource(IProject project, ResourceType type,
            String name) {

        ResourceFolderType folderType = null;
        for (ResourceFolderType f : FolderTypeRelationship.getRelatedFolders(type)) {
            if (NewXmlFileWizard.canCreateXmlFile(f)) {
                folderType = f;
                break;
            }
        }
        if (folderType == null) {
            return null;
        }

        // Find "dimens.xml" file in res/values/ (or corresponding name for other
        // value types)
        IPath projectPath = new Path(FD_RESOURCES + WS_SEP + folderType.getName() + WS_SEP
            + name + '.' + EXT_XML);
        IFile file = project.getFile(projectPath);
        return NewXmlFileWizard.createXmlFile(project, file, folderType);
    }

    /**
     * Creates a resource of a given type, name and (if applicable) value
     *
     * @param project the project to contain the resource
     * @param type the type of resource
     * @param name the name of the resource
     * @param value the value of the resource, if it is a value-type resource
     * @return a pair of the file containing the resource and a region where the value
     *         appears
     */
    public static Pair<IFile,IRegion> createResource(IProject project, ResourceType type,
            String name, String value) {
        if (!isValueBasedResourceType(type)) {
            return createFileResource(project, type, name);
        }

        // Find "dimens.xml" file in res/values/ (or corresponding name for other
        // value types)
        String fileName = type.getName() + 's';
        String projectPath = FD_RESOURCES + WS_SEP + FD_RES_VALUES + WS_SEP
            + fileName + '.' + EXT_XML;
        Object editRequester = project;
        IResource member = project.findMember(projectPath);
        if (member != null) {
            if (member instanceof IFile) {
                IFile file = (IFile) member;
                // File exists: Must add item to the XML
                IModelManager manager = StructuredModelManager.getModelManager();
                IStructuredModel model = null;
                try {
                    model = manager.getExistingModelForEdit(file);
                    if (model == null) {
                        model = manager.getModelForEdit(file);
                    }
                    if (model instanceof IDOMModel) {
                        model.beginRecording(editRequester, String.format("Add %1$s",
                                type.getDisplayName()));
                        IDOMModel domModel = (IDOMModel) model;
                        Document document = domModel.getDocument();
                        Element root = document.getDocumentElement();
                        IStructuredDocument structuredDocument = model.getStructuredDocument();
                        Node lastElement = null;
                        NodeList childNodes = root.getChildNodes();
                        String indent = null;
                        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
                            Node node = childNodes.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                lastElement = node;
                                indent = AndroidXmlEditor.getIndent(structuredDocument, node);
                                break;
                            }
                        }
                        if (indent == null || indent.length() == 0) {
                            indent = "    "; //$NON-NLS-1$
                        }
                        Node nextChild = lastElement != null ? lastElement.getNextSibling() : null;
                        Text indentNode = document.createTextNode('\n' + indent);
                        root.insertBefore(indentNode, nextChild);
                        Element element = document.createElement(Hyperlinks.getTagName(type));
                        element.setAttribute(NAME_ATTR, name);
                        root.insertBefore(element, nextChild);
                        Text valueNode = document.createTextNode(value);
                        element.appendChild(valueNode);
                        model.save();
                        IndexedRegion domRegion = VisualRefactoring.getRegion(valueNode);
                        int startOffset = domRegion.getStartOffset();
                        int length = domRegion.getLength();
                        IRegion region = new Region(startOffset, length);
                        return Pair.of(file, region);
                    }
                } catch (Exception e) {
                    AdtPlugin.log(e, "Cannot access XML value model");
                } finally {
                    if (model != null) {
                        model.endRecording(editRequester);
                        model.releaseFromEdit();
                    }
                }
            }

            return null;
        } else {
            // No such file exists: just create it
            String prolog = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"; //$NON-NLS-1$
            StringBuilder sb = new StringBuilder(prolog);

            String root = ResourcesDescriptors.ROOT_ELEMENT;
            sb.append('<').append(root).append('>').append('\n');
            sb.append("    "); //$NON-NLS-1$
            sb.append('<');
            sb.append(type.getName());
            sb.append(" name=\""); //$NON-NLS-1$
            sb.append(name);
            sb.append('"');
            sb.append('>');
            int start = sb.length();
            sb.append(value);
            int end = sb.length();
            sb.append('<').append('/');
            sb.append(type.getName());
            sb.append(">\n");                            //$NON-NLS-1$
            sb.append('<').append('/').append(root).append('>').append('\n');
            String result = sb.toString();
            String error = null;
            try {
                byte[] buf = result.getBytes("UTF8");    //$NON-NLS-1$
                InputStream stream = new ByteArrayInputStream(buf);
                IFile file = project.getFile(new Path(projectPath));
                file.create(stream, true /*force*/, null /*progress*/);
                IRegion region = new Region(start, end - start);
                return Pair.of(file, region);
            } catch (UnsupportedEncodingException e) {
                error = e.getMessage();
            } catch (CoreException e) {
                error = e.getMessage();
            }

            error = String.format("Failed to generate %1$s: %2$s", name, error);
            AdtPlugin.displayError("New Android XML File", error);
        }
        return null;
    }
}
