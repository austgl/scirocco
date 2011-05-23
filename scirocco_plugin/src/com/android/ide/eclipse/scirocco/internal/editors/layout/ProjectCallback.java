/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.scirocco.internal.editors.layout;

import com.android.ide.common.rendering.api.IProjectCallback;
import com.android.ide.common.rendering.api.LayoutLog;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.legacy.LegacyCallback;
import com.android.ide.eclipse.scirocco.internal.editors.layout.ProjectCallback;
import com.android.ide.eclipse.scirocco.AdtConstants;
import com.android.ide.eclipse.scirocco.AdtPlugin;
import com.android.ide.eclipse.scirocco.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.scirocco.internal.resources.manager.ProjectClassLoader;
import com.android.ide.eclipse.scirocco.internal.resources.manager.ProjectResources;
import com.android.resources.ResourceType;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.xml.ManifestData;
import com.android.util.Pair;

import org.eclipse.core.resources.IProject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loader for Android Project class in order to use them in the layout editor.
 * <p/>This implements {@link IProjectCallback} for the old and new API through
 * {@link ILegacyCallback}
 */
public final class ProjectCallback extends LegacyCallback {

    private final HashMap<String, Class<?>> mLoadedClasses = new HashMap<String, Class<?>>();
    private final Set<String> mMissingClasses = new TreeSet<String>();
    private final Set<String> mBrokenClasses = new TreeSet<String>();
    private final IProject mProject;
    private final ClassLoader mParentClassLoader;
    private final ProjectResources mProjectRes;
    private boolean mUsed = false;
    private String mNamespace;
    private ProjectClassLoader mLoader = null;
    private LayoutLog mLogger;

    /**
     * Creates a new {@link ProjectCallback} to be used with the layout lib.
     *
     * @param classLoader The class loader that was used to load layoutlib.jar
     * @param projectRes the {@link ProjectResources} for the project.
     * @param project the project.
     */
    public ProjectCallback(ClassLoader classLoader, ProjectResources projectRes, IProject project) {
        mParentClassLoader = classLoader;
        mProjectRes = projectRes;
        mProject = project;
    }

    public Set<String> getMissingClasses() {
        return mMissingClasses;
    }

    public Set<String> getUninstantiatableClasses() {
        return mBrokenClasses;
    }

    /**
     * Sets the {@link LayoutLog} logger to use for error messages during problems
     *
     * @param logger the new logger to use, or null to clear it out
     */
    public void setLogger(LayoutLog logger) {
        mLogger = logger;
    }

    /**
     * Returns the {@link LayoutLog} logger used for error messages, or null
     *
     * @return the logger being used, or null if no logger is in use
     */
    public LayoutLog getLogger() {
        return mLogger;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation goes through the output directory of the Eclipse project and loads the
     * <code>.class</code> file directly.
     */
    @SuppressWarnings("unchecked")
    public Object loadView(String className, Class[] constructorSignature,
            Object[] constructorParameters)
            throws ClassNotFoundException, Exception {
        mUsed = true;

        // look for a cached version
        Class<?> clazz = mLoadedClasses.get(className);
        if (clazz != null) {
            return instantiateClass(clazz, constructorSignature, constructorParameters);
        }

        // load the class.

        try {
            if (mLoader == null) {
                mLoader = new ProjectClassLoader(mParentClassLoader, mProject);
            }
            clazz = mLoader.loadClass(className);
        } catch (Exception e) {
            // Add the missing class to the list so that the renderer can print them later.
            // no need to log this.
            mMissingClasses.add(className);
        }

        try {
            if (clazz != null) {
                // first try to instantiate it because adding it the list of loaded class so that
                // we don't add broken classes.
                Object view = instantiateClass(clazz, constructorSignature, constructorParameters);
                mLoadedClasses.put(className, clazz);

                return view;
            }
        } catch (Throwable e) {
            // Find root cause to log it.
            while (e.getCause() != null) {
                e = e.getCause();
            }

            AdtPlugin.log(e, "%1$s failed to instantiate.", className); //$NON-NLS-1$

            // Add the missing class to the list so that the renderer can print them later.
            mBrokenClasses.add(className);
        }

        // Create a mock view instead. We don't cache it in the mLoadedClasses map.
        // If any exception is thrown, we'll return a CFN with the original class name instead.
        try {
            clazz = mLoader.loadClass(SdkConstants.CLASS_MOCK_VIEW);
            Object view = instantiateClass(clazz, constructorSignature, constructorParameters);

            // Set the text of the mock view to the simplified name of the custom class
            Method m = view.getClass().getMethod("setText",
                                                 new Class<?>[] { CharSequence.class });
            m.invoke(view, getShortClassName(className));
            return view;
        } catch (Exception e) {
            // We failed to create and return a mock view.
            // Just throw back a CNF with the original class name.
            throw new ClassNotFoundException(className, e);
        }
    }

    private String getShortClassName(String fqcn) {
        // The name is typically a fully-qualified class name. Let's make it a tad shorter.

        if (fqcn.startsWith("android.")) {                                      //$NON-NLS-1$
            // For android classes, convert android.foo.Name to android...Name
            int first = fqcn.indexOf('.');
            int last = fqcn.lastIndexOf('.');
            if (last > first) {
                return fqcn.substring(0, first) + ".." + fqcn.substring(last);   //$NON-NLS-1$
            }
        } else {
            // For custom non-android classes, it's best to keep the 2 first segments of
            // the namespace, e.g. we want to get something like com.example...MyClass
            int first = fqcn.indexOf('.');
            first = fqcn.indexOf('.', first + 1);
            int last = fqcn.lastIndexOf('.');
            if (last > first) {
                return fqcn.substring(0, first) + ".." + fqcn.substring(last);   //$NON-NLS-1$
            }
        }

        return fqcn;
    }

    /**
     * Returns the namespace for the project. The namespace contains a standard part + the
     * application package.
     *
     * @return The package namespace of the project or null in case of error.
     */
    public String getNamespace() {
        if (mNamespace == null) {
            ManifestData manifestData = AndroidManifestHelper.parseForData(mProject);
            if (manifestData != null) {
                String javaPackage = manifestData.getPackage();
                mNamespace = String.format(AdtConstants.NS_CUSTOM_RESOURCES, javaPackage);
            }
        }

        return mNamespace;
    }

    public Pair<ResourceType, String> resolveResourceId(int id) {
        if (mProjectRes != null) {
            return mProjectRes.resolveResourceId(id);
        }

        return null;
    }

    public String resolveResourceId(int[] id) {
        if (mProjectRes != null) {
            return mProjectRes.resolveStyleable(id);
        }

        return null;
    }

    public Integer getResourceId(ResourceType type, String name) {
        if (mProjectRes != null) {
            return mProjectRes.getResourceId(type, name);
        }

        return null;
    }

    /**
     * Returns whether the loader has received requests to load custom views. Note that
     * the custom view loading may not actually have succeeded; this flag only records
     * whether it was <b>requested</b>.
     * <p/>
     * This allows to efficiently only recreate when needed upon code change in the
     * project.
     *
     * @return true if the loader has been asked to load custom views
     */
    public boolean isUsed() {
        return mUsed;
    }

    /**
     * Instantiate a class object, using a specific constructor and parameters.
     * @param clazz the class to instantiate
     * @param constructorSignature the signature of the constructor to use
     * @param constructorParameters the parameters to use in the constructor.
     * @return A new class object, created using a specific constructor and parameters.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Object instantiateClass(Class<?> clazz,
            Class[] constructorSignature,
            Object[] constructorParameters) throws Exception {
        Constructor<?> constructor = null;

        try {
            constructor = clazz.getConstructor(constructorSignature);

        } catch (NoSuchMethodException e) {
            // Custom views can either implement a 3-parameter, 2-parameter or a
            // 1-parameter. Let's synthetically build and try all the alternatives.
            // That's kind of like switching to the other box.
            //
            // The 3-parameter constructor takes the following arguments:
            // ...(Context context, AttributeSet attrs, int defStyle)

            int n = constructorSignature.length;
            if (n == 0) {
                // There is no parameter-less constructor. Nobody should ask for one.
                throw e;
            }

            for (int i = 3; i >= 1; i--) {
                if (i == n) {
                    // Let's skip the one we know already fails
                    continue;
                }
                Class[] sig = new Class[i];
                Object[] params = new Object[i];

                int k = i;
                if (n < k) {
                    k = n;
                }
                System.arraycopy(constructorSignature, 0, sig, 0, k);
                System.arraycopy(constructorParameters, 0, params, 0, k);

                for (k++; k <= i; k++) {
                    if (k == 2) {
                        // Parameter 2 is the AttributeSet
                        sig[k-1] = clazz.getClassLoader().loadClass("android.util.AttributeSet");
                        params[k-1] = null;

                    } else if (k == 3) {
                        // Parameter 3 is the int defstyle
                        sig[k-1] = int.class;
                        params[k-1] = 0;
                    }
                }

                constructorSignature = sig;
                constructorParameters = params;

                try {
                    // Try again...
                    constructor = clazz.getConstructor(constructorSignature);
                    if (constructor != null) {
                        // Found a suitable constructor, now let's use it.
                        // (But let's warn the user if the simple View constructor was found
                        // since Unexpected Things may happen if the attribute set constructors
                        // are not found)
                        if (constructorSignature.length < 2 && mLogger != null) {
                            mLogger.warning("wrongconstructor", //$NON-NLS-1$
                                String.format("Custom view %1$s is not using the 2- or 3-argument "
                                    + "View constructors; XML attributes will not work",
                                    clazz.getSimpleName()), null /*data*/);
                        }
                        break;
                    }
                } catch (NoSuchMethodException e1) {
                    // pass
                }
            }

            // If all the alternatives failed, throw the initial exception.
            if (constructor == null) {
                throw e;
            }
        }

        constructor.setAccessible(true);
        return constructor.newInstance(constructorParameters);
    }

    public String getAdapterItemValue(ResourceReference adapterView, ResourceReference itemRef,
            int fullPosition, int typePosition, ResourceReference viewRef, String viewClass) {
        if (viewClass.contains("TextView")) {
            return viewRef.getName() + " " + typePosition;
        }

        return null;
    }
}
