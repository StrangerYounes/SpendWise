package com.corner.takecontrol.data.repository;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeStatus;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskProgress;
import com.corner.takecontrol.util.ShareCodeGenerator;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChallengeRepository {

    private static final String COLLECTION_CHALLENGES = "challenges";
    private static final String SUBCOLLECTION_TASKS = "tasks";
    private static final String SUBCOLLECTION_PROGRESS = "progress";
    private static final String SUBCOLLECTION_FEED = "feed";

    private final FirebaseFirestore firestore;

    public ChallengeRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void createChallenge(String userId, String title, String description, int durationDays, int maxSkips,
                                boolean isPublic, String encryptedPassword, RepositoryCallback<String> callback) {
        Challenge challenge = new Challenge();
        challenge.setTitle(title);
        challenge.setDescription(description);
        challenge.setCreatedBy(userId);
        challenge.setDurationDays(durationDays);
        challenge.setMaxSkips(maxSkips);
        challenge.setPublic(isPublic);
        challenge.setEncryptedPassword(encryptedPassword);
        challenge.setStatusEnum(ChallengeStatus.DRAFT);
        List<String> members = new ArrayList<>();
        members.add(userId);
        challenge.setMemberIds(members);

        firestore.collection(COLLECTION_CHALLENGES)
                .add(challenge)
                .addOnSuccessListener(ref -> {
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public ListenerRegistration listenToMyChallenges(String userId, boolean showArchived, RepositoryCallback<List<Challenge>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES)
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null) {
                        if (callback != null) callback.onSuccess(Collections.emptyList());
                        return;
                    }
                    List<Challenge> challenges = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Challenge challenge = doc.toObject(Challenge.class);
                        boolean isArchived = challenge.getArchivedMemberIds() != null && challenge.getArchivedMemberIds().contains(userId);
                        if (showArchived != isArchived) {
                            continue;
                        }
                        challenge.setId(doc.getId());
                        challenges.add(challenge);
                    }
                    challenges.sort(Comparator.comparing(
                            (Challenge c) -> c.getCreatedAt() != null ? c.getCreatedAt().toDate().getTime() : 0L
                    ).reversed());
                    if (callback != null) callback.onSuccess(challenges);
                });
    }

    public void getChallenge(String challengeId, RepositoryCallback<Challenge> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Challenge challenge = snapshot.toObject(Challenge.class);
                    if (challenge != null) {
                        challenge.setId(snapshot.getId());
                    }
                    if (callback != null) callback.onSuccess(challenge);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public ListenerRegistration listenToChallenge(String challengeId, RepositoryCallback<Challenge> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        if (callback != null) callback.onSuccess(null);
                        return;
                    }
                    Challenge challenge = snapshot.toObject(Challenge.class);
                    if (challenge != null) {
                        challenge.setId(snapshot.getId());
                    }
                    if (callback != null) callback.onSuccess(challenge);
                });
    }

    public void updateChallenge(String challengeId, Challenge challenge, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .set(challenge)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void updateTask(String challengeId, ChallengeTask task, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS).document(task.getId())
                .set(task)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void addTask(String challengeId, ChallengeTask task, RepositoryCallback<String> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS)
                .add(task)
                .addOnSuccessListener(ref -> {
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void getTasks(String challengeId, RepositoryCallback<List<ChallengeTask>> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (callback != null) callback.onSuccess(mapTasks(snapshot, challengeId));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public ListenerRegistration listenToTasks(String challengeId, RepositoryCallback<List<ChallengeTask>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error.getMessage());
                        return;
                    }
                    if (callback != null) callback.onSuccess(snapshot != null ? mapTasks(snapshot, challengeId) : Collections.emptyList());
                });
    }

    private List<ChallengeTask> mapTasks(com.google.firebase.firestore.QuerySnapshot snapshot, String challengeId) {
        List<ChallengeTask> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            ChallengeTask task = doc.toObject(ChallengeTask.class);
            task.setId(doc.getId());
            task.setChallengeId(challengeId);
            tasks.add(task);
        }
        return tasks;
    }

    public void deleteTask(String challengeId, String taskId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS).document(taskId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void startChallenge(String challengeId, Challenge challenge, RepositoryCallback<Void> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Timestamp startDate = new Timestamp(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, challenge.getDurationDays());
        Timestamp endDate = new Timestamp(calendar.getTime());

        challenge.setStartDate(startDate);
        challenge.setEndDate(endDate);
        challenge.setStatusEnum(ChallengeStatus.ACTIVE);

        updateChallenge(challengeId, challenge, callback);
    }

    public void completeChallenge(String challengeId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("status", ChallengeStatus.COMPLETED.getValue())
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void deleteChallenge(String challengeId, RepositoryCallback<Void> callback) {
        // Note: Simple delete doesn't delete subcollections in Firestore from client SDK
        // but for a small app it might be okay or handled by cloud functions.
        // We'll just delete the main document for now.
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void leaveChallenge(String challengeId, String userId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void kickMember(String challengeId, String memberUid, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(memberUid))
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void archiveChallenge(String challengeId, String userId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("archivedMemberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void generateShareCode(String challengeId, RepositoryCallback<String> callback) {
        String code = ShareCodeGenerator.generate();
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("shareCode", code)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(code);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void joinByShareCode(String userId, String shareCode, RepositoryCallback<String> callback) {
        String normalized = ShareCodeGenerator.normalize(shareCode);
        firestore.collection(COLLECTION_CHALLENGES)
                .whereEqualTo("shareCode", normalized)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        if (callback != null) callback.onError("Challenge not found. Check the code and try again.");
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    Challenge challenge = doc.toObject(Challenge.class);
                    if (challenge == null) {
                        if (callback != null) callback.onError("Challenge not found.");
                        return;
                    }
                    if (challenge.getMemberIds() != null && challenge.getMemberIds().contains(userId)) {
                        if (callback != null) callback.onSuccess(doc.getId());
                        return;
                    }
                    
                    // Return the challenge object to check for password requirement in the UI
                    if (callback != null) callback.onSuccess("CHECK_PASSWORD:" + doc.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void joinChallenge(String userId, String challengeId, RepositoryCallback<String> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(challengeId);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void getPublicChallenges(String userId, RepositoryCallback<List<Challenge>> callback) {
        firestore.collection(COLLECTION_CHALLENGES)
                .whereEqualTo("isPublic", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Challenge> challenges = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Challenge challenge = doc.toObject(Challenge.class);
                        if (challenge.getMemberIds() != null && !challenge.getMemberIds().contains(userId)) {
                            challenge.setId(doc.getId());
                            challenges.add(challenge);
                        }
                    }
                    if (callback != null) callback.onSuccess(challenges);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public List<ChallengeTask> getTodayTasksSync(String userId) throws Exception {
        // 1. Get user's challenges
        com.google.firebase.firestore.QuerySnapshot challengeSnapshot = Tasks.await(
                firestore.collection(COLLECTION_CHALLENGES)
                        .whereArrayContains("memberIds", userId)
                        .whereEqualTo("status", ChallengeStatus.ACTIVE.getValue())
                        .get()
        );

        List<ChallengeTask> allTasks = new ArrayList<>();
        for (DocumentSnapshot challengeDoc : challengeSnapshot.getDocuments()) {
            // 2. Get tasks for each challenge
            com.google.firebase.firestore.QuerySnapshot tasksSnapshot = Tasks.await(
                    challengeDoc.getReference().collection(SUBCOLLECTION_TASKS).get()
            );

            for (DocumentSnapshot taskDoc : tasksSnapshot.getDocuments()) {
                ChallengeTask task = taskDoc.toObject(ChallengeTask.class);
                if (task != null) {
                    task.setId(taskDoc.getId());
                    task.setChallengeId(challengeDoc.getId());

                    // Filter by day of week if applicable
                    if (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) {
                        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                        if (!task.getDaysOfWeek().contains(dayOfWeek)) {
                            continue;
                        }
                    }

                    // 3. Check if already completed today
                    String periodKey = com.corner.takecontrol.util.PeriodKeyUtil.getCurrentPeriodKey(task.getFrequencyEnum());
                    String progressDocId = TaskProgress.buildDocumentId(userId, task.getId(), periodKey);
                    DocumentSnapshot progressDoc = Tasks.await(
                            challengeDoc.getReference().collection(SUBCOLLECTION_PROGRESS).document(progressDocId).get()
                    );

                    TaskProgress progress = progressDoc.toObject(TaskProgress.class);
                    if (progress == null || !progress.isCompleted()) {
                        allTasks.add(task);
                    }
                }
            }
        }
        return allTasks;
    }

    public void getActiveChallengeCount(String userId, RepositoryCallback<Integer> callback) {
        firestore.collection(COLLECTION_CHALLENGES)
                .whereArrayContains("memberIds", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 0;
                    for (DocumentSnapshot doc : snapshot) {
                        Challenge challenge = doc.toObject(Challenge.class);
                        if (challenge != null) {
                            // Count as active if it's ACTIVE or DRAFT and not archived by this user
                            boolean isArchived = challenge.getArchivedMemberIds() != null && challenge.getArchivedMemberIds().contains(userId);
                            if (!isArchived && (challenge.getStatusEnum() == ChallengeStatus.ACTIVE || challenge.getStatusEnum() == ChallengeStatus.DRAFT)) {
                                count++;
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(count);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void saveProgress(String challengeId, TaskProgress progress, RepositoryCallback<Void> callback) {
        String docId = TaskProgress.buildDocumentId(progress.getUserId(), progress.getTaskId(), progress.getPeriodKey());
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_PROGRESS).document(docId)
                .set(progress)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public ListenerRegistration listenToProgress(String challengeId, RepositoryCallback<List<TaskProgress>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_PROGRESS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error.getMessage());
                        return;
                    }
                    List<TaskProgress> progressList = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            TaskProgress progress = doc.toObject(TaskProgress.class);
                            progress.setId(doc.getId());
                            progressList.add(progress);
                        }
                    }
                    if (callback != null) callback.onSuccess(progressList);
                });
    }

    public void postToChallengeFeed(String challengeId, com.corner.takecontrol.data.model.ChallengePost post, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_FEED)
                .add(post)
                .addOnSuccessListener(ref -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void removeProgressPostFromFeed(String challengeId, String userId, String taskId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_FEED)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "PROGRESS")
                .get()
                .addOnSuccessListener(snapshot -> {
                    com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                    boolean found = false;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String docTaskId = doc.getString("taskId");
                        if ((docTaskId != null && docTaskId.equals(taskId)) || (docTaskId == null && doc.getString("content") != null && doc.getString("content").contains("completed"))) {
                            batch.delete(doc.getReference());
                            found = true;
                        }
                    }
                    if (found) {
                        batch.commit()
                                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public ListenerRegistration listenToChallengeFeed(String challengeId, RepositoryCallback<List<com.corner.takecontrol.data.model.ChallengePost>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_FEED)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error.getMessage());
                        return;
                    }
                    List<com.corner.takecontrol.data.model.ChallengePost> posts = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            com.corner.takecontrol.data.model.ChallengePost post = doc.toObject(com.corner.takecontrol.data.model.ChallengePost.class);
                            post.setId(doc.getId());
                            posts.add(post);
                        }
                    }
                    if (callback != null) callback.onSuccess(posts);
                });
    }
}
