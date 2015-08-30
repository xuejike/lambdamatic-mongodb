/**
 * 
 */
package org.lambdamatic.mongodb.apt;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.lambdamatic.mongodb.annotations.DocumentField;
import org.lambdamatic.mongodb.apt.testutil.ClassAssertion;
import org.lambdamatic.mongodb.apt.testutil.CompilationAndAnnotationProcessingRule;
import org.lambdamatic.mongodb.apt.testutil.FieldAssertion;
import org.lambdamatic.mongodb.apt.testutil.WithDomainClass;
import org.lambdamatic.mongodb.internal.LambdamaticMongoCollectionImpl;
import org.lambdamatic.mongodb.internal.codecs.EncoderUtils;
import org.lambdamatic.mongodb.internal.configuration.MongoClientConfiguration;
import org.lambdamatic.mongodb.metadata.LocationField;
import org.lambdamatic.mongodb.metadata.ProjectionArray;
import org.lambdamatic.mongodb.metadata.ProjectionField;
import org.lambdamatic.mongodb.metadata.ProjectionMap;
import org.lambdamatic.mongodb.metadata.ProjectionMetadata;
import org.lambdamatic.mongodb.metadata.QueryArray;
import org.lambdamatic.mongodb.metadata.QueryField;
import org.lambdamatic.mongodb.metadata.QueryMap;
import org.lambdamatic.mongodb.metadata.QueryMetadata;

import com.mongodb.MongoClient;
import com.sample.Bar;
import com.sample.Baz;
import com.sample.EnumBar;
import com.sample.EnumFoo;
import com.sample.Foo;

/**
 * Testing the {@link DocumentAnnotationProcessor}
 * 
 * @author Xavier Coulon <xcoulon@redhat.com>
 *
 */
public class LambdamaticAnnotationProcessorTest {

	@Rule
	public CompilationAndAnnotationProcessingRule generateMetdataClassesRule = new CompilationAndAnnotationProcessingRule();

	@Test
	@WithDomainClass(Foo.class)
	@WithDomainClass(Bar.class)
	public void shouldProcessFooClassAndGenerateQueryMetadataClass() throws URISyntaxException, ClassNotFoundException,
			NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException {
		// verification
		final Class<?> fooQueryClass = Class.forName("com.sample.QFoo");
		ClassAssertion.assertThat(fooQueryClass).isAbstract().isImplementing(QueryMetadata.class, Foo.class);
		// id
		FieldAssertion.assertThat(fooQueryClass, "id").isParameterizedType(QueryField.class, ObjectId.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", EncoderUtils.MONGOBD_DOCUMENT_ID);
		// stringField: *custom name in the @DocumentField annotation*
		FieldAssertion.assertThat(fooQueryClass, "stringField").isParameterizedType(QueryField.class, String.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "stringField_");
		// transientStringField: annotated with @Transient, should not be here
		ClassAssertion.assertThat(fooQueryClass).hasNoField("transientStringField");
		// primitiveByteField: on @DocumentField annotation: should use class field name as document field name
		FieldAssertion.assertThat(fooQueryClass, "primitiveByteField").isParameterizedType(QueryField.class, Byte.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "primitiveByteField");
		// byteField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "byteField").isParameterizedType(QueryField.class, Byte.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "byteField");
		// primitiveShortField
		FieldAssertion.assertThat(fooQueryClass, "primitiveShortField")
				.isParameterizedType(QueryField.class, Short.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveShortField");
		// shortField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "shortField").isParameterizedType(QueryField.class, Short.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "shortField");
		// primitiveIntField
		FieldAssertion.assertThat(fooQueryClass, "primitiveIntField")
				.isParameterizedType(QueryField.class, Integer.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveIntField");
		// integerField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "integerField").isParameterizedType(QueryField.class, Integer.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "integerField");
		// primitiveLongField
		FieldAssertion.assertThat(fooQueryClass, "primitiveLongField").isParameterizedType(QueryField.class, Long.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "primitiveLongField");
		// longField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "longField").isParameterizedType(QueryField.class, Long.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "longField");
		// primitiveFloatField
		FieldAssertion.assertThat(fooQueryClass, "primitiveFloatField")
				.isParameterizedType(QueryField.class, Float.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveFloatField");
		// floatField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "floatField").isParameterizedType(QueryField.class, Float.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "floatField");
		// primitiveDoubleField
		FieldAssertion.assertThat(fooQueryClass, "primitiveDoubleField")
				.isParameterizedType(QueryField.class, Double.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveDoubleField");
		// doubleField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "doubleField").isParameterizedType(QueryField.class, Double.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "doubleField");
		// primitiveBooleanField
		FieldAssertion.assertThat(fooQueryClass, "primitiveBooleanField")
				.isParameterizedType(QueryField.class, Boolean.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveBooleanField");
		// booleanField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "booleanField").isParameterizedType(QueryField.class, Boolean.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "booleanField");
		// primitiveCharField
		FieldAssertion.assertThat(fooQueryClass, "primitiveCharField")
				.isParameterizedType(QueryField.class, Character.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveCharField");
		// characterField: metadata field is primitive type instead of class wrapper
		FieldAssertion.assertThat(fooQueryClass, "characterField")
				.isParameterizedType(QueryField.class, Character.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "characterField");
		// location
		FieldAssertion.assertThat(fooQueryClass, "location").isType(LocationField.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "location");
		// enumFoo
		FieldAssertion.assertThat(fooQueryClass, "enumFoo").isParameterizedType(QueryField.class, EnumFoo.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "enumFoo");
		// bar
		FieldAssertion.assertThat(fooQueryClass, "bar").isType("com.sample.QBar").isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "bar");
		// barList
		FieldAssertion.assertThat(fooQueryClass, "barList").isParameterizedType(QueryArray.class, ClassUtils.getClass("com.sample.QBar")).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "barList");
		// barMap
		FieldAssertion.assertThat(fooQueryClass, "barMap").isParameterizedType(QueryMap.class, String.class, Class.forName("com.sample.QBar")).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "barMap");
		// enumBarArray
		FieldAssertion.assertThat(fooQueryClass, "enumBarArray")
				.isParameterizedType(QueryArray.class, ClassUtils.getClass("com.sample.EnumBar")).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "enumBarArray");
		// stringSet
		FieldAssertion.assertThat(fooQueryClass, "stringSet").isParameterizedType(QueryArray.class, TypeUtils.parameterize(QueryField.class, String.class)).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "stringSet");
		// stringMap
		FieldAssertion.assertThat(fooQueryClass, "stringMap")
				.isParameterizedType(QueryMap.class, String.class, TypeUtils.parameterize(QueryField.class, String.class)).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "stringMap");
	}

	@Test
	@WithDomainClass(Foo.class)
	@WithDomainClass(Bar.class)
	public void shouldProcessFooClassAndGenerateProjectionMetadataClass()
			throws URISyntaxException, ClassNotFoundException, NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException, InstantiationException {
		// verification
		final Class<?> fooProjectionClass = Class.forName("com.sample.PFoo");
		ClassAssertion.assertThat(fooProjectionClass).isAbstract().isImplementing(ProjectionMetadata.class, Foo.class)
				.isExtending("java.lang.Object");
		// id
		FieldAssertion.assertThat(fooProjectionClass, "id").isType(ProjectionField.class).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "_id");
		// stringField: *custom name in the @DocumentField annotation*
		FieldAssertion.assertThat(fooProjectionClass, "stringField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "stringField_");
		// transientStringField: annotated with @Transient, should not be here
		ClassAssertion.assertThat(fooProjectionClass).hasNoField("transientStringField");
		// primitiveByteField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveByteField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveByteField");

		// primitiveShortField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveShortField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveShortField");

		// primitiveIntField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveIntField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveIntField");

		// primitiveLongField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveLongField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveLongField");

		// primitiveFloatField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveFloatField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveFloatField");

		// primitiveDoubleField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveDoubleField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveDoubleField");

		// primitiveBooleanField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveBooleanField").isType(ProjectionField.class)
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "primitiveBooleanField");

		// primitiveCharField
		FieldAssertion.assertThat(fooProjectionClass, "primitiveCharField").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "primitiveCharField");

		// location
		FieldAssertion.assertThat(fooProjectionClass, "location").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "location");

		// enumFoo
		FieldAssertion.assertThat(fooProjectionClass, "enumFoo").isType(ProjectionField.class).isNotFinal()
				.isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "enumFoo");

		// bar
		FieldAssertion.assertThat(fooProjectionClass, "bar").isType("com.sample.PBar").isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "bar");
		// embedded PBar Projection class
		final Class<?> barProjectionClass = Class.forName("com.sample.PBar");
		ClassAssertion.assertThat(barProjectionClass).isImplementing(ProjectionMetadata.class, Bar.class);
		// barList
		FieldAssertion.assertThat(fooProjectionClass, "barList")
				.isParameterizedType(ProjectionArray.class, Class.forName("com.sample.QBar")).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "barList");
		// barMap
		FieldAssertion.assertThat(fooProjectionClass, "barMap")
				.isParameterizedType(ProjectionMap.class, String.class, Class.forName("com.sample.QBar")).isNotFinal().isNotStatic()
				.hasAnnotation(DocumentField.class).hasAttributeValue("name", "barMap");
		// enumBarArray
		FieldAssertion.assertThat(fooProjectionClass, "enumBarArray")
				.isParameterizedType(ProjectionArray.class, TypeUtils.parameterize(QueryField.class, EnumBar.class))
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class)
				.hasAttributeValue("name", "enumBarArray");
		// stringSet
		FieldAssertion.assertThat(fooProjectionClass, "stringSet")
				.isParameterizedType(ProjectionArray.class, TypeUtils.parameterize(QueryField.class, String.class))
				.isNotFinal().isNotStatic().hasAnnotation(DocumentField.class).hasAttributeValue("name", "stringSet");

	}

	@Test
	@WithDomainClass(Foo.class)
	public void shouldProcessSingleDomainClassAndGenerateCollectionAndCollectionProducer()
			throws URISyntaxException, ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
		// verification
		final Class<?> fooCollectionClass = Class.forName("com.sample.FooCollection");
		final Class<?> queryFooClass = Class.forName("com.sample.QFoo");
		final Class<?> projectionFooClass = Class.forName("com.sample.PFoo");
		final Class<?> updateFooClass = Class.forName("com.sample.UFoo");
		ClassAssertion.assertThat(fooCollectionClass).isExtending(LambdamaticMongoCollectionImpl.class, Foo.class,
				queryFooClass, projectionFooClass, updateFooClass);
		// should it rather provide a 'users' public field instead of a
		// getUsers() method ?
		final Class<?> fooCollectionProducerClass = Class.forName("com.sample.FooCollectionProducer");
		ClassAssertion.assertThat(fooCollectionProducerClass).hasMethod("getFooCollection", MongoClient.class,
				MongoClientConfiguration.class);

	}

	@Test
	@WithDomainClass(Foo.class)
	@WithDomainClass(Bar.class)
	@WithDomainClass(Baz.class)
	public void shouldProcessAllDomainClassesAndGenerateCollectionAndCollectionProducer()
			throws URISyntaxException, ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
		// verification
		final Class<?> fooCollectionClass = Class.forName("com.sample.FooCollection");
		final Class<?> queryFooClass = Class.forName("com.sample.QFoo");
		final Class<?> projectionFooClass = Class.forName("com.sample.PFoo");
		final Class<?> updateFooClass = Class.forName("com.sample.UFoo");
		ClassAssertion.assertThat(fooCollectionClass).isExtending(LambdamaticMongoCollectionImpl.class, Foo.class,
				queryFooClass, projectionFooClass, updateFooClass);
		final Class<?> fooCollectionProducerClass = Class.forName("com.sample.FooCollectionProducer");
		ClassAssertion.assertThat(fooCollectionProducerClass).hasMethod("getFooCollection", MongoClient.class,
				MongoClientConfiguration.class);
		final Class<?> bazCollectionClass = Class.forName("com.sample.BazCollection");
		final Class<?> queryBazClass = Class.forName("com.sample.QBaz");
		final Class<?> projectionBazClass = Class.forName("com.sample.PBaz");
		final Class<?> updateBazClass = Class.forName("com.sample.UBaz");
		ClassAssertion.assertThat(bazCollectionClass).isExtending(LambdamaticMongoCollectionImpl.class, Baz.class,
				queryBazClass, projectionBazClass, updateBazClass);
		final Class<?> bazCollectionProducerClass = Class.forName("com.sample.BazCollectionProducer");
		ClassAssertion.assertThat(bazCollectionProducerClass).hasMethod("getBazCollection", MongoClient.class,
				MongoClientConfiguration.class);

	}

	@Test(expected = ClassNotFoundException.class)
	@WithDomainClass(Bar.class)
	public void shouldProcessSingleDomainClassAndNotGenerateCollection()
			throws URISyntaxException, ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
		Class.forName("com.sample.BarCollection");
	}

	@Test(expected = ClassNotFoundException.class)
	@WithDomainClass(Bar.class)
	public void shouldProcessSingleDomainClassAndNotGenerateCollectionProducer()
			throws URISyntaxException, ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
		Class.forName("com.sample.BarCollectionProducer");
	}

	@Ignore
	@Test
	// @WithDomainClass(BikeStation.class)
	public void shouldAllowOnlyStringOrObjectIdForDocumentId() {
		fail("Not implemented yet");
	}

	@Ignore
	@Test
	// @WithDomainClass(BikeStation.class)
	public void shouldUseDocumentFieldNameInDomainClass() {
		fail("Not implemented yet - need to use custom mapping name and verify they are included in the generated metaclasses");
	}

}
