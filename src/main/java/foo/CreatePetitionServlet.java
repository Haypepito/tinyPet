package foo;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

import java.io.IOException;

@WebServlet(name = "CreatePetitionServlet", urlPatterns = { "/petitions/create" })
public class CreatePetitionServlet extends HttpServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Récupérer les données du formulaire de création de pétition
        String petitionTitle = request.getParameter("title");
        String petitionBody = request.getParameter("body");

        // Valider les données (vous pouvez ajouter des vérifications supplémentaires ici)

        // Créer la pétition dans Datastore
        createPetition(petitionTitle, petitionBody);

        // Rediriger vers une autre page après la création de la pétition
        response.sendRedirect("/petitions");
    }

    private void createPetition(String title, String body) {
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity petitionEntity = new Entity("Petition");
        petitionEntity.setProperty("title", title);
        petitionEntity.setProperty("body", body);
        datastore.put(petitionEntity);
    
    }
}
