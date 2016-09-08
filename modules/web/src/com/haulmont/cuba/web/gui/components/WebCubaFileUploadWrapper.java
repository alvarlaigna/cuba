/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.web.toolkit.ui.UploadComponent;
import com.vaadin.server.SizeWithUnit;
import com.vaadin.ui.*;
import org.apache.commons.lang.StringUtils;

import static com.vaadin.ui.themes.BaseTheme.BUTTON_LINK;

public class WebCubaFileUploadWrapper extends CustomField {

    protected static final String FILE_UPLOAD_WRAPPER = "cuba-fileupload-wrapper";
    protected static final String EMPTY_VALUE_STYLE = "cuba-fileupload-empty";
    protected static final String ERROR_STYLE = "error";

    protected Messages messages = AppBeans.get(Messages.NAME);

    protected HorizontalLayout container;
    protected Button fileNameButton;
    protected Button clearButton;
    protected UploadComponent uploadButton;

    protected boolean showFileName = false;
    protected boolean showClearButton = true;
    protected String fileName;

    protected boolean editable;

    public WebCubaFileUploadWrapper(UploadComponent uploadButton) {
        setPrimaryStyleName(FILE_UPLOAD_WRAPPER);
        initLayout(uploadButton);
    }

    private void initLayout(UploadComponent uploadComponent) {
        this.uploadButton = uploadComponent;

        container = new HorizontalLayout();
        container.setSpacing(true);
        container.addStyleName("fileupload-wrapper-container");

        fileNameButton = new Button();
        fileNameButton.setWidth("100%");
        fileNameButton.addStyleName(BUTTON_LINK);
        container.addComponent(fileNameButton);
        container.setComponentAlignment(fileNameButton, Alignment.MIDDLE_LEFT);

        container.addComponent(uploadComponent);

        clearButton = new Button(messages.getMainMessage("FileUploadField.clearButtonCaption"));
        container.addComponent(clearButton);
        setShowClearButton(showClearButton);

        setShowFileName(false);
    }

    @Override
    protected Component initContent() {
        return container;
    }

    @Override
    public Class getType() {
        return FileDescriptor.class;
    }

    @Override
    protected void setInternalValue(Object newValue) {
        super.setInternalValue(newValue);

        if (newValue != null) {
            FileDescriptor fileDescriptor = (FileDescriptor) newValue;
            setFileNameButtonCaption(fileDescriptor.getName());
        } else {
            setFileNameButtonCaption(null);
        }
    }

    public boolean isShowFileName() {
        return showFileName;
    }

    public void setShowFileName(boolean showFileName) {
        this.showFileName = showFileName;
        fileNameButton.setVisible(showFileName);

        updateComponentSizes();
    }

    public void setFileNameButtonCaption(String title) {
        fileName = title;

        if (StringUtils.isNotEmpty(title)) {
            fileNameButton.setCaption(title);
            fileNameButton.removeStyleName(EMPTY_VALUE_STYLE);

            if (isRequired()) {
                fileNameButton.removeStyleName(ERROR_STYLE);
            }
        } else {
            fileNameButton.setCaption(messages.getMainMessage("FileUploadField.fileNotSelected"));
            fileNameButton.addStyleName(EMPTY_VALUE_STYLE);

            if (isRequired()) {
                fileNameButton.addStyleName(ERROR_STYLE);
            } else {
                fileNameButton.removeStyleName(ERROR_STYLE);
            }
        }
    }

    public void addFileNameClickListener(Button.ClickListener clickListener) {
        fileNameButton.addClickListener(clickListener);
    }

    public void removeFileNameClickListener(Button.ClickListener clickListener) {
        fileNameButton.removeClickListener(clickListener);
    }

    private void updateComponentWidth() {
        if (container != null && getWidth() >= 0) {
            container.setWidth(getWidth(), getWidthUnits());
            if (isShowFileName()) {
                fileNameButton.setWidth(100, Unit.PERCENTAGE);
                container.setExpandRatio(fileNameButton, 1);

                uploadButton.setWidthUndefined();
                container.setExpandRatio(uploadButton, 0);
            } else {
                uploadButton.setWidth(100, Unit.PERCENTAGE);
                container.setExpandRatio(uploadButton, 1);
            }
        } else {
            setWidthUndefined();
        }
    }

    private void updateComponentHeight() {
        if (container != null && getHeight() >= 0) {
            container.setHeight(getHeight(), getHeightUnits());

            if (isShowFileName()) {
                fileNameButton.setHeight(100, Unit.PERCENTAGE);
                container.setExpandRatio(fileNameButton, 1);

                uploadButton.setHeightUndefined();
                container.setExpandRatio(uploadButton, 0);
            } else {
                uploadButton.setHeight(100, Unit.PERCENTAGE);
                container.setExpandRatio(uploadButton, 1);
            }
        } else {
            setHeightUndefined();
        }
    }

    private void updateComponentSizes() {
        updateComponentWidth();
        updateComponentHeight();
    }

    @Override
    public void setWidth(float width, Unit unit) {
        super.setWidth(width, unit);

        updateComponentWidth();
    }

    @Override
    public void setWidth(String width) {
        super.setWidth(width);

        SizeWithUnit size = SizeWithUnit.parseStringSize(width);
        if (size != null) {
            setWidth(size.getSize(), size.getUnit());
        } else {
            setWidthUndefined();
        }
    }

    @Override
    public void setWidthUndefined() {
        if (container != null) {
            container.setWidthUndefined();

            if (fileNameButton != null) {
                fileNameButton.setWidth(-1, Unit.PIXELS);
            }
            uploadButton.setWidthUndefined();
        }
    }

    @Override
    public void setHeight(float height, Unit unit) {
        super.setHeight(height, unit);

        updateComponentHeight();
    }

    @Override
    public void setHeightUndefined() {
        if (container != null) {
            container.setHeightUndefined();

            if (fileNameButton != null) {
                fileNameButton.setHeightUndefined();
            }
            uploadButton.setHeightUndefined();
        }
    }

    @Override
    public void setHeight(String height) {
        super.setHeight(height);

        SizeWithUnit size = SizeWithUnit.parseStringSize(height);
        if (size != null) {
            setHeight(size.getSize(), size.getUnit());
        } else {
            setHeightUndefined();
        }
    }

    @Override
    public void setSizeFull() {
        super.setSizeFull();

        setHeight("100%");
        setWidth("100%");
    }

    public void setEditable(boolean editable) {
        this.editable = editable;

        updateButtonsVisibility();
    }

    @Override
    public void setRequired(boolean required) {
        super.setRequired(required);

        setFileNameButtonCaption(fileName);
        updateButtonsVisibility();
    }

    @Override
    public void focus() {
        super.focus();
        if (uploadButton instanceof Focusable) {
            ((Focusable) uploadButton).focus();
        }
    }

    protected void updateButtonsVisibility() {
        uploadButton.setVisible(editable);

        if (editable && !isRequired() && showClearButton) {
            clearButton.setVisible(true);
        } else {
            clearButton.setVisible(false);
        }
    }

    /*
    * Clear button
    * */

    public boolean isShowClearButton() {
        return showClearButton;
    }

    public void setShowClearButton(boolean showClearButton) {
        this.showClearButton = showClearButton;

        updateButtonsVisibility();
    }

    public void setClearButtonCaption(String caption) {
        clearButton.setCaption(caption);
    }

    public String getClearButtonCaption() {
        return clearButton.getCaption();
    }

    public void setClearButtonIcon(String icon) {
        clearButton.setIcon(WebComponentsHelper.getIcon(icon));
    }

    public String getClearButtonIcon() {
        return clearButton.getIcon().toString();
    }

    public void setClearButtonAction(Button.ClickListener listener) {
        clearButton.addClickListener(listener);
    }

    public void removeClearButtonAction(Button.ClickListener listener) {
        clearButton.removeClickListener(listener);
    }

    public void setClearButtonDescription(String description) {
        clearButton.setDescription(description);
    }

    public String getClearButtonDescription() {
        return clearButton.getDescription();
    }

    /*
    * Upload button
    * */

    public void setUploadButtonDescription(String description) {
        uploadButton.setDescription(description);
    }

    public String getUploadButtonDescription() {
        return uploadButton.getDescription();
    }

    public void setUploadButtonCaption(String caption) {
        uploadButton.setCaption(caption);
    }

    public String getUploadButtonCaption() {
        return uploadButton.getCaption();
    }

    public void setUploadButtonIcon(String icon) {
        uploadButton.setIcon(WebComponentsHelper.getIcon(icon));
    }

    public String getUploadButtonIcon() {
        return uploadButton.getIcon().toString();
    }
}