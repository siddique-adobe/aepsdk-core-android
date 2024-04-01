/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.services;

import android.util.DisplayMetrics;

class DisplayInfoService implements DeviceInforming.DisplayInformation {

    private final DisplayMetrics displayMetrics;

    DisplayInfoService(final DisplayMetrics displayMetrics) {
        this.displayMetrics = displayMetrics;
    }

    @Override
    public int getWidthPixels() {
        return displayMetrics.widthPixels;
    }

    @Override
    public int getHeightPixels() {
        return displayMetrics.heightPixels;
    }

    @Override
    public int getDensityDpi() {
        return displayMetrics.densityDpi;
    }
}
