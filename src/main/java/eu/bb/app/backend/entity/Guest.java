package eu.bb.app.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guests")
public class Guest {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String guestName;
    
    private Long userId; // nullable - для незарегистрированных гостей
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getGuestName() { 
        return guestName; 
    }
    
    public void setGuestName(String guestName) { 
        this.guestName = guestName; 
    }
    
    public Long getUserId() { 
        return userId; 
    }
    
    public void setUserId(Long userId) { 
        this.userId = userId; 
    }
}
