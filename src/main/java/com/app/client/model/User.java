package com.app.client.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class User {
    private final IntegerProperty id            = new SimpleIntegerProperty();
    private final StringProperty  login         = new SimpleStringProperty();
    private final StringProperty  password      = new SimpleStringProperty();
    private final StringProperty  email         = new SimpleStringProperty();
    private final StringProperty  fullName      = new SimpleStringProperty();
    private final StringProperty  role          = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    

    public User() {}

    public User(int id, String login, String passwordHash, String email,
                String fullName, String role,
                LocalDateTime createdAt) {
        this.id.set(id);
        this.login.set(login);
        this.password.set(passwordHash);
        this.email.set(email);
        this.fullName.set(fullName);
        this.role.set(role);
        this.createdAt.set(createdAt);
        
    }

    // getters/setters & properties...

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getLogin() { return login.get(); }
    public void setLogin(String v) { login.set(v); }
    public StringProperty loginProperty() { return login; }

    public String getPassword() { return password.get(); }
    public void setPassword(String v) { password.set(v); }

    public String getEmail() { return email.get(); }
    public void setEmail(String v) { email.set(v); }
    public StringProperty emailProperty() { return email; }

    public String getFullName() { return fullName.get(); }
    public void setFullName(String v) { fullName.set(v); }
    public StringProperty fullNameProperty() { return fullName; }

    public String getRole() { return role.get(); }
    public void setRole(String v) { role.set(v); }
    public StringProperty roleProperty() { return role; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }


}
