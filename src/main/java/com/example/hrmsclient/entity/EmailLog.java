package com.example.hrmsclient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "email_logs",
    indexes = {
        @Index(name = "idx_email_log_recipient", columnList = "recipientEmail"),
        @Index(name = "idx_email_log_status",    columnList = "status"),
        @Index(name = "idx_email_log_sent_at",   columnList = "sentAt")
    }
)
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String recipientEmail;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(length = 50)
    private String emailType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public EmailLog() {}
    public EmailLog(Long id, String recipientEmail, String subject,
                    String emailType, String status,
                    String errorMessage, LocalDateTime sentAt) {
        this.id             = id;
        this.recipientEmail = recipientEmail;
        this.subject        = subject;
        this.emailType      = emailType;
        this.status         = status;
        this.errorMessage   = errorMessage;
        this.sentAt         = sentAt;
    }

    public Long getId()                 { return id;             }
    public String getRecipientEmail()   { return recipientEmail; }
    public String getSubject()          { return subject;        }
    public String getEmailType()        { return emailType;      }
    public String getStatus()           { return status;         }
    public String getErrorMessage()     { return errorMessage;   }
    public LocalDateTime getSentAt()    { return sentAt;         }

    // ✅ Setters
    public void setId(Long id)                          { this.id             = id;             }
    public void setRecipientEmail(String recipientEmail){ this.recipientEmail = recipientEmail; }
    public void setSubject(String subject)              { this.subject        = subject;        }
    public void setEmailType(String emailType)          { this.emailType      = emailType;      }
    public void setStatus(String status)                { this.status         = status;         }
    public void setErrorMessage(String errorMessage)    { this.errorMessage   = errorMessage;   }
    public void setSentAt(LocalDateTime sentAt)         { this.sentAt         = sentAt;         }

    // ✅ Manual Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String recipientEmail;
        private String subject;
        private String emailType;
        private String status;
        private String errorMessage;
        private LocalDateTime sentAt;

        public Builder id(Long id)                          { this.id             = id;             return this; }
        public Builder recipientEmail(String recipientEmail){ this.recipientEmail = recipientEmail; return this; }
        public Builder subject(String subject)              { this.subject        = subject;        return this; }
        public Builder emailType(String emailType)          { this.emailType      = emailType;      return this; }
        public Builder status(String status)                { this.status         = status;         return this; }
        public Builder errorMessage(String errorMessage)    { this.errorMessage   = errorMessage;   return this; }
        public Builder sentAt(LocalDateTime sentAt)         { this.sentAt         = sentAt;         return this; }

        public EmailLog build() {
            return new EmailLog(id, recipientEmail, subject,
                                emailType, status, errorMessage, sentAt);
        }
    }
}