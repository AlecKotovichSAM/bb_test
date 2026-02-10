package eu.bb.app.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_avatars")
public class UserAvatar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long userId;
    
    @Lob
    @Column(columnDefinition = "CLOB", nullable = false)
    private String avatar; // Base64 строка: data:image/jpeg;base64,... или data:image/png;base64,...
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
