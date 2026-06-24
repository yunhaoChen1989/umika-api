package ca.umika.api.email;

import jakarta.persistence.*;
import ca.umika.api.common.persistence.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "subject")
    private String subject;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "is_active")
    private Boolean isActive;
public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
