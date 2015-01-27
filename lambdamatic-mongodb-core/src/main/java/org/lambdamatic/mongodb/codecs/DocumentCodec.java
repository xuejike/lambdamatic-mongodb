/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.lambdamatic.mongodb.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonElement;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.types.ObjectId;
import org.lambdamatic.mongodb.types.geospatial.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Xavier Coulon <xcoulon@redhat.com>
 *
 */
public class DocumentCodec<T> implements Codec<T> {

	/** The Logger name to use when logging conversion results.*/
	static final String LOGGER_NAME = DocumentCodec.class.getName();
	
	/** The usual Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

	/** Name of the field used to store the document id in MongoDB. */
	public static final String MONGOBD_DOCUMENT_ID = "_id";

	/**
	 * Name of the field used to handle the Java type associated with a document
	 * in MongoDB.
	 */
	public static final String TARGET_CLASS_FIELD = "_targetClass";

	/** The user-defined domain class associated with this Codec. */
	private final Class<T> targetClass;
	
	/** the codec registry, to decode elements of an incoming BSON document. */
	protected final CodecRegistry codecRegistry;

	/** The binding service. */
	private final BindingService bindingService;
	
	/**
	 * Constructor
	 * 
	 * @param targetClass
	 *            the domain class supported by this {@link Codec}.
	 * @param codecRegistry
	 *            the associated {@link CodecRegistry}
	 * @param bindingService
	 *            the Binding Service.
	 */
	public DocumentCodec(final Class<T> targetClass, final CodecRegistry codecRegistry, final BindingService bindingService) {
		this.targetClass = targetClass;
		this.codecRegistry = codecRegistry;
		this.bindingService = bindingService;
	}

	/**
	 * @return the {@link BindingService} of this {@link DocumentCodec}.
	 */
	public BindingService getBindingService() {
		return bindingService;
	}
	
	@Override
	public Class<T> getEncoderClass() {
		return targetClass;
	}
	
	
	/**
	 * Encodes the given {@code domainObject}, putting the {@code _id} attribute first, followed by {@code _targetClassName} to 
	 * be able to decode the BSON/JSON document later, followed by the other annotated Java attributes.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void encode(final BsonWriter writer, final T domainObject, final EncoderContext encoderContext) {
		if (LOGGER.isDebugEnabled()) {
			try {
				// use an intermediate JsonWriter whose Outputstream can be
				// retrieved
				final ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
				final BsonWriter debugWriter = new JsonWriter(new OutputStreamWriter(jsonOutputStream, "UTF-8"));
				encodeDomainObject(debugWriter, domainObject, encoderContext);
				final String jsonContent = IOUtils.toString(jsonOutputStream.toByteArray(), "UTF-8");
				LOGGER.debug("Encoded document: {}", jsonContent);
				// now, write the document in the target writer
				final JsonReader jsonContentReader = new JsonReader(jsonContent);
				writer.pipe(jsonContentReader);
				writer.flush();
			} catch (IOException e) {
				throw new ConversionException("Failed to convert '" + domainObject.toString()
						+ "' to a BSON document", e);
			}
		} else {
			encodeDomainObject(writer, domainObject, encoderContext);
		}
	}

	/**
	 * Converts the BSON Documentation provided by the given {@link BsonReader}
	 * into an instance of user-domain class. The user-domain specific class is
	 * found in the {@link DocumentCodec#TARGET_CLASS_FIELD} field,
	 * otherwise, the code assumes it should return an instance of type
	 * {@code T}, where {@code T} is the parameter type of this {@link Codec}).
	 * <p>Note: target class {@code T} should have a default constructor (this may be improved in further version)</p>
	 */
	@Override
	public T decode(final BsonReader reader, final DecoderContext decoderContext) {
		// code duplicated and adapted from "org.bson.codecs.BsonDocumentCodec"
		final Map<String, BsonElement> keyValuePairs = new HashMap<>();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            keyValuePairs.put(fieldName, new BsonElement(fieldName, readValue(reader, decoderContext)));
        }
        reader.readEndDocument();
        // now, convert the map key-pairs into an instance of the target document 
		final T domainDocument = getTargetClass(keyValuePairs);
		final Map<String, Field> bindings = bindingService.getBindings(domainDocument.getClass());
		for (Iterator<String> iterator = keyValuePairs.keySet().iterator(); iterator.hasNext();) {
			final String key = iterator.next();
			final Field field = bindings.get(key);
			if (field == null) {
				LOGGER.debug("Field '{}' does not exist in class '{}'", key, targetClass.getName());
				continue;
			}
			final Object fieldValue = getValue(keyValuePairs.get(key), field.getType());
			try {
				field.setAccessible(true);
				field.set(domainDocument, fieldValue);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ConversionException("Unable to set value '" + fieldValue + "' to field '" + domainDocument.getClass().getName() + "." + field.getName() + "'", e);
			}
		}
        return domainDocument;
	}
	
	/**
	 * Instantiate the target user-domain Document if the given
	 * {@code keyPairValues} contains a field named
	 * {@link DocumentCodec#TARGET_CLASS_FIELD}, otherwise use the
	 * default {@code targetClass} provided in the constructor of this codec.
	 * 
	 * @param keyValuePairs the key-value pairs to use
	 * @return the instance of the target user-domain Document
	 */
	@SuppressWarnings("unchecked")
	private T getTargetClass(final Map<String, BsonElement> keyValuePairs) {
		final String targetClassName = getTargetClassName(keyValuePairs);
		try {
			return (T) Class.forName(targetClassName).newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new ConversionException("Failed to create a new instance of '" + targetClassName + "'", e);
		}
	}

	/**
	 * Retrieves the fully qualified name of the target Document if the given
	 * {@code keyPairValues} contains a field named
	 * {@link DocumentCodec#TARGET_CLASS_FIELD}, otherwise use the
	 * default {@code targetClass} provided in the constructor of this codec.
	 * 
	 * @param keyValuePairs the key-value pairs to use
	 * @return the fully qualified name of the class to instantiate
	 */
	private String getTargetClassName(final Map<String, BsonElement> keyValuePairs) {
		if (keyValuePairs.containsKey(TARGET_CLASS_FIELD)) {
			final BsonElement targetClassElement = keyValuePairs.get(TARGET_CLASS_FIELD);
			if (targetClassElement.getValue().isString()) {
				return targetClassElement.getValue().asString().getValue();
			}
		}
		return this.targetClass.getName();
	}
	
	

	/**
	 * @param writer
	 */
	protected void writeValue(final BsonWriter writer, final String attributeName, final Object attributeValue, final EncoderContext encoderContext) {
		if(attributeValue == null) {
			return;
		}
		if (attributeValue.getClass().isEnum()) {
			writer.writeString(attributeName, ((Enum<?>) attributeValue).name());
		} else {
			// FIXME: complete the switches to cover all writer.writeXXX methods
			final String attributeValueClassName = attributeValue.getClass().getName();
			switch(attributeValueClassName) {
			case "org.bson.types.ObjectId":
				writer.writeObjectId(MONGOBD_DOCUMENT_ID, (ObjectId) attributeValue);
				break;
			case "java.lang.Boolean": // also covers "boolean"
				final boolean booleanValue = (boolean) attributeValue;
				if(booleanValue != false) {
					writer.writeBoolean(attributeName, booleanValue);
				}
				break;
			case "java.lang.Byte": // covers also "int" with is autoboxed
				final byte byteValue = (byte) attributeValue;
				if(byteValue != 0) {
					writer.writeInt32(attributeName, byteValue);
				}
				break;
			case "java.lang.Short": // covers also "int" with is autoboxed
				final short shortValue = (short) attributeValue;
				if(shortValue != 0) {
					writer.writeInt32(attributeName, shortValue);
				}
				break;
			case "java.lang.Character": // covers also "int" with is autoboxed
				final char charValue = (char) attributeValue;
				if(charValue != 0) {
					writer.writeInt32(attributeName, charValue);
				}
				break;
			case "java.lang.Integer": // covers also "int" with is autoboxed
				final int intValue = (int) attributeValue;
				if(intValue != 0) {
					writer.writeInt32(attributeName, intValue);
				}
				break;
			case "java.lang.Long": // covers also "long" with is autoboxed
				final long longValue = (long) attributeValue;
				if(longValue != 0) {
					writer.writeInt64(attributeName, longValue);
				}
				break;
			case "java.lang.Float": // covers also "float" with is autoboxed
				final float floatValue = (float) attributeValue;
				if(floatValue != 0) {
					writer.writeDouble(attributeName, floatValue);
				}
				break;
			case "java.lang.Double": // covers also "double" with is autoboxed
				final double doubleValue = (double) attributeValue;
				if(doubleValue != 0) {
					writer.writeDouble(attributeName, doubleValue);
				}
				break;
			case "java.lang.String":
				writer.writeString(attributeName, (String) attributeValue);
				break;
			case "org.lambdamatic.mongodb.types.geospatial.Location":
				writer.writeStartDocument(attributeName);
				new LocationCodec(codecRegistry, bindingService).encode(writer, (Location)attributeValue, encoderContext);
				writer.writeEndDocument();
				break;
			// assume this is an embedded document
			default: 
				encodeDomainObject(writer, attributeName, attributeValue, encoderContext);
			}
		}
	}

	void encodeDomainObject(final BsonWriter writer, final Object domainObject, final EncoderContext encoderContext) {
		try {
			writer.writeStartDocument();
			encodeDomainObjectContent(writer, domainObject, encoderContext);
			writer.writeEndDocument();
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ConversionException("Failed to convert following domain object to BSON document: " + domainObject, e);
		} finally {
			writer.flush();
		}
	}

	void encodeDomainObject(final BsonWriter writer, final String documentName, final Object domainObject, final EncoderContext encoderContext) {
		try {
			writer.writeStartDocument(documentName);
			encodeDomainObjectContent(writer, domainObject, encoderContext);
			writer.writeEndDocument();
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ConversionException("Failed to convert following domain object to BSON document: " + domainObject, e);
		} finally {
			writer.flush();
		}
	}

	private void encodeDomainObjectContent(final BsonWriter writer, final Object domainObject,
			final EncoderContext encoderContext) throws IllegalAccessException {
		final Map<String, Field> bindings = bindingService.getBindings(domainObject.getClass());
		// write the "_id" attribute first
		final Optional<Entry<String, Field>> idBinding = bindings.entrySet().stream().filter(e -> bindingService.isIdBinding(e)).findFirst();
		if(idBinding.isPresent()) {
			final Object idValue = bindingService.getBindingValue(domainObject, idBinding.get().getValue());
			if(idValue == null) {
				final ObjectId generatedIdValue = new ObjectId();
				idBinding.get().getValue().set(domainObject, generatedIdValue);
				writeValue(writer, MONGOBD_DOCUMENT_ID, generatedIdValue, encoderContext);	
			} else {
				writeValue(writer, MONGOBD_DOCUMENT_ID, idValue, encoderContext);	
			}
		}
		// write the technical/inner "_targetClassName" attribute
		writer.writeString(TARGET_CLASS_FIELD, domainObject.getClass().getName());
		// write other attributes
		bindings.entrySet().stream().filter(e -> !bindingService.isIdBinding(e)).forEach(
				binding -> {
					writeValue(writer, binding.getKey(), bindingService.getBindingValue(domainObject, binding.getValue()), encoderContext);
				});
	}
	
	/**
	 * Returns the value of the given {@code bsonElement} converted to the given {@code fieldType}.
	 * @param bsonElement the {@link BsonElement} to inspect
	 * @param expectedType the {@link Class} of the value to return
	 * @return the wrapped value or {@code null} if the given {@code bsonElement} is {@code null}
	 */
	protected Object getValue(final BsonElement bsonElement, final Class<?> expectedType) {
		if(bsonElement != null) {
			final Object bsonValue = getValue(bsonElement.getValue());
			return convert(bsonValue, expectedType);
		} 
		return null;
	}

	/**
	 * Converts the given {@code value} to the given {@code type}
	 * @param targetType the target type
	 * @param value the value to convert
	 * @return the converted value
	 */
	Object convert(final Object value, final Class<?> targetType) {
		if(targetType.isEnum() && value instanceof String) {
			for(Object e : targetType.getEnumConstants()) {
				if(e.toString().equals((String)value)) {
					return e;
				}
			}
		} else if(targetType.isArray()) {
			final Class<?> componentType = targetType.getComponentType();
			final List<Object> values = new ArrayList<>();
			for(Object v : (Collection<?>)value) {
				values.add(getValue((BsonValue)v));
			}
			return values.toArray((Object[])java.lang.reflect.Array.newInstance(componentType, values.size()));
		}
		
		//FIXME: missing switches. Can we write it differently ?
		switch (targetType.getName()) {
		case "boolean":
		case "java.lang.Boolean":
			return Boolean.parseBoolean(value.toString());
		case "byte":
		case "java.lang.Byte":
			return Byte.parseByte(value.toString());
		case "short":
		case "java.lang.Short":
			return Short.parseShort(value.toString());
		case "char":
		case "java.lang.Character":
			return new Character((char)value);
		case "int":
		case "java.lang.Integer":
			return Integer.parseInt(value.toString());
		case "long":
		case "java.lang.Long":
			return Long.parseLong(value.toString());
		case "float":
		case "java.lang.Float":
			return Float.parseFloat(value.toString());
		case "double":
		case "java.lang.Double":
			return Double.parseDouble(value.toString());
		case "java.lang.String":
			return value.toString();
		case "org.bson.types.ObjectId":
			return new ObjectId(value.toString());
		case "org.lambdamatic.mongodb.types.geospatial.Location":
			return new LocationCodec(this.codecRegistry, bindingService).decode(new BsonDocumentReader((BsonDocument) value), DecoderContext.builder().build());
		}
		throw new ConversionException("Unable to convert value '" + value + "' to type " + targetType.getName());
	}
	
	/**
	 * Returns the value of the given {@link BsonElement}.
	 * @param bsonElement the element to inspect
	 * @return the wrapped value
	 */
	private Object getValue(final BsonValue bsonValue) {
		if(bsonValue != null) {
			switch(bsonValue.getBsonType()) {
			case ARRAY:
				return bsonValue.asArray().getValues();
			case BINARY:
				return bsonValue.asBinary().getData();
			case BOOLEAN:
				return bsonValue.asBoolean().getValue();
			case DATE_TIME:
				return bsonValue.asDateTime().getValue();
			case DB_POINTER:
				return bsonValue.asDBPointer().getId();
			case DOUBLE:
				return bsonValue.asDouble().getValue();
			case INT32:
				return bsonValue.asInt32().getValue();
			case INT64:
				return bsonValue.asInt64().getValue();
			case JAVASCRIPT:
				return bsonValue.asJavaScript().getCode();
			case JAVASCRIPT_WITH_SCOPE:
				return bsonValue.asJavaScriptWithScope().getCode();
			case NULL:
				return null;
			case OBJECT_ID:
				return bsonValue.asObjectId().getValue();
			case REGULAR_EXPRESSION:
				return bsonValue.asRegularExpression().getPattern();
			case STRING:
				return bsonValue.asString().getValue();
			case SYMBOL:
				return bsonValue.asSymbol().getSymbol();
			case TIMESTAMP:
				return bsonValue.asTimestamp().getTime();
			case DOCUMENT:
				// the bsonValue (a BsonDocument) will be processed later
				return bsonValue;
			case END_OF_DOCUMENT:
			case UNDEFINED:
			case MAX_KEY:
			case MIN_KEY:
			default:
				throw new ConversionException("Unexpected BSON Element value of type '" + bsonValue.getBsonType() + "'");
			}
		}
		return null;
	}

	/**
	 * Returns the value of the current reader position 
	 *
	 * @param reader         the read to read the value from
	 * @param decoderContext the context
	 * @return the non-null value read from the reader
	 */
	protected BsonValue readValue(final BsonReader reader, final DecoderContext decoderContext) {
	    final Class<? extends BsonValue> classForBsonType = BsonValueCodecProvider.getClassForBsonType(reader.getCurrentBsonType());
		final Codec<? extends BsonValue> codec = codecRegistry.get(classForBsonType);
		return codec.decode(reader, decoderContext);
	}
	
}