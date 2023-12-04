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

package com.adobe.marketing.mobile.services.ui;

import com.adobe.marketing.mobile.services.ui.AlertSetting;
import com.adobe.marketing.mobile.services.ui.AndroidUIService;
import com.adobe.marketing.mobile.services.ui.FloatingButton;
import com.adobe.marketing.mobile.services.ui.FloatingButtonView;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.NotificationSetting;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class TestAppUIServices {

	private AndroidUIService uiService;

	public TestAppUIServices() {
		uiService = new AndroidUIService();
	}

	public void showAlert(final String title, final String message, final String positiveButtonText,
						  final String negativeButtonText) {
		AlertSetting settings =  AlertSetting.build(title, message, positiveButtonText, negativeButtonText);
		uiService.showAlert(settings, null);
	}

	public void showLocalNotification(final String identifier, final String content, final long fireDate,
									  final int delaySeconds, final String deeplink, final Map<String, Object> userInfo,
									  final String sound) {
		NotificationSetting notificationSetting = NotificationSetting.build(identifier, content, fireDate, delaySeconds, deeplink, userInfo, sound, null);
		uiService.showLocalNotification(notificationSetting);
	}

	public void showLocalNotification(final String identifier, final String content, final long fireDate,
									  final int delaySeconds, final String deeplink, final Map<String, Object> userInfo,
									  final String sound, final String title) {
		NotificationSetting notificationSetting = NotificationSetting.build(identifier, content, fireDate, delaySeconds, deeplink, userInfo, sound, title);
		uiService.showLocalNotification(notificationSetting);
	}

	public void showFullscreenMessage(final String html) {
		final MessageSettings messageSettings = new MessageSettings();
		// ACS fullscreen messages are displayed at 100% scale
		messageSettings.setHeight(100);
		messageSettings.setWidth(100);
		messageSettings.setParent(this);
		messageSettings.setVerticalAlign(MessageSettings.MessageAlignment.TOP);
		messageSettings.setHorizontalAlign(MessageSettings.MessageAlignment.CENTER);
		messageSettings.setDisplayAnimation(MessageSettings.MessageAnimation.BOTTOM);
		messageSettings.setDismissAnimation(MessageSettings.MessageAnimation.BOTTOM);
		messageSettings.setBackdropColor("#FFFFFF"); // html code for white
		messageSettings.setBackdropOpacity(1.0f);
		messageSettings.setUiTakeover(true);

		FullscreenMessage fullScreenMessage = uiService.createFullscreenMessage(html, new FullscreenMessageDelegate() {
			@Override
			public void onShow(FullscreenMessage message) {}
			@Override
			public void onDismiss(FullscreenMessage message) {}
			@Override
			public boolean overrideUrlLoad(FullscreenMessage message, String url) {
				try {
					final URI uri = new URI(url);
					if (uri.getScheme().equals("adbinapp") && uri.getHost().equals("dismiss")) {
						message.dismiss();
						return true;
					}
				} catch (URISyntaxException ex) {
					return false;
				}
				return false;
			}
			@Override
			public void onShowFailure() {}
		}, false, messageSettings);

		if (fullScreenMessage != null) {
			fullScreenMessage.show();
		}
	}

	public void showUrl(final String url) {
		uiService.showUrl(url);
	}

	public void showFloatingButton() {
		FloatingButton floatingButtonManager = uiService.createFloatingButton(null);
		floatingButtonManager.display();
	}

	public void hideFloatingButton() {
		FloatingButton floatingButtonManager = uiService.createFloatingButton(null);
		floatingButtonManager.remove();
	}

	public static String getFloatingButtonTag() {
		return FloatingButtonView.VIEW_TAG;
	}
}
