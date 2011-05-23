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

package com.android.ide.eclipse.scirocco.internal.editors.layout.gre;

import static com.android.ide.common.layout.LayoutConstants.ANDROID_URI;
import static com.android.ide.common.layout.LayoutConstants.ATTR_ID;
import static com.android.ide.common.layout.LayoutConstants.ID_PREFIX;
import static com.android.ide.common.layout.LayoutConstants.NEW_ID_PREFIX;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.android.annotations.VisibleForTesting;
import com.android.ide.common.api.IViewMetadata.FillPreference;
import com.android.ide.eclipse.scirocco.AdtPlugin;
import com.android.ide.eclipse.scirocco.internal.editors.IconFactory;
import com.android.ide.eclipse.scirocco.internal.editors.layout.descriptors.LayoutDescriptors;
import com.android.ide.eclipse.scirocco.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.scirocco.internal.sdk.AndroidTargetData;
import com.android.util.Pair;

/**
 * The {@link ViewMetadataRepository} contains additional metadata for Android view
 * classes
 */
public class ViewMetadataRepository {
    private static final String PREVIEW_CONFIG_FILENAME = "rendering-configs.xml";  //$NON-NLS-1$
    private static final String METADATA_FILENAME = "extra-view-metadata.xml";  //$NON-NLS-1$

    /** Singleton instance */
    private static ViewMetadataRepository sInstance = new ViewMetadataRepository();

    /**
     * Returns the singleton instance
     *
     * @return the {@link ViewMetadataRepository}
     */
    public static ViewMetadataRepository get() {
        return sInstance;
    }

    /**
     * Ever increasing counter used to assign natural ordering numbers to views and
     * categories
     */
    private static int sNextOrdinal = 0;

    /**
     * List of categories (which contain views); constructed lazily so use
     * {@link #getCategories()}
     */
    private List<CategoryData> mCategories;

    /**
     * Map from class names to view data objects; constructed lazily so use
     * {@link #getClassToView}
     */
    private Map<String, ViewData> mClassToView;

    /** Hidden constructor: Create via factory {@link #get()} instead */
    private ViewMetadataRepository() {
    }

    /** Returns a map from class fully qualified names to {@link ViewData} objects */
    private Map<String, ViewData> getClassToView() {
        if (mClassToView == null) {
            int initialSize = 75;
            mClassToView = new HashMap<String, ViewData>(initialSize);
            List<CategoryData> categories = getCategories();
            for (CategoryData category : categories) {
                for (ViewData view : category) {
                    mClassToView.put(view.getFcqn(), view);
                }
            }
            assert mClassToView.size() <= initialSize;
        }

        return mClassToView;
    }

    /**
     * Returns an XML document containing rendering configurations for the various Android
     * views. The FQN of each view can be obtained via the
     * {@link #getFullClassName(Element)} method
     *
     * @return an XML document containing rendering elements
     */
    public Document getRenderingConfigDoc() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Class<ViewMetadataRepository> clz = ViewMetadataRepository.class;
        InputStream paletteStream = clz.getResourceAsStream(PREVIEW_CONFIG_FILENAME);
        InputSource is = new InputSource(paletteStream);
        try {
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(is);
        } catch (Exception e) {
            AdtPlugin.log(e, "Parsing palette file failed");
            return null;
        }
    }

    /**
     * Returns a fully qualified class name for an element in the rendering document
     * returned by {@link #getRenderingConfigDoc()}
     *
     * @param element the element to look up the fqcn for
     * @return the fqcn of the view the element represents a preview for
     */
    public String getFullClassName(Element element) {
        // We don't use the element tag name, because in some cases we have
        // an outer element to render some interesting inner element, such as a tab widget
        // (which must be rendered inside a tab host).
        //
        // Therefore, we instead use the convention that the id is the fully qualified
        // class name, with .'s replaced with _'s.

        // Special case: for tab host we aren't allowed to mess with the id
        String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);

        if ("@android:id/tabhost".equals(id)) {
            // Special case to distinguish TabHost and TabWidget
            NodeList children = element.getChildNodes();
            if (children.getLength() > 1 && (children.item(1) instanceof Element)) {
                Element child = (Element) children.item(1);
                String childId = child.getAttributeNS(ANDROID_URI, ATTR_ID);
                if ("@+id/android_widget_TabWidget".equals(childId)) {
                    return "android.widget.TabWidget"; // TODO: Tab widget!
                }
            }
            return "android.widget.TabHost"; // TODO: Tab widget!
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        if (id.startsWith(NEW_ID_PREFIX)) {
            i = NEW_ID_PREFIX.length();
        } else if (id.startsWith(ID_PREFIX)) {
            i = ID_PREFIX.length();
        }

        for (; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '_') {
                sb.append('.');
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /** Returns an ordered list of categories and views, parsed from a metadata file */
    private List<CategoryData> getCategories() {
        if (mCategories == null) {
            mCategories = new ArrayList<CategoryData>();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Class<ViewMetadataRepository> clz = ViewMetadataRepository.class;
            InputStream inputStream = clz.getResourceAsStream(METADATA_FILENAME);
            InputSource is = new InputSource(new BufferedInputStream(inputStream));
            try {
                factory.setNamespaceAware(true);
                factory.setValidating(false);
                factory.setIgnoringElementContentWhitespace(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(is);
                Map<String, FillPreference> fillTypes = new HashMap<String, FillPreference>();
                for (FillPreference pref : FillPreference.values()) {
                    fillTypes.put(pref.toString().toLowerCase(), pref);
                }

                NodeList categoryNodes = document.getDocumentElement().getChildNodes();
                for (int i = 0, n = categoryNodes.getLength(); i < n; i++) {
                    Node node = categoryNodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        if (element.getNodeName().equals("category")) { //$NON-NLS-1$
                            String name = element.getAttribute("name"); //$NON-NLS-1$
                            CategoryData category = new CategoryData(name);
                            NodeList children = element.getChildNodes();
                            for (int j = 0, m = children.getLength(); j < m; j++) {
                                Node childNode = children.item(j);
                                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element child = (Element) childNode;
                                    ViewData view = createViewData(fillTypes, child,
                                            null, FillPreference.NONE, RenderMode.NORMAL);
                                    category.addView(view);
                                }
                            }
                            mCategories.add(category);
                        }
                    }
                }
            } catch (Exception e) {
                AdtPlugin.log(e, "Invalid palette metadata"); //$NON-NLS-1$
            }
        }

        return mCategories;
    }

    private ViewData createViewData(Map<String, FillPreference> fillTypes,
            Element child, String defaultFqcn, FillPreference defaultFill,
            RenderMode defaultRender) {
        String fqcn = child.getAttribute("class"); //$NON-NLS-1$
        if (fqcn.length() == 0) {
            fqcn = defaultFqcn;
        }
        String fill = child.getAttribute("fill"); //$NON-NLS-1$
        FillPreference fillPreference = null;
        if (fill.length() > 0) {
            fillPreference = fillTypes.get(fill);
        }
        if (fillPreference == null) {
            fillPreference = defaultFill;
        }
        String skip = child.getAttribute("skip"); //$NON-NLS-1$
        RenderMode renderMode = defaultRender;
        String render = child.getAttribute("render"); //$NON-NLS-1$
        if (render.length() > 0) {
            renderMode = RenderMode.get(render);
        }
        String displayName = child.getAttribute("name"); //$NON-NLS-1$
        if (displayName.length() == 0) {
            displayName = null;
        }
        String relatedTo = child.getAttribute("relatedTo"); //$NON-NLS-1$
        ViewData view = new ViewData(fqcn, displayName, fillPreference,
                skip.length() == 0 ? false : Boolean.valueOf(skip),
                renderMode, relatedTo);

        String init = child.getAttribute("init"); //$NON-NLS-1$
        String icon = child.getAttribute("icon"); //$NON-NLS-1$

        view.setInitString(init);
        if (icon.length() > 0) {
            view.setIconName(icon);
        }

        // Nested variations?
        if (child.hasChildNodes()) {
            // Palette variations
            NodeList childNodes = child.getChildNodes();
            for (int k = 0, kl = childNodes.getLength(); k < kl; k++) {
                Node variationNode = childNodes.item(k);
                if (variationNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element variation = (Element) variationNode;
                    ViewData variationView = createViewData(fillTypes, variation,
                            fqcn, fillPreference, renderMode);
                    view.addVariation(variationView);
                }
            }
        }

        return view;
    }

    /**
     * Computes the palette entries for the given {@link AndroidTargetData}, looking up the
     * available node descriptors, categorizing and sorting them.
     *
     * @param targetData the target data for which to compute palette entries
     * @param alphabetical if true, sort all items in alphabetical order
     * @param createCategories if true, organize the items into categories
     * @return a list of pairs where each pair contains of the category label and an
     *         ordered list of elements to be included in that category
     */
    public List<Pair<String, List<ViewElementDescriptor>>> getPaletteEntries(
            AndroidTargetData targetData, boolean alphabetical, boolean createCategories) {
        List<Pair<String, List<ViewElementDescriptor>>> result =
            new ArrayList<Pair<String, List<ViewElementDescriptor>>>();

        List<List<ViewElementDescriptor>> lists = new ArrayList<List<ViewElementDescriptor>>(2);
        LayoutDescriptors layoutDescriptors = targetData.getLayoutDescriptors();
        lists.add(layoutDescriptors.getViewDescriptors());
        lists.add(layoutDescriptors.getLayoutDescriptors());

        // First record map of FQCN to ViewElementDescriptor such that we can quickly
        // determine if a particular palette entry is available
        Map<String, ViewElementDescriptor> fqcnToDescriptor =
            new HashMap<String, ViewElementDescriptor>();
        for (List<ViewElementDescriptor> list : lists) {
            for (ViewElementDescriptor view : list) {
                String fqcn = view.getFullClassName();
                if (fqcn == null) {
                    // <view> and <merge> tags etc
                    fqcn = view.getUiName();
                }
                fqcnToDescriptor.put(fqcn, view);
            }
        }

        Set<ViewElementDescriptor> remaining = new HashSet<ViewElementDescriptor>(
                layoutDescriptors.getViewDescriptors().size()
                + layoutDescriptors.getLayoutDescriptors().size());
        remaining.addAll(layoutDescriptors.getViewDescriptors());
        remaining.addAll(layoutDescriptors.getLayoutDescriptors());

        // Now iterate in palette metadata order over the items in the palette and include
        // any that also appear as a descriptor
        List<ViewElementDescriptor> categoryItems = new ArrayList<ViewElementDescriptor>();
        for (CategoryData category : getCategories()) {
            if (createCategories) {
                categoryItems = new ArrayList<ViewElementDescriptor>();
            }
            for (ViewData view : category) {
                String fqcn = view.getFcqn();
                ViewElementDescriptor descriptor = fqcnToDescriptor.get(fqcn);
                if (descriptor != null) {
                    if (view.getDisplayName() != null || view.getInitString().length() > 0) {
                        categoryItems.add(new PaletteMetadataDescriptor(descriptor,
                                view.getDisplayName(), view.getInitString(), view.getIconName()));
                    } else {
                        categoryItems.add(descriptor);
                    }
                    remaining.remove(descriptor);

                    if (view.hasVariations()) {
                        for (ViewData variation : view.getVariations()) {
                            String init = variation.getInitString();
                            String icon = variation.getIconName();
                            ViewElementDescriptor desc = new PaletteMetadataDescriptor(descriptor,
                                    variation.getDisplayName(), init, icon);
                            categoryItems.add(desc);
                        }
                    }
                }
            }

            if (createCategories && categoryItems.size() > 0) {
                if (alphabetical) {
                    Collections.sort(categoryItems);
                }
                result.add(Pair.of(category.getName(), categoryItems));
            }
        }

        if (remaining.size() > 0) {
            List<ViewElementDescriptor> otherItems = new ArrayList<ViewElementDescriptor>(remaining);
            // Always sorted, we don't have a natural order for these unknowns
            Collections.sort(otherItems);
            if (createCategories) {
                result.add(Pair.of("Other", otherItems));
            } else {
                categoryItems.addAll(otherItems);
            }
        }

        if (!createCategories) {
            if (alphabetical) {
                Collections.sort(categoryItems);
            }
            result.add(Pair.of("Views", categoryItems));
        }

        return result;
    }

    @VisibleForTesting
    Collection<String> getAllFqcns() {
        return getClassToView().keySet();
    }

    /**
     * Metadata holder for a particular category - contains the name of the category, its
     * ordinal (for natural/logical sorting order) and views contained in the category
     */
    private static class CategoryData implements Iterable<ViewData>, Comparable<CategoryData> {
        /** Category name */
        private final String mName;
        /** Views included in this category */
        private final List<ViewData> mViews = new ArrayList<ViewData>();
        /** Natural ordering rank */
        private final int mOrdinal = sNextOrdinal++;

        /** Constructs a new category with the given name */
        private CategoryData(String name) {
            super();
            mName = name;
        }

        /** Adds a new view into this category */
        private void addView(ViewData view) {
            mViews.add(view);
        }

        private String getName() {
            return mName;
        }

        // Implements Iterable<ViewData> such that we can use for-each on the category to
        // enumerate its views
        public Iterator<ViewData> iterator() {
            return mViews.iterator();
        }

        // Implements Comparable<CategoryData> such that categories can be naturally sorted
        public int compareTo(CategoryData other) {
            return mOrdinal - other.mOrdinal;
        }
    }

    /** Metadata holder for a view of a given fully qualified class name */
    private static class ViewData implements Comparable<ViewData> {
        /** The fully qualified class name of the view */
        private final String mFqcn;
        /** Fill preference of the view */
        private final FillPreference mFillPreference;
        /** Skip this item in the palette? */
        private final boolean mSkip;
        /** Must this item be rendered alone? skipped? etc */
        private final RenderMode mRenderMode;
        /** Related views */
        private final String mRelatedTo;
        /** The relative rank of the view for natural ordering */
        private final int mOrdinal = sNextOrdinal++;
        /** List of optional variations */
        private List<ViewData> mVariations;
        /** Display name. Can be null. */
        private String mDisplayName;
        /**
         * Optional initialization string - a comma separate set of name/value pairs to
         * initialize the element with
         */
        private String mInitString;
        /** The name of an icon (known to the {@link IconFactory} to show for this view */
        private String mIconName;

        /** Constructs a new view data for the given class */
        private ViewData(String fqcn, String displayName,
                FillPreference fillPreference, boolean skip, RenderMode renderMode,
                String relatedTo) {
            super();
            mFqcn = fqcn;
            mDisplayName = displayName;
            mFillPreference = fillPreference;
            mSkip = skip;
            mRenderMode = renderMode;
            mRelatedTo = relatedTo;
        }

        /** Returns the {@link FillPreference} for views of this type */
        private FillPreference getFillPreference() {
            return mFillPreference;
        }

        /** Fully qualified class name of views of this type */
        private String getFcqn() {
            return mFqcn;
        }

        private String getDisplayName() {
            return mDisplayName;
        }

        // Implements Comparable<ViewData> such that views can be sorted naturally
        public int compareTo(ViewData other) {
            return mOrdinal - other.mOrdinal;
        }

        public RenderMode getRenderMode() {
            return mRenderMode;
        }

        public boolean getSkip() {
            return mSkip;
        }

        public List<String> getRelatedTo() {
            if (mRelatedTo == null || mRelatedTo.length() == 0) {
                return Collections.emptyList();
            } else {
                String[] basenames = mRelatedTo.split(","); //$NON-NLS-1$
                List<String> result = new ArrayList<String>();
                ViewMetadataRepository repository = ViewMetadataRepository.get();
                Map<String, ViewData> classToView = repository.getClassToView();

                List<String> fqns = new ArrayList<String>(classToView.keySet());
                for (String basename : basenames) {
                    boolean found = false;
                    for (String fqcn : fqns) {
                        String suffix = '.' + basename;
                        if (fqcn.endsWith(suffix)) {
                            result.add(fqcn);
                            found = true;
                            break;
                        }
                    }
                    assert found : basename;
                }

                return result;
            }
        }

        void addVariation(ViewData variation) {
            if (mVariations == null) {
                mVariations = new ArrayList<ViewData>(4);
            }
            mVariations.add(variation);
        }

        List<ViewData> getVariations() {
            return mVariations;
        }

        boolean hasVariations() {
            return mVariations != null && mVariations.size() > 0;
        }

        private void setInitString(String initString) {
            this.mInitString = initString;
        }

        private String getInitString() {
            return mInitString;
        }

        private void setIconName(String iconName) {
            this.mIconName = iconName;
        }

        private String getIconName() {
            return mIconName;
        }
    }

    /**
     * Returns the {@link FillPreference} for classes with the given fully qualified class
     * name
     *
     * @param fqcn the fully qualified class name of the view
     * @return a suitable {@link FillPreference} for the given view type
     */
    public FillPreference getFillPreference(String fqcn) {
        ViewData view = getClassToView().get(fqcn);
        if (view != null) {
            return view.getFillPreference();
        }

        return FillPreference.NONE;
    }

    /**
     * Returns the {@link RenderMode} for classes with the given fully qualified class
     * name
     *
     * @param fqcn the fully qualified class name
     * @return the {@link RenderMode} to use for previews of the given view type
     */
    public RenderMode getRenderMode(String fqcn) {
        ViewData view = getClassToView().get(fqcn);
        if (view != null) {
            return view.getRenderMode();
        }

        return RenderMode.NORMAL;
    }

    /**
     * Returns true if classes with the given fully qualified class name should be hidden
     * or skipped from the palette
     *
     * @param fqcn the fully qualified class name
     * @return true if views of the given type should be hidden from the palette
     */
    public boolean getSkip(String fqcn) {
        ViewData view = getClassToView().get(fqcn);
        if (view != null) {
            return view.getSkip();
        }

        return false;
    }

    /**
     * Returns a set of fully qualified names for views that are closely related to the
     * given view
     *
     * @param fqcn the fully qualified class name
     * @return a list, never null but possibly empty, of views that are related to the
     *         view of the given type
     */
    public List<String> getRelatedTo(String fqcn) {
        ViewData view = getClassToView().get(fqcn);
        if (view != null) {
            return view.getRelatedTo();
        }

        return Collections.emptyList();
    }

    /** Render mode for palette preview */
    public enum RenderMode {
        /**
         * Render previews, and it can be rendered as a sibling of many other views in a
         * big linear layout
         */
        NORMAL,
        /** This view needs to be rendered alone */
        ALONE,
        /**
         * Skip this element; it doesn't work or does not produce any visible artifacts
         * (such as the basic layouts)
         */
        SKIP;

        public static RenderMode get(String render) {
            if ("alone".equals(render)) {
                return ALONE;
            } else if ("skip".equals(render)) {
                return SKIP;
            } else {
                return NORMAL;
            }
        }
    }
}
