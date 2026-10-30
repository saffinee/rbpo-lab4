package rbpo.task2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users") //user - зарезервированное слово в PostgreSQL
public class User extends BaseEntity {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;
    private String role = "USER";
    private boolean enabled = true;
    private boolean locked = false;

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean value) { locked = value; }
    private String name;
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
