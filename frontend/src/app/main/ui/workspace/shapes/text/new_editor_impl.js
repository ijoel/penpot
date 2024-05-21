/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) KALEIDOS INC
 */

import cljs from "goog:cljs.core";

import TextEditor from "./new_editor/TextEditor.js";
import textLayoutImpl from "./new_editor/TextLayout.js";

/**
 * Applies styles to the current selection or the
 * saved selection.
 *
 * @param {TextEditor} editor
 * @param {*} styles
 */
export function applyStylesToSelection(editor, styles) {
  return editor.applyStylesToSelection(styles);
}

/**
 * Returns the editor root.
 *
 * @param {TextEditor} editor
 * @returns {HTMLDivElement}
 */
export function getRoot(editor) {
  return editor.root;
}

/**
 * Sets the editor root.
 *
 * @param {TextEditor} editor
 * @param {HTMLDivElement} root
 * @returns {TextEditor}
 */
export function setRoot(editor, root) {
  editor.root = root;
  return editor;
}

/**
 * Performs a layout operation from content.
 *
 * @param {cljs.PersistentHashMap} content
 * @param {*} options
 * @returns {ContentLayout}
 */
export function layoutFromContent(content, options) {
  return textLayout.layoutFromContent(content, options);
}

/**
 * Performs a layout operation using a HTML element.
 *
 * @param {HTMLElement} element
 * @returns {ContentLayout}
 */
export function layoutFromElement(element) {
  return textLayout.layoutFromElement(element);
}

/**
 * Performs a layout operation using a TextEditor.
 *
 * @param {TextEditor} editor
 * @param {"complete"|"partial"} type
 * @param {CommandMutations} mutations
 * @returns {ContentLayout}
 */
export function layoutFromEditor(editor, type, mutations) {
  if (type === "complete") {
    return textLayout.partialLayoutFromElement(editor.element, mutations);
  }
  return textLayout.layoutFromElement(editor.element);
}

/**
 * Creates a new Text Editor instance.
 *
 * @param {HTMLElement} element
 * @param {object} options
 * @returns {TextEditor}
 */
export function createTextEditor(element, options) {
  return new TextEditor(element, {
    ...options,
  });
}

export const textLayout = textLayoutImpl;

export default {
  textLayout,
  createTextEditor,
  setRoot,
  getRoot,
  layoutFromContent,
  layoutFromEditor,
  layoutFromElement,
};
