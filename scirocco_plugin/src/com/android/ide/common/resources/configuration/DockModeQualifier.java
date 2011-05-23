/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.resources.configuration;

import com.android.resources.DockMode;
import com.android.resources.ResourceEnum;

/**
 * Resource Qualifier for Navigation Method.
 */
public final class DockModeQualifier extends EnumBasedResourceQualifier {

    public static final String NAME = "Dock Mode";

    private DockMode mValue;

    public DockModeQualifier() {
        // pass
    }

    public DockModeQualifier(DockMode value) {
        mValue = value;
    }

    public DockMode getValue() {
        return mValue;
    }

    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Dock Mode";
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        DockMode mode = DockMode.getEnum(value);
        if (mode != null) {
            DockModeQualifier qualifier = new DockModeQualifier(mode);
            config.setDockModeQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier qualifier) {
        // only NONE is a match other DockModes
        if (mValue == DockMode.NONE) {
            return true;
        }

        // others must be an exact match
        return ((DockModeQualifier)qualifier).mValue == mValue;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier compareTo, ResourceQualifier reference) {
        if (compareTo == null) {
            return true;
        }

        DockModeQualifier compareQualifier = (DockModeQualifier)compareTo;
        DockModeQualifier referenceQualifier = (DockModeQualifier)reference;

        if (compareQualifier.getValue() == referenceQualifier.getValue()) {
            // what we have is already the best possible match (exact match)
            return false;
        } else  if (mValue == referenceQualifier.mValue) {
            // got new exact value, this is the best!
            return true;
        } else if (mValue == DockMode.NONE) {
            // else "none" can be a match in case there's no exact match
            return true;
        }

        return false;
    }
}
