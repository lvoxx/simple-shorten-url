// validation tools
import { anyPass, isEmpty, isNil, is } from "ramda";

export { isEmpty, anyPass, isNil, is };

/**
 * Returns `true` if the given value is its type's empty value, `null` or `undefined`.
 *
 * @func isNilOrEmpty
 * @memberOf Validator
 * @category Validator
 * @sig * -> Boolean
 * @param {*} val The value to test
 * @return {Boolean}
 * @see {@link http://ramdajs.com/docs/#isEmpty|isEmpty}, {@link http://ramdajs.com/docs/#isNil|isNil}
 * @example
 *
 * Validator.isNilOrEmpty([1, 2, 3]); //=> false
 * Validator.isNilOrEmpty([]); //=> true
 * Validator.isNilOrEmpty(''); //=> true
 * Validator.isNilOrEmpty(null); //=> true
 * Validator.isNilOrEmpty(undefined): //=> true
 * Validator.isNilOrEmpty({}); //=> true
 * Validator.isNilOrEmpty({length: 0}); //=> false
 */
export const isNilOrEmpty = anyPass([isNil, isEmpty]);

/**
 * Checks if array is not empty
 *
 * @param {Array} value - the array argument
 * @returns {Boolean} - false or true, depending on the value of the array
 */
export const isArrayNotEmpty = (array: unknown[]): boolean =>
  !isNilOrEmpty(array) && Array.isArray(array) && array.length > 0;

/**
 * Checks if an image URL is valid and loads successfully
 * @param imageSrc - The source URL of the image
 * @returns A promise that resolves with the HTMLImageElement if successful, or null if the source is empty
 */
export const checkImage = (
  imageSrc: string = "",
): Promise<HTMLImageElement | null> => {
  if (isNilOrEmpty(imageSrc)) {
    return Promise.resolve(null);
  }
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.src = imageSrc;
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("could not load image"));
  });
};

const isObject = (data: object | string) =>
  typeof data === "object" && data !== null;

/**
 * Check if a string is valid JSON
 *
 * @func isValidJson
 * @memberOf Validator
 * @category Validator
 * @sig String -> Boolean
 * @param jsonString
 * @returns
 */
export const validateJson = (json: object | string): object | false => {
  if (isObject(json)) {
    return json;
  }
  try {
    return JSON.parse(json as string);
  } catch (e) {
    console.error("Invalid JSON string:", e);
    return false;
  }
};
