package eu.bb.app.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "gift_categories")
public class GiftCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    @JsonIgnore // Не возвращаем поле hidden в API
    private Boolean hidden = false;
    
    @ManyToMany(mappedBy = "categories")
    @JsonIgnore // Игнорируем обратную связь при сериализации, чтобы избежать циклических ссылок
    private Set<Gift> gifts = new HashSet<>();
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Boolean getHidden() {
        return hidden;
    }
    
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
    
    public Set<Gift> getGifts() {
        return gifts;
    }
    
    public void setGifts(Set<Gift> gifts) {
        this.gifts = gifts;
    }
}
