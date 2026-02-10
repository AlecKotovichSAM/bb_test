package eu.bb.app.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "event_guests")
public class EventGuest {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long eventId;
    
    private String rsvpStatus; // open | accepted | declined
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest; // связь с Guest - основное поле для маппинга колонки guest_id
    
    // Временные поля для десериализации запросов (не сохраняются в БД)
    @Transient
    private String guestName; // используется только при создании нового гостя через API
    
    @Transient
    private Long userId; // используется только при создании нового гостя через API
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public Long getEventId() { 
        return eventId; 
    }
    
    public void setEventId(Long eventId) { 
        this.eventId = eventId; 
    }
    
    // Геттер для получения ID гостя из связи
    public Long getGuestId() { 
        return guest != null ? guest.getId() : null; 
    }
    
    // Сеттер для установки ID гостя - создает временный объект Guest с ID
    // ВАЖНО: этот метод работает только если Guest уже загружен в контексте Hibernate
    // Для новых объектов используйте setGuest() с загруженным объектом Guest из репозитория
    public void setGuestId(Long guestId) { 
        if (guestId != null) {
            // Используем EntityManager.getReference() через Hibernate Session
            // Это создаст прокси объект Guest с указанным ID
            // Hibernate будет использовать этот ID для сохранения в колонку guest_id
            Guest g = new Guest();
            g.setId(guestId);
            this.guest = g;
        } else {
            this.guest = null;
        }
    }
    
    public Guest getGuest() {
        return guest;
    }
    
    public void setGuest(Guest guest) {
        this.guest = guest;
    }
    
    public String getRsvpStatus() { 
        return rsvpStatus; 
    }
    
    public void setRsvpStatus(String rsvpStatus) { 
        this.rsvpStatus = rsvpStatus; 
    }
    
    // Вспомогательные методы для обратной совместимости
    public String getGuestName() {
        // Сначала проверяем временное поле (для десериализации запросов)
        if (guestName != null) {
            return guestName;
        }
        // Затем проверяем связанного гостя
        return guest != null ? guest.getGuestName() : null;
    }
    
    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }
    
    public Long getUserId() {
        // Сначала проверяем временное поле (для десериализации запросов)
        if (userId != null) {
            return userId;
        }
        // Затем проверяем связанного гостя
        return guest != null ? guest.getUserId() : null;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
