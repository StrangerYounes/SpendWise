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

    private final FirebaseFirestore firestore;

    public ChallengeRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void createChallenge(String userId, String title, String description, int durationDays, int maxSkips,
                                RepositoryCallback<String> callback) {
        Challenge challenge = new Challenge();
        challenge.setTitle(title);
        challenge.setDescription(description);
        challenge.setCreatedBy(userId);
        challenge.setDurationDays(durationDays);
        challenge.setMaxSkips(maxSkips);
        challenge.setStatusEnum(ChallengeStatus.DRAFT);
        List<String> members = new ArrayList<>();
        members.add(userId);
        challenge.setMemberIds(members);

        firestore.collection(COLLECTION_CHALLENGES)
                .add(challenge)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToMyChallenges(String userId, RepositoryCallback<List<Challenge>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES)
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null) {
                        callback.onSuccess(Collections.emptyList());
                        return;
                    }
                    List<Challenge> challenges = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Challenge challenge = doc.toObject(Challenge.class);
                        challenge.setId(doc.getId());
                        challenges.add(challenge);
                    }
                    challenges.sort(Comparator.comparing(
                            (Challenge c) -> c.getCreatedAt() != null ? c.getCreatedAt().toDate().getTime() : 0L
                    ).reversed());
                    callback.onSuccess(challenges);
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
                    callback.onSuccess(challenge);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToChallenge(String challengeId, RepositoryCallback<Challenge> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    Challenge challenge = snapshot.toObject(Challenge.class);
                    if (challenge != null) {
                        challenge.setId(snapshot.getId());
                    }
                    callback.onSuccess(challenge);
                });
    }

    public void updateChallenge(String challengeId, Challenge challenge, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .set(challenge)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateTask(String challengeId, ChallengeTask task, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS).document(task.getId())
                .set(task)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addTask(String challengeId, ChallengeTask task, RepositoryCallback<String> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS)
                .add(task)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getTasks(String challengeId, RepositoryCallback<List<ChallengeTask>> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapTasks(snapshot)))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToTasks(String challengeId, RepositoryCallback<List<ChallengeTask>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    callback.onSuccess(snapshot != null ? mapTasks(snapshot) : Collections.emptyList());
                });
    }

    private List<ChallengeTask> mapTasks(com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<ChallengeTask> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            ChallengeTask task = doc.toObject(ChallengeTask.class);
            task.setId(doc.getId());
            tasks.add(task);
        }
        return tasks;
    }

    public void deleteTask(String challengeId, String taskId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_TASKS).document(taskId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void generateShareCode(String challengeId, RepositoryCallback<String> callback) {
        String code = ShareCodeGenerator.generate();
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .update("shareCode", code)
                .addOnSuccessListener(unused -> callback.onSuccess(code))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void joinByShareCode(String userId, String shareCode, RepositoryCallback<String> callback) {
        String normalized = ShareCodeGenerator.normalize(shareCode);
        firestore.collection(COLLECTION_CHALLENGES)
                .whereEqualTo("shareCode", normalized)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onError("Challenge not found. Check the code and try again.");
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    Challenge challenge = doc.toObject(Challenge.class);
                    if (challenge == null) {
                        callback.onError("Challenge not found.");
                        return;
                    }
                    if (challenge.getMemberIds() != null && challenge.getMemberIds().contains(userId)) {
                        callback.onSuccess(doc.getId());
                        return;
                    }
                    firestore.collection(COLLECTION_CHALLENGES).document(doc.getId())
                            .update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                            .addOnSuccessListener(unused -> callback.onSuccess(doc.getId()))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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

    public void saveProgress(String challengeId, TaskProgress progress, RepositoryCallback<Void> callback) {
        String docId = TaskProgress.buildDocumentId(progress.getUserId(), progress.getTaskId(), progress.getPeriodKey());
        firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_PROGRESS).document(docId)
                .set(progress)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToProgress(String challengeId, RepositoryCallback<List<TaskProgress>> callback) {
        return firestore.collection(COLLECTION_CHALLENGES).document(challengeId)
                .collection(SUBCOLLECTION_PROGRESS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
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
                    callback.onSuccess(progressList);
                });
    }
}
