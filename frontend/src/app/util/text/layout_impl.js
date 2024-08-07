import LayoutType from "./layout/LayoutType.js";
import textLayoutImpl from "./TextLayout.js";

/**
 * @typedef {object} LayoutFromRootOptions
 * @property {number} x
 * @property {number} y
 * @property {number} width
 * @property {number} height
 */

/**
 * Performs a layout operation using only content.
 *
 * @param {HTMLDivElement} root
 * @param {LayoutFromRootOptions} [options]
 * @returns {ContentLayout}
 */
export function layoutFromRoot(root, options) {
  return textLayout.layoutFromRoot(root, options);
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
 * @param {"full"|"partial"} type
 * @param {CommandMutations} mutations
 * @returns {ContentLayout}
 */
export function layoutFromEditor(editor, type, mutations) {
  console.log(editor, type, mutations);
  if (!LayoutType.isLayoutType(type)) throw new TypeError("`type` is not a valid layout type");
  if (type === LayoutType.FULL) {
    return textLayout.layoutFromElement(editor.element);
  }
  return textLayout.partialLayoutFromElement(editor.element, mutations);
}

export const textLayout = textLayoutImpl;

export default {
  layoutFromEditor,
  layoutFromElement,
  layoutFromRoot,
};
