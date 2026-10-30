package rbpo.task2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {
    private String number; //номер автомобиля

    @ManyToOne //у пользователя может быть много машин
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"email", "role", "enabled", "locked"})
    private User owner; //владелец

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
