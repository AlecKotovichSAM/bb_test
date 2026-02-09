package eu.bb.app.backend.dto;

import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.entity.EventGuest;
import eu.bb.app.backend.entity.GuestChild;
import java.util.List;

public class EventResponse {
    private Event event;
    private List<GuestWithChildrenResponse> guests;
    
    public static class GuestWithChildrenResponse {
        private EventGuest guest;
        private List<GuestChild> children;
        
        public GuestWithChildrenResponse() {
        }
        
        public GuestWithChildrenResponse(EventGuest guest, List<GuestChild> children) {
            this.guest = guest;
            this.children = children;
        }
        
        public EventGuest getGuest() {
            return guest;
        }
        
        public void setGuest(EventGuest guest) {
            this.guest = guest;
        }
        
        public List<GuestChild> getChildren() {
            return children;
        }
        
        public void setChildren(List<GuestChild> children) {
            this.children = children;
        }
    }
    
    public EventResponse() {
    }
    
    public EventResponse(Event event, List<GuestWithChildrenResponse> guests) {
        this.event = event;
        this.guests = guests;
    }
    
    public Event getEvent() {
        return event;
    }
    
    public void setEvent(Event event) {
        this.event = event;
    }
    
    public List<GuestWithChildrenResponse> getGuests() {
        return guests;
    }
    
    public void setGuests(List<GuestWithChildrenResponse> guests) {
        this.guests = guests;
    }
}
