package eu.bb.app.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "gifts")
public class Gift {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long eventId;
    private String title;
    @Column(length = 500)
    private String description;
    @Column(length = 500)
    private String url;
    @Column(length = 500)
    private String image;
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    private String status; // open | reserved
    private Long reservedByGuest; // FK to event_guests
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReservedByGuest() { return reservedByGuest; }
    public void setReservedByGuest(Long reservedByGuest) { this.reservedByGuest = reservedByGuest; }
}
