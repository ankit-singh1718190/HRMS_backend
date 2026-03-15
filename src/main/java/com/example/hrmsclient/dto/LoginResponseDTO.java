package com.example.hrmsclient.dto;

public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String emailId;
    private String fullName;
    private String role;
    private String employeeId;
    /** Numeric employee DB id (for employees only); used by frontend for leave apply / own leaves. */
    private Long id;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String accessToken, String refreshToken,
                            String emailId, String fullName,
                            String role, String employeeId) {
        this(accessToken, refreshToken, emailId, fullName, role, employeeId, null);
    }

    public LoginResponseDTO(String accessToken, String refreshToken,
                            String emailId, String fullName,
                            String role, String employeeId, Long id) {
        this.accessToken  = accessToken;
        this.refreshToken = refreshToken;
        this.emailId      = emailId;
        this.fullName     = fullName;
        this.role         = role;
        this.employeeId   = employeeId;
        this.id           = id;
    }

    // Getters & Setters
    public String getAccessToken()              { return accessToken;  }
    public void setAccessToken(String t)        { this.accessToken = t; }

    public String getRefreshToken()             { return refreshToken; }
    public void setRefreshToken(String t)       { this.refreshToken = t; }

    public String getTokenType()                { return tokenType;    }
    public void setTokenType(String t)          { this.tokenType = t;  }

    public String getEmailId()                  { return emailId;      }
    public void setEmailId(String e)            { this.emailId = e;    }

    public String getFullName()                 { return fullName;     }
    public void setFullName(String n)           { this.fullName = n;   }

    public String getRole()                     { return role;         }
    public void setRole(String r)               { this.role = r;       }

    public String getEmployeeId()               { return employeeId;   }
    public void setEmployeeId(String id)        { this.employeeId = id; }

    public Long getId()                         { return id;           }
    public void setId(Long id)                  { this.id = id;        }
}