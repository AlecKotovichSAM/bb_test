package eu.bb.app.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "child_avatars")
public class ChildAvatar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long childId;
    
    @Lob
    @Column(columnDefinition = "CLOB", nullable = false)
    private String avatar; // Base64 строка: data:image/jpeg;base64,... или data:image/png;base64,...
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getChildId() {
        return childId;
    }
    
    public void setChildId(Long childId) {
        this.childId = childId;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
