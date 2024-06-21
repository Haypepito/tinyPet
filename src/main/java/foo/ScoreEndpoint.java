package foo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import foo.User_sign;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.auth.EspAuthenticator;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

@Api(name = "myApi",
     version = "v1",
     audiences = "927375242383-t21v9ml38tkh2pr30m4hqiflkl3jfohl.apps.googleusercontent.com",
  	 clientIds = {"927375242383-t21v9ml38tkh2pr30m4hqiflkl3jfohl.apps.googleusercontent.com",
        "927375242383-jm45ei76rdsfv7tmjv58tcsjjpvgkdje.apps.googleusercontent.com"},
     namespace =
     @ApiNamespace(
		   ownerDomain = "helloworld.example.com",
		   ownerName = "helloworld.example.com",
		   packagePath = "")
     )

public class ScoreEndpoint {


	Random r = new Random();

    // remember: return Primitives and enums are not allowed. 
	@ApiMethod(name = "getRandom", httpMethod = HttpMethod.GET)
	public RandomResult random() {
		return new RandomResult(r.nextInt(6) + 1);
	}

	@ApiMethod(name = "hello", httpMethod = HttpMethod.GET)
	public User Hello(User user) throws UnauthorizedException {
        if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
        System.out.println("Yeah:"+user.toString());
		return user;
	}


	@ApiMethod(name = "scores", httpMethod = HttpMethod.GET)
	public List<Entity> scores() {
		Query q = new Query("Score").addSort("score", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
		return result;
	}

	@ApiMethod(name = "topscores", httpMethod = HttpMethod.GET)
	public List<Entity> topscores() {
		Query q = new Query("Score").addSort("score", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));
		return result;
	}

	@ApiMethod(name = "myscores", httpMethod = HttpMethod.GET)
	public List<Entity> myscores(@Named("name") String name) {
		Query q = new Query("Score").setFilter(new FilterPredicate("name", FilterOperator.EQUAL, name)).addSort("score",
				SortDirection.DESCENDING);
        //Query q = new Query("Score").setFilter(new FilterPredicate("name", FilterOperator.EQUAL, name));

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));
		return result;
	}

	@ApiMethod(name = "addScore", httpMethod = HttpMethod.GET)
	public Entity addScore(@Named("score") int score, @Named("name") String name) {

		Entity e = new Entity("Score", "" + name + score);
		e.setProperty("name", name);
		e.setProperty("score", score);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(e);

		return e;
	}

	@ApiMethod(name = "postMessage", httpMethod = HttpMethod.POST)
	public Entity postMessage(PostMessage pm) {

		Entity e = new Entity("Post"); // quelle est la clef ?? non specifié -> clef automatique
		e.setProperty("owner", pm.owner);
		e.setProperty("url", pm.url);
		e.setProperty("body", pm.body);
		e.setProperty("likec", 0);
		e.setProperty("date", new Date());

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
		txn.commit();
		return e;
	}

	@ApiMethod(name = "mypost", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> mypost(@Named("name") String name, @Nullable @Named("next") String cursorString) {

	    Query q = new Query("Post").setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, name));

	    // https://cloud.google.com/appengine/docs/standard/python/datastore/projectionqueries#Indexes_for_projections
	    //q.addProjection(new PropertyProjection("body", String.class));
	    //q.addProjection(new PropertyProjection("date", java.util.Date.class));
	    //q.addProjection(new PropertyProjection("likec", Integer.class));
	    //q.addProjection(new PropertyProjection("url", String.class));

	    // looks like a good idea but...
	    // generate a DataStoreNeedIndexException -> 
	    // require compositeIndex on owner + date
	    // Explosion combinatoire.
	    // q.addSort("date", SortDirection.DESCENDING);
	    
	    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	    PreparedQuery pq = datastore.prepare(q);
	    
	    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(2);
	    
	    if (cursorString != null) {
		fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}
	    
	    QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
	    cursorString = results.getCursor().toWebSafeString();
	    
	    return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	    
	}
    
	@ApiMethod(name = "getPost",
		   httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Entity> getPost(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Query q = new Query("Post").
		    setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, user.getEmail()));

		// Multiple projection require a composite index
		// owner is automatically projected...
		// q.addProjection(new PropertyProjection("body", String.class));
		// q.addProjection(new PropertyProjection("date", java.util.Date.class));
		// q.addProjection(new PropertyProjection("likec", Integer.class));
		// q.addProjection(new PropertyProjection("url", String.class));

		// looks like a good idea but...
		// require a composite index
		// - kind: Post
		//  properties:
		//  - name: owner
		//  - name: date
		//    direction: desc

		// q.addSort("date", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(2);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
		cursorString = results.getCursor().toWebSafeString();

		return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "postMsg", httpMethod = HttpMethod.POST)
	public Entity postMsg(User user, PostMessage pm) throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Entity e = new Entity("Post", Long.MAX_VALUE-(new Date()).getTime()+":"+user.getEmail());
		e.setProperty("owner", user.getEmail());
		e.setProperty("url", pm.url);
		e.setProperty("body", pm.body);
		e.setProperty("likec", 0);
		e.setProperty("date", new Date());

///		Solution pour pas projeter les listes
//		Entity pi = new Entity("PostIndex", e.getKey());
//		HashSet<String> rec=new HashSet<String>();
//		pi.setProperty("receivers",rec);
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
//		datastore.put(pi);
		txn.commit();
		return e;
	}

	@ApiMethod(name = "createPetition", httpMethod = HttpMethod.POST)
    public Entity createPetition(Petition petition) throws UnauthorizedException {


        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        HashSet<String> tags = new HashSet<String>();
        String[] chaine_tags = petition.tags.split(",");

        for (String tag : chaine_tags) {
            tags.add(tag);
        } 
        Entity petitionEntity = new Entity("Petition");
        petitionEntity.setProperty("title", petition.title);
        petitionEntity.setProperty("description", petition.description);
        petitionEntity.setProperty("tags",  tags);
        petitionEntity.setProperty("creator", petition.creator);
		petitionEntity.setProperty("nb_sign", 0);
        petitionEntity.setProperty("date", new Date());

        try {
            datastore.put(txn, petitionEntity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }

        return petitionEntity;

    }

	@ApiMethod(name = "allpetition", httpMethod = HttpMethod.GET)
	public List<Entity> allpetition() {
		Query q = new Query("Petition").addSort("date",SortDirection.DESCENDING);
        //Query q = new Query("Score").setFilter(new FilterPredicate("name", FilterOperator.EQUAL, name));

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
		return result;
	}

	@ApiMethod(name = "petition", path = "petition/{id}", httpMethod = HttpMethod.GET)
	public Entity petition(@Named("id") Long id) throws NotFoundException {

		Key petitionKey = KeyFactory.createKey("Petition", id);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query.Filter keyFilter = new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, petitionKey);
        Query query = new Query("Petition").setFilter(keyFilter);
        PreparedQuery preparedQuery = datastore.prepare(query);
    
        Entity petitionEntity = preparedQuery.asSingleEntity();
        if (petitionEntity != null) {
            return petitionEntity;
        } else {
            throw new NotFoundException("Petition not found with ID: wrf  " + id);
        }
	}

	

	@ApiMethod(name = "auth", path = "auth/", httpMethod = HttpMethod.POST)
public void auth(User_sign user) throws NotFoundException, UnauthorizedException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Transaction txn = datastore.beginTransaction();

    try {
        // Log initial
        System.out.println("Starting authentication for user: " + user.getToken());

        List<Object> result = Auth.verifyToken(user.getToken(),user.getId());
        boolean isVerified = (Boolean) result.get(0);
        String mail = (String) result.get(1);

        // Log results of verification
        System.out.println("Token verification result: " + isVerified);
        System.out.println("Email from token: " + mail);

        if (isVerified && mail != null) {
            Key userKey = KeyFactory.createKey("User", user.getId());
            Entity userEntity;

            try {
                userEntity = datastore.get(userKey);
                // Log if user exists
                System.out.println("User found in datastore: " + user.getId());
            } catch (EntityNotFoundException e) {
                // Log if user not found
                System.out.println("User not found, creating new user: " + user.getId());
                userEntity = new Entity("User", user.getId());
                userEntity.setProperty("email", mail);
                userEntity.setProperty("name", user.getName());
				userEntity.setProperty("id", user.getId());
                userEntity.setProperty("signedPetitions", new ArrayList<Long>());
                datastore.put(userEntity);
                txn.commit();
                return;
            }

            // Update user entity if needed
            if (!userEntity.getProperty("email").equals(mail)) {
                userEntity.setProperty("email", mail);
            }
            if (!userEntity.getProperty("name").equals(user.getName())) {
                userEntity.setProperty("name", user.getName());
            }
            datastore.put(userEntity);
            txn.commit();
        } else {
            throw new UnauthorizedException("Token verification failed or email is null.");
        }
    } catch (Exception e) {
        if (txn.isActive()) {
            txn.rollback();
        }
        e.printStackTrace();
        throw new UnauthorizedException("Erreur : " + e.getMessage());
    } finally {
        if (txn.isActive()) {
            txn.rollback();
        }
    }
}

@ApiMethod(name = "signerPetition", path = "petition/{id}/signer/{user_id}", httpMethod = ApiMethod.HttpMethod.GET)
public Entity signerPetition(@Named("id") Long id, @Named("user_id") String user_id) throws NotFoundException, UnauthorizedException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Transaction txn = datastore.beginTransaction();

    try {
        // Récupérer l'entité de la pétition
        Key petitionKey = KeyFactory.createKey("Petition", id);
        Entity petitionEntity = datastore.get(petitionKey);

        // Récupérer l'entité de l'utilisateur
        Key userKey = KeyFactory.createKey("User", user_id);
        Entity userEntity;

        try {
            userEntity = datastore.get(userKey);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException("User not found with email: " + user_id);
        }

        User_sign user = User_sign.fromEntity(userEntity);

        if (user.hasSignedPetition(id)) {
            throw new UnauthorizedException("User has already signed the petition.");
        }

        // Mettre à jour le nombre de signatures
        Long nbSign = (Long) petitionEntity.getProperty("nb_sign");
        petitionEntity.setProperty("nb_sign", nbSign + 1);

        user.addSignedPetition(id);
        userEntity = user.toEntity();

        // Sauvegarder les entités mises à jour dans le datastore
        datastore.put(petitionEntity);
        datastore.put(userEntity);

        txn.commit();
        return petitionEntity;

    } catch (EntityNotFoundException e) {
        if (txn.isActive()) {
            txn.rollback();
        }
        throw new NotFoundException("Entity not found: " + e.getMessage());
    } catch (Exception e) {
        if (txn.isActive()) {
            txn.rollback();
        }
        e.printStackTrace();
        throw new UnauthorizedException("Erreur : " + e.getMessage());
    }
}

	@ApiMethod(name = "searchPetitionsByTag", path = "petition/searchByTag/{tag}", httpMethod = ApiMethod.HttpMethod.GET)
	public List<Entity> searchPetitionsByTag(@Named("tag") String tag) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		
		Query.Filter tagFilter = new Query.FilterPredicate("tags", Query.FilterOperator.EQUAL, tag);
		Query query = new Query("Petition").setFilter(tagFilter);
		PreparedQuery preparedQuery = datastore.prepare(query);

		// Récupérer les résultats de la requête
		List<Entity> results = new ArrayList<>();
		for (Entity entity : preparedQuery.asIterable()) {
			results.add(entity);
		}

		return results;
}

	@ApiMethod(name = "getUsersSigningPetition", path = "petition/{id}/users", httpMethod = ApiMethod.HttpMethod.GET)
	public List<Entity> getUsersSigningPetition(@Named("id") Long petitionId) throws NotFoundException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		// Rechercher tous les utilisateurs dont la liste signedPetitions contient l'ID de la pétition spécifiée
		Query.Filter filter = new Query.FilterPredicate("signedPetitions", Query.FilterOperator.EQUAL, petitionId);
		Query query = new Query("User").setFilter(filter);
		PreparedQuery preparedQuery = datastore.prepare(query);

		List<Entity> userEntities = new ArrayList<>();
		for (Entity entity : preparedQuery.asIterable()) {
			userEntities.add(entity);
		}

		return userEntities;
}

	@ApiMethod(name = "getPetitionsByUser", path = "user/{email}/petitions", httpMethod = HttpMethod.GET)
	public List<Entity> getPetitionsByUser(@Named("email") String email) throws NotFoundException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Petition").setFilter(new FilterPredicate("creator", FilterOperator.EQUAL, email));
      
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
		return result;

		
}
}
