/*******************************************************************************
 * Copyright (c) 2015 Red Hat. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat - Initial Contribution
 *******************************************************************************/

package org.lambdamatic.mongodb;

import static com.lordofthejars.nosqlunit.mongodb.MongoDbConfigurationBuilder.mongoDb;

import org.junit.Rule;

import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import com.mongodb.MongoClient;

public abstract class MongoBaseTest {

  public static final String DATABASE_NAME = "unittests";

  private MongoClient mongoClient = new MongoClient();

  @Rule
  public MongoDbRule remoteMongoDbRule =
      new MongoDbRule(mongoDb().databaseName(DATABASE_NAME).build());

  private final String collectionName;

  public MongoBaseTest(final Class<?> documentClass) {
    this.collectionName = ((org.lambdamatic.mongodb.annotations.Document) documentClass
        .getAnnotation(org.lambdamatic.mongodb.annotations.Document.class)).collection();
  }

  public MongoClient getMongoClient() {
    return mongoClient;
  }

  public String getCollectionName() {
    return collectionName;
  }

}
