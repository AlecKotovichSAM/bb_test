package eu.bb.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEventRequest {
    private Long hostId;
    private Long childId;
    private LocalDateTime datetime;
    private String locationType;
    private String location;
    private String status;
    private String comment;
    private List<GuestWithChildren> guests;
    
    public static class GuestWithChildren {
        private Long guestId; // опционально: ID гостя из предыдущего события для переиспользования
        private String guestName; // опционально: имя нового гостя или переопределение для существующего
        private Long userId; // опционально: ID зарегистрированного пользователя
        private List<String> children; // список имен детей
        
        public Long getGuestId() {
            return guestId;
        }
        
        public void setGuestId(Long guestId) {
            this.guestId = guestId;
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
        
        public List<String> getChildren() {
            return children;
        }
        
        public void setChildren(List<String> children) {
            this.children = children;
        }
    }
    
    // Getters and setters
    public Long getHostId() {
        return hostId;
    }
    
    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
    
    public Long getChildId() {
        return childId;
    }
    
    public void setChildId(Long childId) {
        this.childId = childId;
    }
    
    public LocalDateTime getDatetime() {
        return datetime;
    }
    
    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }
    
    public String getLocationType() {
        return locationType;
    }
    
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public List<GuestWithChildren> getGuests() {
        return guests;
    }
    
    public void setGuests(List<GuestWithChildren> guests) {
        this.guests = guests;
    }
}
