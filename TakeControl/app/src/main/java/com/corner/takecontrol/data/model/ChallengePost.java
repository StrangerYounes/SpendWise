package com.corner.takecontrol.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class ChallengePost {
    @DocumentId
    private String id;
    private String challengeId;
    private String userId;
    private String userName;
    private String userPhotoUrl;
    private String userEncryptedPhoto;
    private String content;
    private String type; // STATUS, ACHIEVEMENT, PROGRESS
    private String visibility; // CHALLENGE, PUBLIC
    
    @ServerTimestamp
    private Timestamp timestamp;

    public ChallengePost() {}

    public ChallengePost(String challengeId, String userId, String userName, String content, String type) {
        this.challengeId = challengeId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.type = type;
        this.visibility = "CHALLENGE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoUrl() { return userPhotoUrl; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public String getUserEncryptedPhoto() { return userEncryptedPhoto; }
    public void setUserEncryptedPhoto(String userEncryptedPhoto) { this.userEncryptedPhoto = userEncryptedPhoto; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
