/**
 * 
 */
package org.lambdamatic.mongodb.testutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate the MongoDB Collection that should be processed by the {@link CleanMongoCollectionsRule}
 * 
 * @author Xavier Coulon
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithDBCollections.class)
public @interface WithDBCollection{

	public Class<?> value();
}

