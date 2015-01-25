package $packageName$;

import javax.annotation.Generated;

import org.lambdamatic.mongodb.LambdamaticMongoCollection;
import org.lambdamatic.mongodb.crud.impl.LambdamaticMongoCollectionImpl;

import com.mongodb.MongoClient;

/**
 * The {@link LambdamaticMongoCollection} implementation associated with the {@link $domainClassName$} 
 * domain class and with support for functional @{FilterExpression}. 
 *
 */
@Generated(value="$processorClassName$")
public class $mongoCollectionClassName$ extends LambdamaticMongoCollectionImpl<$domainClassName$, $metadataDomainClassName$> {

	public $mongoCollectionClassName$(final MongoClient mongoClient, final String databaseName, final String collectionName) {
		super(mongoClient, databaseName, collectionName, $domainClassName$.class);
	}

}