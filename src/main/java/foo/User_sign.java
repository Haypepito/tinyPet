package foo;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.Entity;

public class User_sign {
    private String id;
    private String email;
    private String name;
    private String token;
    private List<Long> signedPetitions;

    public User_sign(String id, String mail, String name) {
        this.id = id;
        this.email = mail;
        this.name = name;
        this.signedPetitions = new ArrayList<>();
    }

    public User_sign() {
    }

    public Entity toEntity() {
        Entity entity = new Entity("User", id);
        entity.setProperty("email", email);
        entity.setProperty("name", name);
        entity.setProperty("signedPetitions", signedPetitions);
        return entity;
    }

    public static User_sign fromEntity(Entity entity) {
        String id = entity.getKey().getName();
        String email = (String) entity.getProperty("email");
        String name = (String) entity.getProperty("name");
        List<Long> signedPetitions = (List<Long>) entity.getProperty("signedPetitions");
        User_sign user = new User_sign(id, email, name);
        user.signedPetitions = signedPetitions != null ? signedPetitions : new ArrayList<>();
        return user;
    }

    public void addSignedPetition(Long petitionId) {
        if (!signedPetitions.contains(petitionId)) {
            signedPetitions.add(petitionId);
        }
    }

    public boolean hasSignedPetition(Long petitionId) {
        return signedPetitions.contains(petitionId);
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<Long> getSignedPetitions() {
        return signedPetitions;
    }

    public void setSignedPetitions(List<Long> signedPetitions) {
        this.signedPetitions = signedPetitions;
    }
}
