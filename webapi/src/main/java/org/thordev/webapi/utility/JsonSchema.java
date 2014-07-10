package org.thordev.webapi.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

public class JsonSchema {

	public static class JsonValidateException extends Exception {

		private static final long serialVersionUID = -2303623192224225975L;

		public JsonValidateException(String message) {
			super(message);
		}

		public JsonValidateException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class ErrorStack {

		StringBuffer sb = new StringBuffer();

		public void push(String message) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(message);
		}

		public void reset() {
			sb.setLength(0);
		}

		public String getMessage() {
			return sb.toString();
		}
	}

	public static interface Validator {

		public static enum JsonType {

			NULL, ANY, BOOLEAN, STRING, NUMBER, ARRAY, OBJECT
		}

		public JsonType getType();

		public boolean validate(JsonElement value, ErrorStack stack);
	}

	public class NullValidator implements Validator {

		@Override
		public JsonType getType() {
			return JsonType.NULL;
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			if (!(value instanceof JsonNull)) {
				stack.push("Expect null");
				return false;
			} else {
				return true;
			}
		}
	}

	public class AnyValidator implements Validator {

		@Override
		public JsonType getType() {
			return JsonType.ANY;
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			return true;
		}
	}

	public class BooleanValidator implements Validator {

		public BooleanValidator() {
		}

		@Override
		public JsonType getType() {
			return JsonType.BOOLEAN;
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			if (!(value instanceof JsonPrimitive)) {
				stack.push("Expect a boolean");
				return false;
			}
			JsonPrimitive primitive = ((JsonPrimitive) value);
			if (!primitive.isBoolean()) {
				stack.push("Expect a boolean");
				return false;
			}
			return true;
		}
	}

	public class StringValidator implements Validator {

		private Pattern pattern;
		private Long minLength;
		private Long maxLength;
		private String[] options;
		private boolean isEnumerate;

		public StringValidator() {
			this(null, null, null);
		}

		public StringValidator(String regex, Long minLength, Long maxLength)
				throws PatternSyntaxException {
			if (regex == null) {
				pattern = null;
			} else {
				pattern = Pattern.compile(regex);
			}
			this.minLength = minLength;
			this.maxLength = maxLength;
			isEnumerate = false;
		}

		public StringValidator(String[] options) {
			this.options = options;
			isEnumerate = true;
		}

		public String[] getOptions() {
			return options;
		}

		public boolean isEnumerate() {
			return isEnumerate;
		}

		public Long getMinLength() {
			return minLength;
		}

		public Long getMaxLength() {
			return maxLength;
		}

		@Override
		public JsonType getType() {
			return JsonType.STRING;
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			if (!(value instanceof JsonPrimitive)) {
				stack.push("Expect a string");
				return false;
			}
			JsonPrimitive primitive = ((JsonPrimitive) value);
			if (!primitive.isString()) {
				stack.push("Expect a string");
				return false;
			}

			String strValue = primitive.getAsString();

			if (isEnumerate) {
				for (String v : options) {
					if (v.equals(strValue)) {
						return true;
					}
				}
				stack.push("String value is not one of the options.");
				return false;
			} else {
				if (minLength != null && strValue.length() < minLength) {
					stack.push("String is too short, expect greater than or equal " + minLength);
					return false;
				}
				if (maxLength != null && strValue.length() > maxLength) {
					stack.push("String value is too long, expect less than or equal " + maxLength);
					return false;
				}
				if (pattern != null) {
					Matcher matcher = pattern.matcher(strValue);
					if (!matcher.matches()) {
						stack.push("String value didn't match the rule: " + pattern.pattern());
						return false;
					}
				}
				return true;
			}
		}
	}

	public class NumberValidator implements Validator {

		private BigDecimal minValue;
		private BigDecimal maxValue;
		private boolean isOnlyInteger;
		private boolean isEnumerate;
		private BigDecimal[] options;

		public NumberValidator() {
			this(null, null, false);
		}

		public NumberValidator(BigDecimal minValue, BigDecimal maxValue, boolean onlyInteger) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.isOnlyInteger = onlyInteger;
			isEnumerate = false;
		}

		public NumberValidator(BigDecimal[] options) {
			this.options = options;
			isEnumerate = true;
		}

		@Override
		public JsonType getType() {
			return JsonType.NUMBER;
		}

		public BigDecimal[] getOptions() {
			return options;
		}

		public boolean isEnumerate() {
			return isEnumerate;
		}

		public BigDecimal getMinValue() {
			return minValue;
		}

		public BigDecimal getMaxValue() {
			return maxValue;
		}

		public boolean isOnlyInteger() {
			return isOnlyInteger;
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			if (!(value instanceof JsonPrimitive)) {
				stack.push("Expect a number");
				return false;
			}
			JsonPrimitive primitive = ((JsonPrimitive) value);
			if (!primitive.isNumber()) {
				stack.push("Expect a number");
				return false;
			}
			BigDecimal numberValue = primitive.getAsBigDecimal();

			if (isEnumerate) {
				for (BigDecimal v : options) {
					if (v.compareTo(value.getAsBigDecimal()) == 0) {
						return true;
					}
				}
				stack.push("Number value is not one of the options.");
				return false;
			} else {
				if (minValue != null && numberValue.compareTo(minValue) == -1) {
					stack.push("Number is too small, expect greater than or equal " + minValue);
					return false;
				}
				if (maxValue != null && numberValue.compareTo(maxValue) == 1) {
					stack.push("Number is too large, expect less than or equal " + maxValue);
					return false;
				}
				if (isOnlyInteger) {
					try {
						numberValue.toBigIntegerExact();
					} catch (ArithmeticException e) {
						stack.push("Number value must be an integer.");
						return false;
					}
				}
				return true;
			}
		}
	}

	public class ArrayValidator implements Validator {

		private Long minItems;
		private Long maxItems;
		private String[] allowedTypes;

		public ArrayValidator() {
			this(null, null, new String[]{"any"});
		}

		public ArrayValidator(Long minItems, Long maxItems, String[] allowedTypes) {
			this.minItems = minItems;
			this.maxItems = maxItems;
			this.allowedTypes = allowedTypes;
		}

		@Override
		public JsonType getType() {
			return JsonType.ARRAY;
		}

		public Long getMinItems() {
			return minItems;
		}

		public Long getMaxItems() {
			return maxItems;
		}

		public Validator[] getAllowedTypes() {
			Validator[] resultArry = new Validator[allowedTypes.length];
			for (int i = 0; i < allowedTypes.length; i++) {
				resultArry[i] = definedTypes.get(allowedTypes[i]);
			}
			return resultArry;
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			if (!(value instanceof JsonArray)) {
				stack.push("Expect an array");
				return false;
			}
			JsonArray array = (JsonArray) value;
			if (minItems != null && array.size() < minItems) {
				stack.push("Array items are too few, expect greater than or equal " + minItems);
				return false;
			}

			if (maxItems != null && array.size() > maxItems) {
				stack.push("Array items are too much, expect less than or equal " + maxItems);
				return false;
			}

			for (int i = 0; i < array.size(); i++) {
				JsonElement subItem = array.get(i);
				boolean succeeded = false;
				checktype:
				for (String type : allowedTypes) {
					if (type.equals("any")
							|| definedTypes.get(type).validate(subItem, stack)) {
						stack.reset();
						succeeded = true;
						break;
					}
				}
				if (!succeeded) {
					stack.push("Array item type invalid, position: " + i);
					return false;
				}
			}
			return true;
		}
	}

	public class ObjectValidator implements Validator {

		private HashMap<String, String[]> properties;
		private boolean isAllowOtherProperties;

		public ObjectValidator() {
			this(new HashMap<String, String[]>(), true);
		}

		public ObjectValidator(HashMap<String, String[]> properties, boolean allowOtherProperties) {
			this.properties = properties;
			this.isAllowOtherProperties = allowOtherProperties;
		}

		@Override
		public JsonType getType() {
			return JsonType.OBJECT;
		}

		public Set<Entry<String, Validator[]>> getProperties() {
			HashMap<String, Validator[]> resultProperties = new HashMap<>();
			for (Entry<String, String[]> entry : properties.entrySet()) {
				String[] propertyArray = entry.getValue();
				Validator[] validatorArray = new Validator[propertyArray.length];
				for (int i = 0; i < propertyArray.length; i++) {
					validatorArray[i] = definedTypes.get(propertyArray[i]);
				}
				resultProperties.put(entry.getKey(), validatorArray);
			}
			return resultProperties.entrySet();
		}

		@Override
		public boolean validate(JsonElement value, ErrorStack stack) {
			if (!(value instanceof JsonObject)) {
				stack.push("Expect an object");
				return false;
			}
			JsonObject object = (JsonObject) value;
			Set<Entry<String, JsonElement>> entrySet = object.entrySet();
			for (Entry<String, JsonElement> entry : entrySet) {
				if (properties.containsKey(entry.getKey())) {
					String[] allowedSchemas = properties.get(entry.getKey());
					boolean succeeded = false;
					checktype:
					for (String type : allowedSchemas) {
						if (type.equals("any")
								|| definedTypes.get(type).validate(entry.getValue(), stack)) {
							stack.reset();
							succeeded = true;
							break;
						}
					}
					if (!succeeded) {
						stack.push("Property type invalid: " + entry.getKey());
						return false;
					}
				} else {
					if (!isAllowOtherProperties) {
						stack.push("Property not allowed: " + entry.getKey());
						return false;
					}
				}
			}
			for (Entry<String, String[]> entry : properties.entrySet()) {
				String[] schemas = entry.getValue();
				boolean allowNull = false;
				checkexists:
				for (String schema : schemas) {
					if (schema.equals("null") || schema.equals("any")) {
						allowNull = true;
						break;
					}
				}
				if (!allowNull) {
					if (!object.has(entry.getKey())) {
						stack.push("Property required: " + entry.getKey());
						return false;
					}
				}
			}
			return true;
		}
	}

	private HashMap<String, Validator> definedTypes;
	private HashSet<String> rootTypes;

	public Validator[] getRootTypes() {
		LinkedList<Validator> validators = new LinkedList<>();
		for (String typeName : rootTypes) {
			validators.add(definedTypes.get(typeName));
		}
		return validators.toArray(new Validator[validators.size()]);
	}

	public JsonSchema(InputStream jsonStream, String charset) throws JsonValidateException, IOException {
		InputStreamReader reader = new InputStreamReader(jsonStream, charset);
		internalInit(reader);
	}

	public JsonSchema(String resourceName, String charset) throws JsonValidateException, IOException {
		InputStream in = null;
		try {
			in = JsonSchema.class.getClassLoader().getResourceAsStream(resourceName);
			InputStreamReader reader = new InputStreamReader(in, charset);
			internalInit(reader);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public JsonSchema(String jsonString) throws JsonValidateException {
		StringReader reader = new StringReader(jsonString);
		internalInit(reader);
	}

	public JsonSchema(Reader jsonReader) throws JsonValidateException {
		internalInit(jsonReader);
	}

	private void internalInit(Reader jsonReader) throws JsonValidateException {
		try {
			JsonParser parser = new JsonParser();
			JsonObject root = (JsonObject) parser.parse(new JsonReader(jsonReader));
			HashMap<String, String> checkList
					= new HashMap<>();
			definedTypes = new HashMap<>();
			rootTypes = new HashSet<>();

			definedTypes.put("any", new AnyValidator());
			definedTypes.put("null", new NullValidator());
			definedTypes.put("boolean", new BooleanValidator());
			definedTypes.put("string", new StringValidator());
			definedTypes.put("number", new NumberValidator());
			definedTypes.put("object", new ObjectValidator());
			definedTypes.put("array", new ArrayValidator());

			HashSet<String> preDefined = new HashSet<>();
			preDefined.add("any");
			preDefined.add("null");
			preDefined.add("boolean");
			preDefined.add("string");
			preDefined.add("number");
			preDefined.add("object");
			preDefined.add("array");

			JsonObject define = (JsonObject) root.get("define");
			if (define != null) {
				for (Entry<String, JsonElement> entry : define.entrySet()) {
					JsonObject definedType = (JsonObject) entry.getValue();
					if (definedTypes.containsKey(entry.getKey())) {
						throw new JsonValidateException("Schema type redefined: " + entry.getKey());
					}
					JsonPrimitive primitiveType = (JsonPrimitive) definedType.get("type");
					if (primitiveType == null || !primitiveType.isString()) {
						throw new JsonValidateException("Schema type define error, must specify 'type': " + entry.getKey());
					}
					String typeName = primitiveType.getAsString();
					switch (typeName) {
						case "string": {
							JsonArray options = (JsonArray) definedType.get("options");
							if (options != null) {
								// An string enumerate
								String[] optionArray = new String[options.size()];
								for (int i = 0; i < options.size(); i++) {
									JsonPrimitive item = (JsonPrimitive) options.get(i);
									if (!item.isString()) {
										throw new JsonValidateException("String options must be a string: " + entry.getKey());
									}
									optionArray[i] = item.getAsString();
								}
								StringValidator stringValidator = new StringValidator(optionArray);
								definedTypes.put(entry.getKey(), stringValidator);
							} else {
								JsonPrimitive value;
								Long min = null;
								value = (JsonPrimitive) definedType.get("min");
								if (value != null) {
									if (value.isNumber()) {
										try {
											min = value.getAsBigDecimal().longValueExact();
										} catch (ArithmeticException e) {
											throw new JsonValidateException("String 'min' length must be an integer: " + entry.getKey());
										}
									} else {
										throw new JsonValidateException("String 'min' length must be an integer: " + entry.getKey());
									}
								}
								Long max = null;
								value = (JsonPrimitive) definedType.get("max");
								if (value != null) {
									if (value.isNumber()) {
										try {
											max = value.getAsBigDecimal().longValueExact();
										} catch (ArithmeticException e) {
											throw new JsonValidateException("String 'max' length must be an integer: " + entry.getKey());
										}
									} else {
										throw new JsonValidateException("String 'max' length must be an integer: " + entry.getKey());
									}
								}
								String match = null;
								value = (JsonPrimitive) definedType.get("match");
								if (value != null) {
									if (value.isString()) {
										match = value.getAsString();
									} else {
										throw new JsonValidateException("String 'match' must be a regex expression: " + entry.getKey());
									}
								}
								StringValidator stringValidator = new StringValidator(match, min, max);
								definedTypes.put(entry.getKey(), stringValidator);
							}
							break;
						}
						case "number": {
							JsonArray options = (JsonArray) definedType.get("options");
							if (options != null) {
								// An string enumerate
								BigDecimal[] optionArray = new BigDecimal[options.size()];
								for (int i = 0; i < options.size(); i++) {
									JsonPrimitive item = (JsonPrimitive) options.get(i);
									if (!item.isNumber()) {
										throw new JsonValidateException("Number options must be a number: " + entry.getKey());
									}
									optionArray[i] = item.getAsBigDecimal();
								}
								NumberValidator numberValidator = new NumberValidator(optionArray);
								definedTypes.put(entry.getKey(), numberValidator);
							} else {
								JsonPrimitive value;
								BigDecimal min = null;
								value = (JsonPrimitive) definedType.get("min");
								if (value != null) {
									if (value.isNumber()) {
										min = value.getAsBigDecimal();
									} else {
										throw new JsonValidateException("Number 'min' value must be a number: " + entry.getKey());
									}
								}
								BigDecimal max = null;
								value = (JsonPrimitive) definedType.get("max");
								if (value != null) {
									if (value.isNumber()) {
										max = value.getAsBigDecimal();
									} else {
										throw new JsonValidateException("Number 'max' value must be a number: " + entry.getKey());
									}
								}
								boolean mustInteger = false;
								value = (JsonPrimitive) definedType.get("integer");
								if (value != null) {
									if (value.isBoolean()) {
										mustInteger = value.getAsBoolean();
									} else {
										throw new JsonValidateException("Number 'integer' must be a boolean value: " + entry.getKey());
									}
								}
								NumberValidator numberValidator = new NumberValidator(min, max, mustInteger);
								definedTypes.put(entry.getKey(), numberValidator);
							}
							break;
						}
						case "array": {
							JsonPrimitive value;
							Long min = null;
							value = (JsonPrimitive) definedType.get("min");
							if (value != null) {
								if (value.isNumber()) {
									try {
										min = value.getAsBigDecimal().longValueExact();
									} catch (ArithmeticException e) {
										throw new JsonValidateException("Array 'min' size must be an integer: " + entry.getKey());
									}
								} else {
									throw new JsonValidateException("Array 'min' size must be an integer: " + entry.getKey());
								}
							}
							Long max = null;
							value = (JsonPrimitive) definedType.get("max");
							if (value != null) {
								if (value.isNumber()) {
									try {
										max = value.getAsBigDecimal().longValueExact();
									} catch (ArithmeticException e) {
										throw new JsonValidateException("Array 'max' size must be an integer: " + entry.getKey());
									}
								} else {
									throw new JsonValidateException("Array 'max' size must be an integer: " + entry.getKey());
								}
							}
							String[] allow;
							JsonArray array = (JsonArray) definedType.get("allow");
							if (array != null) {
								allow = new String[array.size()];
								for (int i = 0; i < array.size(); i++) {
									if (!array.get(i).isJsonPrimitive()) {
										throw new JsonValidateException("Array 'allow' type must be an array of string: " + entry.getKey());
									}
									value = (JsonPrimitive) array.get(i);
									if (!value.isString()) {
										throw new JsonValidateException("Array 'allow' type must be an array of string: " + entry.getKey());
									}
									allow[i] = value.getAsString();
									if (!checkList.containsKey(allow[i])) {
										checkList.put(allow[i], "Type not define, used by: " + entry.getKey());
									}
								}
							} else {
								allow = new String[]{"any"};
							}
							ArrayValidator arrayValidator = new ArrayValidator(min, max, allow);
							definedTypes.put(entry.getKey(), arrayValidator);
							break;
						}
						case "object": {
							boolean scalable = true;
							JsonPrimitive value;
							value = (JsonPrimitive) definedType.get("scalable");
							if (value != null) {
								if (value.isBoolean()) {
									scalable = value.getAsBoolean();
								} else {
									throw new JsonValidateException("Object 'scalable' must be a boolean: " + entry.getKey());
								}
							}
							HashMap<String, String[]> properties = new HashMap<>();
							JsonObject propertyDefine = (JsonObject) definedType.get("properties");
							if (propertyDefine != null) {
								for (Entry<String, JsonElement> propertyItem : propertyDefine.entrySet()) {
									String key = propertyItem.getKey();
									JsonArray array = (JsonArray) propertyItem.getValue();
									String[] allow = new String[array.size()];
									for (int i = 0; i < array.size(); i++) {
										if (!array.get(i).isJsonPrimitive()) {
											throw new JsonValidateException("Object property type must be an array of string: " + entry.getKey());
										}
										value = (JsonPrimitive) array.get(i);
										if (!value.isString()) {
											throw new JsonValidateException("Object property type must be an array of string: " + entry.getKey());
										}
										allow[i] = value.getAsString();
										if (!checkList.containsKey(allow[i])) {
											checkList.put(allow[i], "Type not define, used by: " + entry.getKey());
										}
									}
									properties.put(key, allow);
								}
							}
							ObjectValidator objectValidator = new ObjectValidator(properties, scalable);
							definedTypes.put(entry.getKey(), objectValidator);
							break;
						}
					}
				}
			}
			JsonArray jsonRoot = (JsonArray) root.get("root");
			if (jsonRoot == null) {
				throw new JsonValidateException("Schema must define a root node.");
			}

			for (int i = 0; i < jsonRoot.size(); i++) {
				JsonPrimitive value = (JsonPrimitive) jsonRoot.get(i);
				if (!value.isString()) {
					throw new JsonValidateException("Schema root must be an array of string.");
				}
				rootTypes.add(value.getAsString());
				if (!checkList.containsKey(value.getAsString())) {
					checkList.put(value.getAsString(), "Type not define, used by root.");
				}
			}

			if (rootTypes.isEmpty()) {
				throw new JsonValidateException("Schema root must at least specify one type.");
			}

			for (Entry<String, String> entry : checkList.entrySet()) {
				if (!definedTypes.containsKey(entry.getKey())) {
					throw new JsonValidateException(entry.getValue());
				}
			}

			for (Entry<String, Validator> entry : definedTypes.entrySet()) {
				String key = entry.getKey();
				if (!checkList.containsKey(key) && !preDefined.contains(key)) {
					System.out.println("WARN: JSON schema type was defined but not used: " + key);
				}
			}

		} catch (Exception e) {
			throw new JsonValidateException("JSON schema error", e);
		}
	}

	public JsonElement validate(String jsonString) throws JsonValidateException {
		StringReader reader = new StringReader(jsonString);
		return validate(reader);
	}

	public JsonElement validate(InputStream jsonStream, String charset) throws JsonValidateException, IOException {
		InputStreamReader reader = new InputStreamReader(jsonStream, charset);
		return validate(reader);
	}

	public JsonElement validate(Reader reader) throws JsonValidateException {
		try {
			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(new JsonReader(reader));
			Validator[] types = getRootTypes();
			ErrorStack stack = new ErrorStack();
			for (Validator type : types) {
				if (type.validate(jsonElement, stack)) {
					return jsonElement;
				}
			}
			throw new JsonValidateException("JSON validate failed: " + stack.getMessage());
		} catch (JsonSyntaxException e) {
			throw new JsonValidateException("JSON validate failed", e);
		}
	}
}
